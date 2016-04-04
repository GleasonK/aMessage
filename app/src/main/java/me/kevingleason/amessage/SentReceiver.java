package me.kevingleason.amessage;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by GleasonK on 3/25/16.
 */
public class SentReceiver extends BroadcastReceiver {
    private Pubnub pubnub;

    public SentReceiver(){}

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        boolean enabled = sp.getBoolean(MainActivity.SP_ENABLED, false);
        String cipher   = sp.getString(MainActivity.SP_CIPHER, "");
        String authkey  = sp.getString(MainActivity.SP_AUTHKEY, "");
        TelephonyManager tMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String mPhoneNumber = tMgr.getLine1Number();

        Log.d("SR-oR", "Message Sent Receipt");
        if (!enabled) return;

        final Bundle bundle = intent.getExtras();
        if (bundle == null) return;  // Ignore if no bundle

        boolean isSent = getResultCode() == Activity.RESULT_OK;
        long timeStamp = bundle.getLong(Config.PN_TIMESTAMP, 0l);
        String number = bundle.getString(Config.PN_NUMBER,"");

        this.pubnub = new Pubnub(
                Config.PUB_KEY,
                Config.SUB_KEY, // Dont need subscribe key
                Config.SECRET_KEY, // Dont need secret
                cipher,
                true
        );
        pubnub.setAuthKey(authkey);

        if(getResultCode() == Activity.RESULT_OK) {
            Toast.makeText(context, "SMS sent", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "SMS could not sent", Toast.LENGTH_SHORT).show();
        }

        JSONObject jsonMsg = new JSONObject();
        try {
            jsonMsg.put(Config.PN_TYPE, Config.PN_RECEIPT);
            jsonMsg.put(Config.PN_ISSENT, isSent);
            jsonMsg.put(Config.PN_NUMBER, number);
            jsonMsg.put(Config.PN_TIMESTAMP, timeStamp);
        } catch (JSONException e){
            e.printStackTrace();
        }
        Log.d("SR-oR", "Receipt: " + jsonMsg.toString());
        this.pubnub.publish(mPhoneNumber, jsonMsg, new Callback() {
            @Override
            public void successCallback(String channel, Object message) {
                Log.i("SR-sC","Sent on " + channel + " - " +message.toString());
            }
            @Override
            public void errorCallback(String channel, PubnubError error) {
                Log.e("SR-eC","Error: " + error.getErrorString());
            }
        });

    }
}
