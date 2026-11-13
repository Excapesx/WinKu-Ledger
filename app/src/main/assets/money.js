/* Small arithmetic parser: no eval/Function, compatible with the local CSP. */
function safeParseAmount(raw){
  var s=String(raw==null?'':raw).trim().replace(/[¥￥,，\s]/g,'').replace(/[×xX]/g,'*').replace(/÷/g,'/');
  if(!s||s.length>120||!/^[0-9+\-*/().]+$/.test(s)||/[+\-*/]{2,}/.test(s))return NaN;
  var at=0;
  function factor(){
    var sign=1;if(s[at]==='+'||s[at]==='-'){if(s[at++]==='-')sign=-1;}
    if(s[at]==='('){at++;var v=expression();if(s[at++]!==')')throw Error('parenthesis');return sign*v;}
    var m=/^(?:[0-9]+(?:\.[0-9]*)?|\.[0-9]+)/.exec(s.slice(at));if(!m)throw Error('number');at+=m[0].length;return sign*Number(m[0]);
  }
  function term(){var v=factor();while(s[at]==='*'||s[at]==='/'){var op=s[at++],n=factor();v=op==='*'?v*n:v/n;}return v;}
  function expression(){var v=term();while(s[at]==='+'||s[at]==='-'){var op=s[at++],n=term();v=op==='+'?v+n:v-n;}return v;}
  try{var value=expression();if(at!==s.length||!Number.isFinite(value))return NaN;return Math.abs(Math.round((value+Number.EPSILON)*100)/100);}catch(e){return NaN;}
}
