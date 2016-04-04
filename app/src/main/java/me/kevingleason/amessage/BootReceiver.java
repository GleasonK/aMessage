package me.kevingleason.amessage;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by GleasonK on 3/17/16.
 */
public class BootReceiver extends BroadcastReceiver {
    AlarmManager am;

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean enabled = sharedPrefs.getBoolean(MainActivity.SP_ENABLED, false);
        if (!enabled) return;

        am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        setRepeatingAlarm(context);
    }

    public void setRepeatingAlarm(Context context) {
        Intent intent = new Intent(context, PushAlarm.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
                (5 * 60 * 1000), pendingIntent); //wake up every 5 minutes to ensure service stays alive
    }
}
