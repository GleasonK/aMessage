package me.kevingleason.amessage;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;

import org.json.JSONException;
import org.json.JSONObject;

public class MessageReceiver extends BroadcastReceiver {
    // Get the object of SmsManager
    SmsMessage[] msgs;
    private Pubnub pubnub;

    public MessageReceiver() {}

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        boolean enabled = sp.getBoolean(MainActivity.SP_ENABLED, false);
        String cipher   = sp.getString(MainActivity.SP_CIPHER, "");
        String authkey  = sp.getString(MainActivity.SP_AUTHKEY, "");
        TelephonyManager tMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String mPhoneNumber = tMgr.getLine1Number();
        Log.d("MR-oR","Receiving Message, Enabled: " + enabled);
        if (!enabled) return;

        final Bundle bundle = intent.getExtras();
        if (bundle == null) return;  // Ignore if no bundle

        try {
            if (Build.VERSION.SDK_INT >= 19) { //KITKAT
                msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            } else {
                Object pdus[] = (Object[]) bundle.get("pdus");
                msgs = new SmsMessage[pdus.length];
                for (int i = 0; i < pdus.length; i++) {
                    msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[0]);
                }
            }
        } catch (Exception e){ // In case of null pointer?
            e.printStackTrace();
        }

        this.pubnub = new Pubnub(
                Config.PUB_KEY,
                Config.SUB_KEY, // Dont need subscribe key
                Config.SECRET_KEY, // Dont need secret
                cipher,
                true
        );
        this.pubnub.setAuthKey(authkey);

        for (int i = 0; i < msgs.length; i++) {
            SmsMessage currentMessage = msgs[i];
            String senderNum = currentMessage.getDisplayOriginatingAddress();
            String name = getContactName(context, senderNum);
            String message = currentMessage.getDisplayMessageBody();

            Log.i("SmsReceiver", "senderNum: " + senderNum + "; message: " + message);
            JSONObject jsonMsg = new JSONObject();
            try {
                jsonMsg.put(Config.PN_TYPE, Config.PN_INCOMING);
                jsonMsg.put(Config.PN_SENDER, senderNum);
                jsonMsg.put(Config.PN_NAME, name);
                jsonMsg.put(Config.PN_MESSAGE, message);
            } catch (JSONException e){
                e.printStackTrace();
            }
            this.pubnub.publish(mPhoneNumber, jsonMsg, new Callback() {
                @Override
                public void successCallback(String channel, Object message) {
                    Log.i("MR-oR","Sent on " + channel + " - " +message.toString());
                }
                @Override
                public void errorCallback(String channel, PubnubError error) {
                    Log.e("MR-oR","Error: " + error.getErrorString());
                }
            });
        } // end for loop


    }

    public static String getContactName(Context context, String phoneNumber) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        if (cursor == null) {
            return null;
        }
        String contactName = phoneNumber;
        if(cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }

        if(cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return contactName;
    }
}
