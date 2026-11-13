import fs from 'node:fs';
import crypto from 'node:crypto';
import {execFileSync} from 'node:child_process';
fs.mkdirSync('.signing',{recursive:true});
if(!fs.existsSync('.signing/ledger-release.p12')){
  const secret=crypto.randomBytes(24).toString('hex');
  execFileSync('C:/Program Files/Android/Android Studio/jbr/bin/keytool.exe',['-genkeypair','-keystore','.signing/ledger-release.p12','-storetype','PKCS12','-storepass:env','WINKU_LEDGER_STOREPASS','-keypass:env','WINKU_LEDGER_STOREPASS','-alias','winku-ledger','-keyalg','RSA','-keysize','3072','-validity','36500','-dname','CN=WinKu Ledger, OU=Personal App, O=WinKu Ledger, C=CN'],{env:{...process.env,WINKU_LEDGER_STOREPASS:secret},stdio:'ignore',windowsHide:true});
  fs.writeFileSync('.signing/signing.properties','storeFile=.signing/ledger-release.p12\nstorePassword='+secret+'\n');
}
console.log('Local release signing key ready (not included in source archive).');
