package me.kevingleason.amessage;

/**
 * Created by GleasonK on 3/17/16.
 */
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PushAlarm extends BroadcastReceiver {
    NotificationManager nm;

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent myIntent = new Intent(context, PushService.class);
        context.startService(myIntent);
    }
}
