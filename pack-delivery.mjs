import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
const stage='delivery/source/WinKuLedger';fs.mkdirSync(stage,{recursive:true});
const dirs=['app/src','fixture/src','gradle','tests','reference','.impeccable'];
for(const dir of dirs)fs.cpSync(dir,path.join(stage,dir),{recursive:true,filter:p=>!p.split(/[\\/]/).includes('classes')});
const files=['settings.gradle.kts','build.gradle.kts','gradle.properties','gradlew','gradlew.bat','app/build.gradle.kts','app/proguard-rules.pro','fixture/build.gradle.kts','README.md','AUTHORS.md','PRODUCT.md','DESIGN.md','.gitignore','prepare-assets.mjs','create-signing-key.mjs','pack-delivery.mjs'];
for(const file of files){fs.mkdirSync(path.dirname(path.join(stage,file)),{recursive:true});fs.copyFileSync(file,path.join(stage,file));}
fs.writeFileSync(path.join(stage,'local.properties.example'),'sdk.dir=C\\:/Users/YourName/AppData/Local/Android/Sdk\n');
fs.copyFileSync('README.md','delivery/使用说明.md');
const apk='delivery/WinKu小账本-v1.0.0.apk';const data=fs.readFileSync(apk);const sha=crypto.createHash('sha256').update(data).digest('hex');
fs.writeFileSync('delivery/SHA256.txt',sha+'  WinKu小账本-v1.0.0.apk\n');
console.log(JSON.stringify({apk,bytes:data.length,kib:(data.length/1024).toFixed(1),sha256:sha,sourceStage:stage}));
