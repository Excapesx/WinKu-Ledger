package app.winku.ledger;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.webkit.*;
import android.widget.FrameLayout;
import org.json.*;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class MainActivity extends Activity {
    private WebView web;
    private FrameLayout root;
    private LedgerStore store;
    private ValueCallback<Uri[]> fileCallback;
    private String exportContent;
    private String exportedName;
    private final BroadcastReceiver changes=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){refresh();}};
    private static final int PICK_FILE=21,EXPORT_FILE=22;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);store=new LedgerStore(this);
        getWindow().setStatusBarColor(Color.rgb(247,247,249));getWindow().setNavigationBarColor(Color.rgb(247,247,249));
        web=new WebView(this);web.setBackgroundColor(Color.rgb(247,247,249));
        root=new FrameLayout(this);root.setBackgroundColor(Color.rgb(247,247,249));root.addView(web,new FrameLayout.LayoutParams(-1,-1));
        if(Build.VERSION.SDK_INT>=30)root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return WindowInsets.CONSUMED;});
        setContentView(root);
        WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(true);s.setBlockNetworkLoads(true);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setTextZoom(Math.round(getResources().getConfiguration().fontScale*100));
        web.addJavascriptInterface(new Bridge(),"LedgerNative");
        web.setWebViewClient(new WebViewClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){
                Uri u=request.getUrl();if("https".equals(u.getScheme())&&"ledger.winku.local".equals(u.getHost())){
                    String path=u.getPath();String name="/".equals(path)?"index.html":path.substring(1);
                    if(!name.matches("[a-zA-Z0-9_.-]+"))return empty();
                    try{return new WebResourceResponse(name.endsWith(".js")?"application/javascript":name.endsWith(".css")?"text/css":"text/html","UTF-8",getAssets().open(name));}catch(Exception e){return empty();}
                }return empty();
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest r){return !"ledger.winku.local".equals(r.getUrl().getHost());}
            @Override public void onPageFinished(WebView view,String url){refresh();}
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onShowFileChooser(WebView w,ValueCallback<Uri[]> callback,FileChooserParams p){
                if(fileCallback!=null)fileCallback.onReceiveValue(null);fileCallback=callback;
                Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE,p.getMode()==FileChooserParams.MODE_OPEN_MULTIPLE);
                try{startActivityForResult(i,PICK_FILE);}catch(ActivityNotFoundException e){fileCallback.onReceiveValue(null);fileCallback=null;message("没有可用的文件选择器");}return true;
            }
        });
        web.loadUrl("https://ledger.winku.local/index.html");
        IntentFilter changed=new IntentFilter(getPackageName()+".DATA_CHANGED");if(Build.VERSION.SDK_INT>=33)registerReceiver(changes,changed,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(changes,changed);
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->handleBack());
    }
    private static WebResourceResponse empty(){return new WebResourceResponse("text/plain","UTF-8",new java.io.ByteArrayInputStream(new byte[0]));}
    @Override public void onResume(){super.onResume();refresh();}
    private void refresh(){if(web!=null)web.evaluateJavascript("window.nativeRefresh && window.nativeRefresh()",null);}
    private void message(String text){runOnUiThread(()->{if(web!=null)web.evaluateJavascript("window.toast && toast("+JSONObject.quote(text)+",true)",null);});}
    private void handleBack(){web.evaluateJavascript("(function(){if(document.querySelector('#modalRoot').children.length){closeModal();return true;}if(window.state&&state.view!=='dashboard'){state.view='dashboard';renderView();return true;}return false;})()",v->{if(!"true".equals(v))finish();});}
    // Android 13+ uses the platform dispatcher registered in onCreate; this override supports 8–12.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed(){handleBack();}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
        if(request==PICK_FILE&&fileCallback!=null){fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));fileCallback=null;}
        if(request==EXPORT_FILE){String content=exportContent;exportContent=null;if(result==RESULT_OK&&data!=null&&data.getData()!=null&&content!=null){Uri uri=data.getData();new Thread(()->{try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new java.io.IOException();out.write(content.getBytes(StandardCharsets.UTF_8));runOnUiThread(()->{if(web!=null)web.evaluateJavascript("toast('已保存备份文件')",null);});}catch(Exception e){message("保存失败，请选择其他位置重试");}},"ledger-export").start();}}
    }
    @Override protected void onDestroy(){unregisterReceiver(changes);if(fileCallback!=null)fileCallback.onReceiveValue(null);web.removeJavascriptInterface("LedgerNative");web.destroy();web=null;store.close();super.onDestroy();}
    final class Bridge {
        @JavascriptInterface public String getData(){try{return store.load().toString();}catch(Exception e){return new JSONObject().toString();}}
        @JavascriptInterface public String saveData(String base,String next){try{store.save(new JSONObject(base),new JSONObject(next));return new JSONObject().put("ok",true).put("data",store.load()).toString();}catch(Exception e){return "{\"ok\":false,\"error\":\"保存失败，账目未覆盖；请检查数据和剩余空间\"}";}}
        @JavascriptInterface public String getStatus(){try{
            JSONObject s=store.status();String enabled=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");ComponentName name=new ComponentName(MainActivity.this,LedgerListener.class);boolean granted=false;if(enabled!=null)for(String v:enabled.split(":"))if(name.equals(ComponentName.unflattenFromString(v)))granted=true;
            boolean running=false;ActivityManager am=(ActivityManager)getSystemService(ACTIVITY_SERVICE);for(ActivityManager.RunningServiceInfo r:am.getRunningServices(100))if(name.equals(r.service))running=true;
            PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
            return s.put("permission",granted).put("connected",granted&&running&&s.optBoolean("connected")).put("batteryExempt",pm.isIgnoringBatteryOptimizations(getPackageName())).put("config",store.config()).put("version","1.0.0").toString();
        }catch(Exception e){return "{\"permission\":false,\"connected\":false,\"config\":{}}";}}
        @JavascriptInterface public String getInbox(String filter){try{return store.inbox(filter,200).toString();}catch(Exception e){return "{\"items\":[],\"error\":\"读取失败\"}";}}
        @JavascriptInterface public String configure(String value){try{store.configure(new JSONObject(value));return "{\"ok\":true}";}catch(Exception e){return "{\"ok\":false}";}}
        @JavascriptInterface public void resolve(String id,String action){try{store.resolve(id,action);}catch(Exception e){message("处理失败，请重试");}}
        @JavascriptInterface public void clearInbox(){store.clearInbox();}
        @JavascriptInterface public void openSettings(String kind){runOnUiThread(()->{
            Intent i;
            if("notifications".equals(kind))i=new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            else if("battery".equals(kind))i=new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            else i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()));
            try{startActivity(i);}catch(ActivityNotFoundException e){message("请在系统设置中搜索通知使用权或后台运行");}
        });}
        @JavascriptInterface public void reconnect(){try{android.service.notification.NotificationListenerService.requestRebind(new ComponentName(MainActivity.this,LedgerListener.class));}catch(Exception e){message("请先开启通知使用权");}}
        @JavascriptInterface public void setTheme(String theme){runOnUiThread(()->{boolean dark="dark".equals(theme);int color=Color.parseColor(dark?"#0E0E10":"#F7F7F9");getWindow().setStatusBarColor(color);getWindow().setNavigationBarColor(color);web.setBackgroundColor(color);root.setBackgroundColor(color);getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);});}
        @JavascriptInterface public void exportFile(String name,String content,String mime){runOnUiThread(()->{
            if(exportContent!=null){message("请先完成上一次导出");return;}exportContent=content;exportedName=name;
            Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(mime.split(";")[0]).putExtra(Intent.EXTRA_TITLE,name);
            try{startActivityForResult(i,EXPORT_FILE);}catch(ActivityNotFoundException e){exportContent=null;message("没有可用的文件保存器");}
        });}
    }
}
