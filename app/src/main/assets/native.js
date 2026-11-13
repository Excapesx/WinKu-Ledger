/* Android platform bridge. No timers, network requests or background WebView. */
var nativeBase='', inboxFilter='pending', nativeLastSnapshot='';
parseAmount=safeParseAmount;
function nativeSave(){
  try{
    var result=JSON.parse(LedgerNative.saveData(nativeBase||'{}',JSON.stringify(state.data)));
    if(!result.ok)throw Error(result.error||'保存失败');
    state.data=result.data;nativeBase=JSON.stringify(result.data);nativeLastSnapshot=nativeBase;return true;
  }catch(e){toast(e.message||'保存失败，原账目仍在本机',true);throw e;}
}
var originalLoad=load;
load=function(){
  var raw=JSON.parse(LedgerNative.getData());
  if(!Array.isArray(raw.txns)||!Array.isArray(raw.accounts))throw Error('本机账本无法读取，请重启应用，不要清除数据');
  nativeBase=JSON.stringify(raw);originalLoad();nativeBase=JSON.stringify(state.data);nativeLastSnapshot=nativeBase;return state.data;
};
var originalTheme=applyTheme;
applyTheme=function(){originalTheme();LedgerNative.setTheme(D().settings.theme);};
function nativeStatus(){return JSON.parse(LedgerNative.getStatus());}
function statusText(s){return !s.config.enabled?'自动记录已暂停':!s.permission?'先开启通知读取':s.connected?'正在监听通知':'等待系统连接';}
function statusStrip(){var s=nativeStatus(), ready=s.connected&&s.config.enabled;return '<button class="auto-strip '+(ready?'ready':'')+'" data-act="goto" data-view="automation"><span class="status-dot"></span><span><strong>'+statusText(s)+'</strong><small>'+(ready?'微信、支付宝的新交易通知将自动识别':'进入自动记账，完成一次设置')+'</small></span>'+icon('i-right')+'</button>';}
var originalDashboard=viewDashboard;
viewDashboard=function(){return statusStrip()+originalDashboard();};
var originalSettings=viewSettings;
viewSettings=function(){return '<div class="quick-links"><button class="btn" data-act="goto" data-view="automation">自动记账</button><button class="btn" data-act="goto" data-view="accounts">账户管理</button><button class="btn" data-act="goto" data-view="budget">预算与目标</button><button class="btn" id="native-theme">切换深浅主题</button><button class="btn" id="native-privacy">隐藏／显示金额</button></div>'+originalSettings()+'<p class="source-note">WinKu 小账本 1.0.0 · 全部账目保存在本机。通知原文默认保留 30 天、最多 5,000 条；已入账的流水长期保留。卸载或清除应用数据会删除本机账本，请先导出备份。账户金额是已记录流水推算值，并非平台实时余额。自动识别的付款账户默认归入对应平台，实际银行卡支付可在明细中调整。</p>';};
VIEW_META.automation=['自动记账','通知到达，账目自动留下'];
function platformName(p){return {'com.tencent.mm':'微信','com.eg.android.AlipayGphone':'支付宝','com.unionpay':'云闪付'}[p]||p;}
function inboxRows(box){
  if(!box.items.length)return '<div class="empty-inbox"><strong>'+(inboxFilter==='pending'?'没有待核对的通知':'还没有捕捉到通知')+'</strong>'+(inboxFilter==='pending'?'明确的交易会直接入账；不确定的通知会留在这里。':'开启权限后，等待平台发出新通知。普通聊天默认不保存。')+'</div>';
  return box.items.map(function(n){
    var labels={posted:'已自动入账',pending:'待核对',resolved:'已处理',ignored:'已忽略'};
    var amount=n.cents?money(n.cents/100):'金额不明';
    return '<article class="inbox-row"><header><div><h4>'+esc(n.title||platformName(n.pkg))+'</h4><small>'+esc(platformName(n.pkg))+' · '+new Date(n.time).toLocaleString('zh-CN')+'</small></div><strong class="num">'+(privacy()?'••••':amount)+'</strong></header><p>'+(privacy()?'通知内容已隐藏':esc(n.body))+'</p><div class="reason '+(n.status==='posted'?'posted':'')+'">'+esc(labels[n.status]||n.status)+' · '+esc(n.reason)+'</div>'+(n.status==='pending'?'<footer><button class="btn btn-sm" data-inbox-edit="'+n.id+'">核对并记账</button><button class="btn btn-sm btn-ghost" data-inbox-dismiss="'+n.id+'">忽略</button></footer>':'')+'</article>';
  }).join('');
}
function viewAutomation(){
  var s=nativeStatus(),c=s.config,box=JSON.parse(LedgerNative.getInbox(inboxFilter));
  var ready=s.connected&&c.enabled;
  return '<div class="auto-page"><div class="auto-heading"><div><h2>'+statusText(s)+'</h2><p>'+(ready?'不必打开支付页面，收到通知即可识别。':'完成下面的设置，之后无需逐笔填写。')+'</p></div>'+icon('i-spark')+'</div>'
  +'<div class="setup-panel"><div class="setup-row"><div><h3>通知使用权'+(s.permission?' · 已开启':' · 未开启')+'</h3><p>允许读取通知，并只处理你选择的平台。</p></div><button class="btn '+(!s.permission?'btn-primary':'')+'" data-native-settings="notifications">'+(s.permission?'管理':'去开启')+'</button></div>'
  +'<div class="setup-row"><div><h3>后台运行与开机恢复</h3><p>在应用设置中允许自启动、后台运行；具体名称因手机而异。重启后解锁手机，系统会尝试恢复监听。</p></div><button class="btn" data-native-settings="app">去设置</button></div>'
  +'<div class="setup-row"><div><h3>电池优化'+(s.batteryExempt?' · 已豁免':'')+'</h3><p>若出现漏记，可将本应用设为不受限制。</p></div><button class="btn" data-native-settings="battery">管理</button></div>'
  +'<div class="setup-row"><div><h3>自动记录</h3><p>明确完成的收付款直接入账，不弹窗打断。</p></div><label class="settings-check"><input id="auto-enabled" type="checkbox" '+(c.enabled?'checked':'')+' aria-label="开启自动记录"></label></div></div>'
  +'<section class="auto-section"><h3>监听平台</h3><p>只保留含交易线索的通知。平台通知中必须包含交易信息，才能自动记账。</p><div class="platforms">'+[['wechat','微信'],['alipay','支付宝'],['unionpay','云闪付']].map(function(p){return '<label class="platform-chip"><input type="checkbox" data-platform="'+p[0]+'" '+(c[p[0]]?'checked':'')+'>'+p[1]+'</label>';}).join('')+'</div></section>'
  +'<section class="auto-section"><div class="card-head"><h3>通知收件箱</h3><button class="btn btn-sm btn-ghost" id="native-refresh">刷新</button></div><p>退款单列为退款收入；未确认转账、多个金额和来源不明的消息不会直接入账。</p><div class="inbox-tabs"><button data-inbox-tab="pending" class="'+(inboxFilter==='pending'?'active':'')+'">待核对 '+(box.pending||0)+'</button><button data-inbox-tab="all" class="'+(inboxFilter==='all'?'active':'')+'">全部通知</button></div>'+inboxRows(box)+'</section>'
  +'<details class="auto-details"><summary>记录设置与运行说明</summary><label for="native-retention">通知原文保留时间</label><select id="native-retention">'+[[7,'7 天'],[30,'30 天'],[90,'90 天'],[365,'365 天']].map(function(x){return '<option value="'+x[0]+'" '+(c.retainDays===x[0]?'selected':'')+'>'+x[1]+'</option>';}).join('')+'</select><label class="settings-check"><input id="native-all" type="checkbox" '+(c.allNotifications?'checked':'')+'>同时保存所选平台的非交易通知</label><p>开启后可能保存聊天内容，仅在本机保留，不会自动把普通聊天记成账。</p><label for="native-packages">其他平台包名（用逗号分隔）</label><input id="native-packages" type="text" value="'+esc(c.custom||'')+'" placeholder="例如：com.example.wallet"><button class="btn btn-sm" id="native-save-packages">保存其他平台</button><p>其他平台先保存通知供核对，不直接使用微信、支付宝的规则。通知原文最多 5,000 条；清理原文不会删除流水。</p><div class="quick-links"><button class="btn" id="native-reconnect">请求重新连接</button><button class="btn btn-danger" id="native-clear-inbox">清空通知原文</button><button class="btn" id="native-export-inbox">导出最近 200 条通知</button></div><p>后台仅由通知事件触发，不持续截图，不轮询，不联网。系统限制、强行停止、权限撤销仍可能导致中断。没有发出的通知和中断期间已消失的通知无法补回。通知规则属于识别推断，建议核对首次识别结果；普通转出可能是借款或内部转移，可在流水中调整。</p>'+(s.lastError?'<p class="native-error">'+esc(s.lastError)+'</p>':'')+'</details></div>';
}
var originalRender=renderView;
renderView=function(keepFocus){
  $('#fab').style.display=['automation','settings','reports'].includes(state.view)?'none':'';
  if(state.view!=='automation'){originalRender(keepFocus);return;}
  $('#viewTitle').textContent='自动记账';$('#viewSubtitle').textContent='通知到达，账目自动留下';$('#monthPicker').style.display='none';
  $$('.nav-item').forEach(function(b){b.classList.toggle('active',b.dataset.view==='automation');});
  $('#content').innerHTML=viewAutomation();applyTheme();applyPrivacyBtn();
};
function nativeRefresh(){
  if(!window.state||!state.data)return;
  if($('#modalRoot').children.length||/INPUT|TEXTAREA|SELECT/.test((document.activeElement||{}).tagName||''))return;
  try{var raw=LedgerNative.getData();var parsed=JSON.parse(raw);if(!Array.isArray(parsed.txns))return;state.data=parsed;nativeBase=raw;nativeLastSnapshot=raw;renderView();}catch(e){toast('刷新失败，已有账目保留在本机',true);}
}
function configureNative(patch){var r=JSON.parse(LedgerNative.configure(JSON.stringify(patch)));if(!r.ok){toast('设置未能保存，请重试',true);return;}nativeRefresh();}
document.addEventListener('change',function(e){
  var t=e.target,patch={};
  if(t.dataset.platform){patch[t.dataset.platform]=t.checked;configureNative(patch);}
  else if(t.id==='auto-enabled')configureNative({enabled:t.checked});
  else if(t.id==='native-retention')configureNative({retainDays:+t.value});
  else if(t.id==='native-all')configureNative({allNotifications:t.checked});
});
var pendingNativeInbox='';
document.addEventListener('click',function(e){
  var t=e.target.closest('button');if(!t)return;
  if(t.dataset.nativeSettings){LedgerNative.openSettings(t.dataset.nativeSettings);return;}
  if(t.dataset.inboxTab){inboxFilter=t.dataset.inboxTab;renderView();return;}
  if(t.dataset.inboxDismiss){LedgerNative.resolve(t.dataset.inboxDismiss,'discard');renderView();return;}
  if(t.dataset.inboxEdit){
    var n=JSON.parse(LedgerNative.getInbox('pending')).items.find(function(x){return x.id===t.dataset.inboxEdit;});if(!n)return;
    openTxnModal();pendingNativeInbox=n.id;
    // Existing transaction form remains the single path for choosing accounts and direction.
    var amount=$('#txnForm [name="amount"]');if(amount&&n.cents)amount.value=(n.cents/100).toFixed(2);
    var note=$('#txnForm [name="note"]');if(note)note.value=(n.title+' · '+n.body).slice(0,60);
    var account=$('#txnForm [name="accountId"]');if(account)account.value=n.pkg==='com.tencent.mm'?'acc_wx':n.pkg==='com.eg.android.AlipayGphone'?'acc_ali':'acc_union';
    var keep=$('#txnForm [name="keepOpen"]');if(keep)keep.checked=false;
    var date=new Date(n.time);$('#txnForm [name="date"]').value=todayStr(date);$('#txnForm [name="time"]').value=pad2(date.getHours())+':'+pad2(date.getMinutes());
    if(n.type==='income'||n.type==='refund'){modalCtx.type='income';syncTxnTypeUI();}
    return;
  }
  switch(t.id){
    case 'native-refresh':nativeRefresh();break;
    case 'native-reconnect':LedgerNative.reconnect();toast('已请求系统重新连接，请稍后刷新状态');break;
    case 'native-save-packages':configureNative({custom:$('#native-packages').value.trim()});toast('其他平台已保存');break;
    case 'native-clear-inbox':confirmDialog('清空通知原文','会清空全部已捕捉的通知，已入账的流水保持保留。',function(){LedgerNative.clearInbox();closeModal();renderView();toast('通知原文已清空');},true);break;
    case 'native-export-inbox':LedgerNative.exportFile('WinKu-通知诊断-'+todayStr()+'.json',JSON.stringify(JSON.parse(LedgerNative.getInbox('all')),null,2),'application/json');break;
    case 'native-theme':D().settings.theme=D().settings.theme==='dark'?'light':'dark';save();renderView();break;
    case 'native-privacy':D().settings.privacy=!D().settings.privacy;save();renderView();break;
  }
});
// Mark an inbox item resolved only when the ledger actually gained a transaction.
var originalTxnSave=typeof saveTxn==='function'?saveTxn:null;
if(originalTxnSave)saveTxn=function(){var before=D().txns.length;var id=pendingNativeInbox;originalTxnSave.apply(this,arguments);if(id&&D().txns.length>before){LedgerNative.resolve(id,'resolve');pendingNativeInbox='';renderView();}};
var originalCloseModal=closeModal;
closeModal=function(){originalCloseModal();pendingNativeInbox='';};
