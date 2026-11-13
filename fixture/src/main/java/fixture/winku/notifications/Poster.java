package fixture.winku.notifications;
import android.app.*;
import android.content.Intent;
import android.os.Bundle;
/** TEST ONLY: never distribute or install on a user's device. */
public class Poster extends Activity {
    @Override public void onCreate(Bundle b){super.onCreate(b);Intent i=getIntent();NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("test","自动记账集成测试",NotificationManager.IMPORTANCE_DEFAULT));int id=i.getIntExtra("id",101);String title=i.getStringExtra("title"),text=i.getStringExtra("text");long time=i.getLongExtra("when",System.currentTimeMillis());nm.notify(id,new Notification.Builder(this,"test").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title==null?"微信支付":title).setContentText(text==null?"收款到账 12.34元":text).setStyle(new Notification.BigTextStyle().bigText(text==null?"收款到账 12.34元":text)).setWhen(time).build());finish();}
}
