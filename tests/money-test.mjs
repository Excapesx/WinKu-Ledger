import fs from 'node:fs';
import vm from 'node:vm';
const context={};vm.createContext(context);vm.runInContext(fs.readFileSync('app/src/main/assets/money.js','utf8'),context);
const cases=[['23.45',23.45],['35+12.5',47.5],['(10+2)*3',36],['10/4',2.5],['￥1,234.56',1234.56],['2×3+1',7],['10÷2',5],['-12.30',12.3],['.5+.25',.75],['1.005',1.01],['1/0',NaN],['',NaN],['alert(1)',NaN],['1;2',NaN],['1..2',NaN],['2**3',NaN],['(2+3',NaN],['3)(',NaN],['4--2',NaN],['1e3',NaN]];
for(const [s,want] of cases){const got=context.safeParseAmount(s);if(!(Number.isNaN(want)?Number.isNaN(got):got===want))throw Error(s+': '+got+' != '+want);}
console.log('PASS: '+cases.length+' amount parser cases; no eval required');
