import {execFileSync} from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import {DatabaseSync} from 'node:sqlite';
const root=process.cwd();
const adb=process.env.ADB_PATH||'D:/Documents/ChatGPT/111/DSH-Android/apps/android/toolchains/local/android-sdk/platform-tools/adb.exe';
const run=(...a)=>execFileSync(adb,a,{encoding:'utf8',timeout:55000,windowsHide:true}).trim();
const pause=ms=>new Promise(r=>setTimeout(r,ms));
const pid=name=>{try{return run('shell','pidof',name);}catch{return '';}};
const results=[];
const check=(label,value,detail)=>{results.push({label,pass:!!value,detail});if(!value)throw Error(label+': '+JSON.stringify(detail));};
function snapshot(){
 const dest=path.join(root,'delivery','qa-ledger.db');
 for(const suffix of ['','-wal']){
   const data=execFileSync(adb,['exec-out','run-as','app.winku.ledger','cat','databases/ledger.db'+suffix],{timeout:10000,windowsHide:true});fs.writeFileSync(dest+suffix,data);
 }
 const db=new DatabaseSync(dest);const tx=db.prepare('SELECT json FROM txns').all().map(v=>JSON.parse(v.json));const inbox=db.prepare('SELECT * FROM inbox').all();db.close();return {tx,inbox};
}
function post(id,text,when,title='微信支付'){run('shell','am','start','-n','com.tencent.mm/fixture.winku.notifications.Poster','--ei','id',String(id),'--es','title',title,'--es','text',text,'--el','when',String(when));}
function ui(script,capture){const args=['shell','am','instrument','-w','-e','ui64',Buffer.from(script).toString('base64')];if(capture)args.push('-e','capture',capture);args.push('app.winku.ledger.test/app.winku.ledger.LedgerInstrumentation');const out=run(...args);if(!out.includes('UI_RESULT'))throw Error(out);return JSON.parse(out.split('UI_RESULT ')[1].split('\n')[0]);}
try{
 check('test target is a disposable emulator',run('shell','getprop','ro.kernel.qemu')==='1');
 run('install','-r','app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk');
 run('shell','am','start','-n','app.winku.ledger/.MainActivity');
 run('shell','cmd','notification','allow_listener','app.winku.ledger/app.winku.ledger.LedgerListener');await pause(700);
 const before=snapshot().tx.length,when=Date.now()-1000;
 run('shell','input','keyevent','HOME');await pause(1000);run('shell','am','kill','app.winku.ledger');await pause(500);
 check('UI process can stop while collector remains',!pid('app.winku.ledger').length&&!!pid('app.winku.ledger:collector'),{collector:pid('app.winku.ledger:collector')});
 post(201,'收款到账23.45元',when);await pause(500);check('notification auto-posts with UI closed',snapshot().tx.length===before+1);
 post(201,'收款到账23.45元',when);await pause(500);check('same notification update is deduplicated',snapshot().tx.length===before+1);
 post(202,'支付成功6.50元',when+1);await pause(500);check('expense auto-posts',snapshot().tx.some(t=>t.type==='expense'&&t.minorUnits===650));
 post(203,'张三向你转账100元，请收款',when+2);await pause(400);const pending=snapshot();check('unaccepted transfer remains pending',pending.tx.length===before+2&&pending.inbox.some(n=>n.status==='pending'&&n.cents===10000));
 post(204,'已支付88元',when+3,'朋友');await pause(400);check('ordinary chat is not booked',snapshot().tx.length===before+2);
 const mem=run('shell','dumpsys','meminfo','app.winku.ledger:collector');fs.writeFileSync('delivery/collector-memory.txt',mem);
 const proc=run('shell','ps','-A');check('collector process is independent',proc.includes('app.winku.ledger:collector'));
 const countBeforeReboot=snapshot().tx.length;
 run('reboot');
 let booted=false;for(let i=0;i<45;i++){await pause(1000);try{if(run('shell','getprop','sys.boot_completed')==='1'){booted=true;break;}}catch{}}
 check('emulator reboots',booted);run('shell','input','keyevent','82');await pause(1500);
 post(205,'收款到账8.88元',when+4);await pause(700);check('collection resumes after reboot without opening ledger',snapshot().tx.length===countBeforeReboot+1);
 const responsive=ui("(function(){goView('automation');return {view:state.view,overflow:document.documentElement.scrollWidth>innerWidth,navBottom:document.querySelector('.sidebar').getBoundingClientRect().bottom,height:innerHeight,connected:nativeStatus().connected};})()",'automation-light');
 check('automation UI fits phone viewport',responsive.view==='automation'&&!responsive.overflow&&responsive.navBottom<=responsive.height+1,responsive);
 const dark=ui("(function(){D().settings.theme='dark';save();goView('dashboard');return getComputedStyle(document.documentElement).getPropertyValue('--bg').trim();})()",'dashboard-dark');check('WinKu dark palette',dark==='#0E0E10',dark);
 const details=ui("(function(){D().settings.theme='light';save();goView('transactions');return {rows:document.querySelectorAll('.txn-row').length,overflow:document.documentElement.scrollWidth>innerWidth};})()",'transactions-light');check('ledger rows render without horizontal overflow',details.rows>0&&!details.overflow,details);
 run('shell','settings','put','system','font_scale','1.3');
 const font=ui("(function(){goView('automation');return {overflow:document.documentElement.scrollWidth>innerWidth,buttonCount:document.querySelectorAll('button').length};})()",'automation-large-font');check('large font automation has no page overflow',!font.overflow,font);run('shell','settings','put','system','font_scale','1.0');
 const inbox=ui("(function(){goView('automation');var b=JSON.parse(LedgerNative.getInbox('pending'));var n=b.items[0];document.querySelector('[data-inbox-edit=\"'+n.id+'\"]').click();return {amount:document.querySelector('#txnForm [name=amount]').value,note:!!document.querySelector('#txnForm [name=note]').value};})()",'inbox-review');check('inbox review prefills amount and note',!!inbox.amount&&inbox.note,inbox);
 for(const name of ['automation-light','dashboard-dark','transactions-light','automation-large-font','inbox-review'])run('pull','/sdcard/Android/data/app.winku.ledger/files/'+name+'.png','delivery/'+name+'.png');
 fs.writeFileSync('delivery/device-check.json',JSON.stringify({device:'Android emulator, API '+run('shell','getprop','ro.build.version.sdk'),results},null,2));console.log(JSON.stringify(results,null,2));
}catch(e){fs.writeFileSync('delivery/device-check.json',JSON.stringify({results,error:e.message},null,2));throw e;}
