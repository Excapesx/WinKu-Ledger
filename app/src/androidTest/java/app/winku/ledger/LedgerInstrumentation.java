package app.winku.ledger;
import android.app.*;
import android.os.Bundle;
import org.json.*;

public class LedgerInstrumentation extends Instrumentation {
    int checks=0;
    Bundle args;
    private void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    @Override public void onCreate(Bundle args){super.onCreate(args);this.args=args;start();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        if(args!=null&&args.containsKey("ui64")){
            String script=new String(android.util.Base64.decode(args.getString("ui64"),android.util.Base64.DEFAULT),java.nio.charset.StandardCharsets.UTF_8);
            Activity activity=startActivitySync(new android.content.Intent(getTargetContext(),MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
            android.webkit.WebView web=(android.webkit.WebView)((android.view.ViewGroup)((android.view.ViewGroup)activity.findViewById(android.R.id.content)).getChildAt(0)).getChildAt(0);
            java.util.concurrent.CountDownLatch ready=new java.util.concurrent.CountDownLatch(1);String[] value={"null"};
            Thread.sleep(1200);runOnMainSync(()->web.evaluateJavascript(script,v->{value[0]=v;ready.countDown();}));
            if(!ready.await(10,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("UI timeout");
            if(args.containsKey("capture")){
                java.util.concurrent.CountDownLatch painted=new java.util.concurrent.CountDownLatch(1);
                runOnMainSync(()->web.postVisualStateCallback(android.os.SystemClock.uptimeMillis(),new android.webkit.WebView.VisualStateCallback(){@Override public void onComplete(long id){painted.countDown();}}));
                if(!painted.await(10,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("UI paint timeout");
                getUiAutomation().waitForIdle(500,5000);String name=args.getString("capture").replaceAll("[^a-zA-Z0-9_-]","");
                try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(getTargetContext().getExternalFilesDir(null),name+".png"))){getUiAutomation().takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
            }
            result.putString("stream","UI_RESULT "+value[0]+"\n");finish(Activity.RESULT_OK,result);return;
        }
        getTargetContext().deleteDatabase("ledger-tests.db");LedgerStore store=new LedgerStore(getTargetContext(),"ledger-tests.db");
        JSONObject base=store.load();check(base.getJSONArray("txns").length()==0,"fresh ledger empty");
        String wx="com.tencent.mm";long now=System.currentTimeMillis();
        store.capture(wx,"key-1",now,"微信支付","收款到账 12.34元");
        check(store.load().getJSONArray("txns").length()==1,"clear income posts");
        check(store.load().getJSONArray("txns").getJSONObject(0).getLong("minorUnits")==1234,"exact cents");
        store.capture(wx,"key-1",now,"微信支付","收款到账 12.34元");
        check(store.load().getJSONArray("txns").length()==1,"notification update dedup");
        store.capture(wx,"key-2",now+1,"微信支付","收款到账 12.34元");
        check(store.load().getJSONArray("txns").length()==2,"same amount different transaction not lost");
        store.capture(wx,"key-3",now,"好友","向你转账 50.00元，请收款");
        check(store.load().getJSONArray("txns").length()==2,"pending transfer not income");
        check(store.inbox("pending",100).getJSONArray("items").length()==1,"ambiguous saved in inbox");
        store.capture(wx,"key-4",now,"好友","我已支付100元");check(store.load().getJSONArray("txns").length()==2,"chat not posted");
        store.capture("com.unknown","key-5",now,"支付成功","支付成功 888元");check(store.load().getJSONArray("txns").length()==2,"non-target filtered");
        JSONObject next=new JSONObject(base.toString());next.getJSONObject("settings").put("privacy",true);store.save(base,next);
        check(store.load().getJSONArray("txns").length()==2,"UI stale snapshot preserves incoming transactions");
        check(store.load().getJSONObject("settings").getBoolean("privacy"),"UI setting persisted");
        JSONObject b2=store.load(),n2=new JSONObject(b2.toString());n2.getJSONArray("txns").remove(0);store.save(b2,n2);check(store.load().getJSONArray("txns").length()==1,"explicit deletion works");
        store.configure(new JSONObject().put("enabled",false));store.capture(wx,"disabled",now,"微信支付","收款到账 8元");check(store.load().getJSONArray("txns").length()==1,"paused does not collect");
        store.configure(new JSONObject().put("enabled",true).put("allNotifications",true));store.capture(wx,"chat",now,"朋友","你好");check(store.inbox("all",100).optInt("ignored")==1,"opt-in ordinary notification archive");
        store.clearInbox();check(store.load().getJSONArray("txns").length()==1,"clearing originals preserves ledger");
        store.close();store=new LedgerStore(getTargetContext(),"ledger-tests.db");check(store.load().getJSONArray("txns").length()==1,"process restart persistence");store.close();getTargetContext().deleteDatabase("ledger-tests.db");
        result.putString("stream","PASS: "+checks+" SQLite/collector integration assertions\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("stream","FAIL after "+checks+": "+e+"\n"+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
