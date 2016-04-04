package me.kevingleason.amessage;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String SP_ENABLED = "aMessage.ENABLED";
    public static final String SP_CIPHER  = "aMessage.CIPHER";
    public static final String SP_AUTHKEY = "aMessage.AUTHKEY";
    public static final String PACKAGE = "me.kevingleason.amessage";

    private ViewSwitcher mCipherSwitcher, mAuthSwitcher;
    private EditText mCipherET, mAuthET;
    private TextView mCipherTV, mAuthTV, mPhoneNumberTV;
    private LinearLayout mEnabledSwitch;
    private TextView mEnabledTV;
    private FloatingActionButton mFAB;

    private SharedPreferences mSharedPrefs;
    private Pubnub mPubnub;

    private String mPhoneNumber;
    private String cipher;
    private String authkey;
    private boolean enabled;
    private boolean editing;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.mPhoneNumberTV = (TextView) findViewById(R.id.phoneNumberTV);

        this.mCipherSwitcher = (ViewSwitcher) findViewById(R.id.cipher_switcher);
        this.mCipherTV = (TextView) findViewById(R.id.tv_cipher);
        this.mCipherET = (EditText) findViewById(R.id.edit_cipher);

        this.mAuthSwitcher = (ViewSwitcher) findViewById(R.id.authkey_switcher);
        this.mAuthTV = (TextView) findViewById(R.id.tv_authkey);
        this.mAuthET = (EditText) findViewById(R.id.edit_authkey);

        this.mEnabledSwitch = (LinearLayout) findViewById(R.id.enabled_switch);
        this.mEnabledTV = (TextView) findViewById(R.id.tv_enabled);
        this.mFAB = (FloatingActionButton) findViewById(R.id.fab);

        this.mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.enabled = this.mSharedPrefs.getBoolean(SP_ENABLED, false);
        this.cipher  = this.mSharedPrefs.getString(SP_CIPHER, "");
        this.authkey = this.mSharedPrefs.getString(SP_AUTHKEY, "");

        TelephonyManager tMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        this.mPhoneNumber = tMgr.getLine1Number();
        this.mPhoneNumberTV.setText(this.mPhoneNumber);

        updateTextViews();
        setupButtons();
        setupPubNub();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (enabled)
            startPushService(); //only really matters the first time you launch the app after you install.. otherwise, it will start on boot
    }

    private void updateTextViews(){
        String enabledText = getString(enabled ? R.string.enabled : R.string.disabled);
        int enabledColor = ContextCompat.getColor(this,
                enabled ? R.color.green_enabled : R.color.red_disabled);
        this.mEnabledTV.setText(enabledText);
        this.mEnabledSwitch.setBackgroundColor(enabledColor);

        String authText   = authkey.isEmpty() ? getString(R.string.authkey_hint) : censorKey(authkey);
        String cipherText = cipher.isEmpty()  ? getString(R.string.cipher_hint)  : censorKey(cipher);
        this.mAuthTV.setText(authText);
        this.mCipherTV.setText(cipherText);
    }

    private void setupButtons(){
        this.mEnabledSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enabled = !enabled;
                SharedPreferences.Editor edit = mSharedPrefs.edit();
                edit.putBoolean(SP_ENABLED, enabled);
                edit.commit();
                updateTextViews();

                if (!enabled) {
                    killService();
                    try {
                        JSONObject disableJson = new JSONObject(); //Send to deactivate pubnub in service
                        disableJson.put(Config.PN_TYPE, Config.PN_DISABLE);
                        mPubnub.publish(mPhoneNumber, disableJson, new Callback() {
                            @Override
                            public void successCallback(String channel, Object message) {
                                Log.e("MA-sB", "Sent: " + message.toString());
                            }

                            @Override
                            public void errorCallback(String channel, PubnubError error) {
                                Log.e("MA-sB", "PN-Error: " + error.getErrorString());
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    startPushService();
                }
            }
        });
    }

    public void clickFAB(View view){
        editing = !editing;
        int fabColor = ContextCompat.getColor(MainActivity.this,
                editing ? R.color.green_enabled : R.color.red_disabled);
        Drawable fabIcon = ContextCompat.getDrawable(MainActivity.this,
                editing ? android.R.drawable.ic_menu_save : android.R.drawable.ic_menu_edit);
        mFAB.setBackgroundTintList(ColorStateList.valueOf(fabColor));
        mFAB.setImageDrawable(fabIcon);

        if (editing) {
            mCipherET.setText(cipher);
            mAuthET.setText(authkey);
            mCipherSwitcher.showNext();
            mAuthSwitcher.showNext();
            mCipherET.requestFocus();

        } else { //TODO: Kill and reset
            killService();
            cipher = mCipherET.getText().toString();
            authkey = mAuthET.getText().toString();

            updateTextViews();

            SharedPreferences.Editor edit = mSharedPrefs.edit();
            edit.putString(SP_CIPHER, cipher);
            edit.putString(SP_AUTHKEY, authkey);
            edit.apply();

            mCipherSwitcher.showPrevious();
            mAuthSwitcher.showPrevious();

            setupPubNub();
            startPushService();
        }
    }

    private void setupPubNub(){
        this.mPubnub = new Pubnub(
                Config.PUB_KEY,
                Config.SUB_KEY,
                Config.SECRET_KEY,
                cipher,
                true
        );
        this.mPubnub.setAuthKey(authkey);
    }

    private String censorKey(String key){
        if (key.length() < 3) return key;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < key.length()-2; i++) {
            sb.append("*");
        }
        return key.charAt(0) + sb.toString() + key.charAt(key.length()-1);
    }

    private void startPushService(){
        Log.d("MA-sPS","Starting Push Service");
        AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, PushAlarm.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
              (5 * 60 * 1000), pendingIntent); //wake up every 5 minutes to ensure service stays alive
    }

    private void killService() {
        ActivityManager manager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);

        List<ActivityManager.RunningAppProcessInfo> services = manager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo service : services) {
            if (service.processName.equals(PACKAGE+":pushservice")) {
                Toast.makeText(this,"Killed " + service.processName,Toast.LENGTH_SHORT).show();
                int pid = service.pid;
                android.os.Process.killProcess(pid);
            }
        }
    }
}
