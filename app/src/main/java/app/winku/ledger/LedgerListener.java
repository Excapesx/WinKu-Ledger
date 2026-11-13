package app.winku.ledger;

import android.app.Notification;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.LinkedHashSet;

public final class LedgerListener extends NotificationListenerService {
    private HandlerThread thread;
    private Handler worker;
    private LedgerStore store;
    @Override public void onCreate(){super.onCreate();store=new LedgerStore(this);thread=new HandlerThread("ledger-writes",android.os.Process.THREAD_PRIORITY_BACKGROUND);thread.start();worker=new Handler(thread.getLooper());}
    @Override public void onListenerConnected(){super.onListenerConnected();worker.post(()->{store.status("connected",true);store.status("connectedAt",System.currentTimeMillis());});try{StatusBarNotification[] active=getActiveNotifications();if(active!=null)for(StatusBarNotification s:active)onNotificationPosted(s);}catch(SecurityException ignored){}}
    @Override public void onNotificationPosted(StatusBarNotification s){
        Notification n=s.getNotification();
        if(n==null || (n.flags&Notification.FLAG_GROUP_SUMMARY)!=0)return;
        Bundle b=n.extras;if(b==null)return;
        String title=text(b.getCharSequence(Notification.EXTRA_TITLE));
        String content=text(b.getCharSequence(Notification.EXTRA_BIG_TEXT));
        if(content.isEmpty())content=text(b.getCharSequence(Notification.EXTRA_TEXT));
        if(content.isEmpty()){
            CharSequence[] lines=b.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            if(lines!=null){LinkedHashSet<String> unique=new LinkedHashSet<>();for(CharSequence line:lines)unique.add(text(line));content=String.join("\n",unique);}
        }
        if(title.isEmpty()&&content.isEmpty())return;
        final String body=content, pkg=s.getPackageName(),key=s.getKey();
        final long event=n.when>0&&n.when<=System.currentTimeMillis()+60000?n.when:s.getPostTime();
        worker.post(()->{try{if(store.accepts(pkg)){TransactionParser.Result r=TransactionParser.parse(pkg,title,body);if(r.candidate||store.config().optBoolean("allNotifications")){store.capture(pkg,key,event,title,body);sendBroadcast(new android.content.Intent(getPackageName()+".DATA_CHANGED").setPackage(getPackageName()));}}}catch(Exception e){store.status("lastError","通知保存失败："+e.getClass().getSimpleName());}});
    }
    private static String text(CharSequence s){if(s==null)return "";String v=s.toString();return v.length()>12000?v.substring(0,12000):v;}
    @Override public void onListenerDisconnected(){worker.post(()->store.status("connected",false));requestRebind(new ComponentName(this,LedgerListener.class));super.onListenerDisconnected();}
    @Override public void onDestroy(){store.status("connected",false);worker.post(()->store.close());thread.quitSafely();super.onDestroy();}
}
