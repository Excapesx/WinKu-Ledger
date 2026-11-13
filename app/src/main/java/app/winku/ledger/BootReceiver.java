package app.winku.ledger;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.service.notification.NotificationListenerService;

public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        String action=intent.getAction();if(!Intent.ACTION_BOOT_COMPLETED.equals(action)&&!Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)&&!Intent.ACTION_USER_UNLOCKED.equals(action))return;
        try{NotificationListenerService.requestRebind(new ComponentName(context,LedgerListener.class));}catch(Exception ignored){}
    }
}
