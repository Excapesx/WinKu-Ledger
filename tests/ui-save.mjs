import {execFileSync} from 'node:child_process';
import fs from 'node:fs';
const adb=process.env.ADB_PATH||'D:/Documents/ChatGPT/111/DSH-Android/apps/android/toolchains/local/android-sdk/platform-tools/adb.exe';
const run=(...a)=>execFileSync(adb,a,{encoding:'utf8',timeout:45000,windowsHide:true}).trim();
if(run('shell','getprop','ro.kernel.qemu')!=='1')throw Error('Run on disposable emulator only');
const js=`(function(){openTxnModal();var f=document.querySelector('#txnForm');f.querySelector('[name=amount]').value='35+12.5';f.querySelector('[name=note]').value='CSP arithmetic integration test';f.querySelector('[name=keepOpen]').checked=false;saveTxn(f);var stored=JSON.parse(LedgerNative.getData());var t=stored.txns.find(x=>x.note==='CSP arithmetic integration test');return {amount:t&&t.amount,saved:!!t,closed:!document.querySelector('#txnForm')};})()`;
const output=run('shell','am','instrument','-w','-e','ui64',Buffer.from(js).toString('base64'),'app.winku.ledger.test/app.winku.ledger.LedgerInstrumentation');
fs.writeFileSync('delivery/ui-save-check.txt',output);
const r=JSON.parse(output.split('UI_RESULT ')[1].split('\n')[0]);if(r.amount!==47.5||!r.saved||!r.closed)throw Error(output);console.log('PASS: manual arithmetic entry saved through WebView/CSP/SQLite, amount 47.50');
