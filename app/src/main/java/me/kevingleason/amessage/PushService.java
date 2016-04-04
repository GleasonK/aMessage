package me.kevingleason.amessage;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.AsyncTask.Status;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


/**
 * Created by GleasonK on 3/17/16.
 */
public class PushService extends Service {
    static Pubnub pubnub;
    SharedPreferences mSharedPrefs;
    PushReceiver mPushReceiver = new PushReceiver();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d("PS-oC", "Creating Service");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.d("PS-oD","PushService Destroyed");
        pubnub.unsubscribeAll();
        super.onDestroy();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        handleCommand();
        Log.d("PS-oS", "Starting Service");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("PS-oSC", "On Start Command");
        return handleCommand();
    }

    public int handleCommand(){
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean enabled = mSharedPrefs.getBoolean(MainActivity.SP_ENABLED, false);
        String cipher   = mSharedPrefs.getString(MainActivity.SP_CIPHER, "");
        String authkey  = mSharedPrefs.getString(MainActivity.SP_AUTHKEY, "");
        Log.d("PS-oSC", "Enabled " + enabled);
        if (!enabled) return START_STICKY;

        if (pubnub != null) { pubnub.unsubscribeAll(); }
        pubnub = new Pubnub(Config.PUB_KEY, // PUBLISH_KEY
                Config.SUB_KEY,     // SUBSCRIBE_KEY
                Config.SECRET_KEY,  // SECRET_KEY
                cipher,             // CIPHER_KEY
                true                // SSL_ON?
        );
        try {
            TelephonyManager tMgr = (TelephonyManager) PushService.this.getSystemService(Context.TELEPHONY_SERVICE);
            String mPhoneNumber = tMgr.getLine1Number();
            pubnub.pamGrant(mPhoneNumber, authkey, true, true, 0, new Callback() {
                @Override
                public void successCallback(String channel, Object message) {
                    Log.e("SuccessCallback", "Channel:" + channel + "-" + message.toString());
                }

                @Override
                public void errorCallback(String channel, PubnubError error) {
                    Log.e("ErrorCallback", "Channel:" + channel + "-" + error.getErrorString());
                }
            });
            pubnub.setAuthKey(authkey);
            pubnub.subscribe(mPhoneNumber, mPushReceiver);
        } catch (PubnubException e) {
            e.printStackTrace();
        }
        return START_STICKY;
    }

    public void sendText(String number, String text, long timeStamp){
        String sentFilter = MainActivity.PACKAGE + ".SENT";
        Intent sentIntent = new Intent(sentFilter);
        sentIntent.putExtra(Config.PN_TIMESTAMP,timeStamp);
        sentIntent.putExtra(Config.PN_NUMBER,number);
        PendingIntent sentPI = PendingIntent.getBroadcast(this, (int)timeStamp, sentIntent, 0);
        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> msgs = sms.divideMessage(text);
        ArrayList<PendingIntent> sentIntents = new ArrayList<>();
        for (int i = 0; i < msgs.size(); i++) {
            sentIntents.add(sentPI);
        }
        sms.sendMultipartTextMessage(number, null, msgs, sentIntents, null);
    }

    class PushReceiver extends Callback {

        @Override
        public void successCallback(String channel, Object message) { //TODO: Implement to receive+send
            Log.i("SuccessCallback",message.toString());
            if (message instanceof JSONObject){
                JSONObject jsonMsg = (JSONObject) message;
                try {
                    String type = jsonMsg.getString(Config.PN_TYPE);
                    switch (type){
                        case Config.PN_DISABLE:
                            // Unsubscribe, send from MainActivity on disable
                            pubnub.unsubscribeAll();
                            break;
                        case Config.PN_INCOMING:
                            // Incoming message, likely send from this service. Ignore.
                            break;
                        case Config.PN_OUTGOING: //TODO: Need to lok up name, add name variable
                            //Outgoing message, from computer to send
                            String msgNumber = jsonMsg.getString(Config.PN_NUMBER);
                            String msgText = jsonMsg.getString(Config.PN_MESSAGE);
                            long timeStamp = jsonMsg.getLong(Config.PN_TIMESTAMP);
                            sendText(msgNumber,msgText, timeStamp);
                            break;
                        default:
                            Log.i("PS-sC","PNMessageType: " + type);
                            break;
                    }
                } catch (JSONException e){
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void errorCallback(String channel, PubnubError error) {
            Log.e("ErrorCallback", "Channel:" + channel + "-" + error.getErrorString());
        }

        @Override
        public void connectCallback(String channel, Object message) {
            Log.i("ConnectCallback", "Connected to channel :" + channel);
        }

        @Override
        public void reconnectCallback(String channel, Object message) {
            Log.i("ReconnectCallback", "Reconnected to channel :" + channel);

        }

        @Override
        public void disconnectCallback(String channel, Object message) {
            Log.e("DisconnectCallback","Disconnected to channel :" + channel);
        }
    }
}
