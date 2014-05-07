package org.btc4all.btc2sms.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.*;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicNameValuePair;
import org.btc4all.btc2sms.App;
import org.btc4all.btc2sms.R;
import org.btc4all.btc2sms.task.HttpTask;

import java.util.List;

public class LogView extends Activity {

    private App app;
    
    private BroadcastReceiver logReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {  
            updateLogView();
        }
    };
    
    private BroadcastReceiver settingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {  
            updateUpgradeButton();
            updateInfo();
        }
    };    
    
    private BroadcastReceiver expansionPacksReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateInfo();
        }
    };        

    private ScrollView scrollView;
    private TextView info;
    private TextView log;
    private TextView heading;
    private WebView loginWebView;
    private LinearLayout logLayout;

    private boolean firstTimeLoad;
    private boolean debugMode;
    
    private class TestTask extends HttpTask
    {
        public TestTask() {
            super(LogView.this.app, new BasicNameValuePair("action", App.ACTION_TEST));   
        }

        @Override
        protected void handleResponse(HttpResponse response) throws Exception 
        {
            app.log("Server connection OK!");            
        }
    }
    
    private int lastLogEpoch = -1;

    public void updateUpgradeButton()
    {
        Button upgradeButton = (Button) this.findViewById(R.id.upgrade_button);
        boolean isUpgradeAvailable = app.isUpgradeAvailable();                
        if (isUpgradeAvailable)
        {
            upgradeButton.setText("New version of app available ("+app.getMarketVersionName()+").\nClick to install...");
            upgradeButton.setVisibility(View.VISIBLE);
        }
        else
        {
            upgradeButton.setVisibility(View.GONE);
        }
                
    }
    
    public synchronized void updateLogView()
    {                   
        int logEpoch = app.getLogEpoch();
        CharSequence displayedLog = app.getDisplayedLog();        
        int logEpoch2 = app.getLogEpoch();
        
        if (lastLogEpoch == logEpoch && logEpoch == logEpoch2)
        {
            int beforeLen = log.getText().length();
            int afterLen = displayedLog.length();
            
            if (beforeLen == afterLen)
            {                
                return;
            }
            
            log.append(displayedLog, beforeLen, afterLen);
        }
        else
        {
            log.setText(displayedLog);
            lastLogEpoch = logEpoch;
        }
                
        scrollView.post(new Runnable() { public void run() { 
            scrollView.fullScroll(View.FOCUS_DOWN);
        } });
    }

    private void preLoad(final Bundle savedInstanceState) {

        setContentView(R.layout.splash);
        LayoutInflater li = getLayoutInflater();
        logLayout = (LinearLayout) li.inflate(R.layout.log_view, null);
        loginWebView = (WebView) logLayout.findViewById(R.id.login_web_view);
        WebSettings webSettings = loginWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        class AndroidJS {

            public void loadComplete() {
                Log.d("JS", "load complete");
                Thread viewThread = new Thread() {
                    public void run() {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                setContentView(logLayout);
                                loginWebView.setVisibility(1);
                                if (firstTimeLoad) {
                                    firstTimeLoad = false;
                                    continueLoading(savedInstanceState);
                                }
                            }
                        });
                    }
                };
                viewThread.start();
            }

            public void setConfig(String basePath, String cn, String mobile, String apiSecret, String servicePath, String password) {
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(LogView.this);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("server_url", String.format("%s/envayasms/%s/sms", basePath, cn));
                editor.putString("phone_number", mobile);
                editor.putString("password", apiSecret);
                editor.putString("outgoing_interval", "0");
                editor.putBoolean("keep_in_inbox", false);
                editor.putBoolean("call_notifications", false);
                editor.putString("send_limit", "400");
                editor.putBoolean("forward_sent", false);
                editor.putBoolean("test_mode", false);
                editor.putString("wifi_sleep_policy", "always stay connected");
                editor.putBoolean("network_failover", true);
                editor.putBoolean("amqp_enabled", true);
                editor.putString("amqp_host", servicePath);
                editor.putString("amqp_port", "5672");
                editor.putString("amqp_vhost", "/");
                editor.putBoolean("amqp_ssl", false);
                editor.putString("amqp_user", cn);
                editor.putString("amqp_password", password);
                editor.putString("amqp_queue", cn);
                editor.putString("amqp_heartbeat", "60");
                editor.putBoolean("enabled", true);
                editor.commit();
                Thread viewThread = new Thread() {
                    public void run() {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                updateInfo();
                            }
                        });
                    }
                };
                viewThread.start();
            }
        }
        loginWebView.addJavascriptInterface(new AndroidJS(), "Android");

        loginWebView.loadUrl("https://www.37coins.com/gateways?noHead=true");
        Log.d("TEST", "pre load finish");
    }

    private void continueLoading(Bundle savedInstanceState)
    {
        registerReceiver(logReceiver, new IntentFilter(App.LOG_CHANGED_INTENT));
        registerReceiver(settingsReceiver, new IntentFilter(App.SETTINGS_CHANGED_INTENT));
        registerReceiver(expansionPacksReceiver, new IntentFilter(App.EXPANSION_PACKS_CHANGED_INTENT));

        PreferenceManager.setDefaultValues(this, R.xml.prefs, false);

        heading = (TextView) this.findViewById(R.id.heading);
        info = (TextView) this.findViewById(R.id.info);

        updateInfo();

        scrollView = (ScrollView) this.findViewById(R.id.log_scroll);

        log = (TextView) this.findViewById(R.id.log);
        log.setMovementMethod(LinkMovementMethod.getInstance());

        updateUpgradeButton();
        updateLogView();


        if (savedInstanceState == null)
        {
            if (app.isUpgradeAvailable())
            {
                showUpgradeDialog();
            }
        }
        else
        {
            curDialog = savedInstanceState.getInt("cur_dialog", 0);
            if (curDialog == UPGRADE_DIALOG)
            {
                showUpgradeDialog();
            }
            else if (curDialog == SETTINGS_DIALOG)
            {
                showSettingsDialog();
            }
        }
    }
            
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        app = (App) getApplication();

        firstTimeLoad = true;

        preLoad(savedInstanceState);

    }

    public static final int NO_DIALOG = 0;
    public static final int UPGRADE_DIALOG = 1;
    public static final int CONFIGURE_SUCCESS_DIALOG = 2;
    public static final int SETTINGS_DIALOG = 3;
    
    private int curDialog = NO_DIALOG;
    
    public void updateInfo()
    {       
        boolean enabled = app.isEnabled();
        heading.setText(Html.fromHtml(
             enabled ? "<b>" + getText(R.string.running) + " ("+app.getPhoneNumber()+")</b>" 
                : "<b>" +getText(R.string.disabled) + "</b>"));       
        
        if (enabled)
        {
            info.setText("New messages will be forwarded to server");
            
            if (app.isTestMode())
            {
                info.append("\n(Test mode enabled)");
            }            
        }
        else
        {
            info.setText("New messages will not be forwarded to server");   
        }
       
    }
    
    public void infoClicked(View v)
    {
        startActivity(new Intent(this, Prefs.class));        
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        
        outState.putInt("cur_dialog", curDialog);
    }    
    
    public void showUpgradeDialog()
    {
        curDialog = UPGRADE_DIALOG;
        
        new AlertDialog.Builder(this)
            .setTitle("Upgrade available")
            .setMessage("A new version of the app is available ("+app.getMarketVersionName()+"). Do you want to upgrade now?")
            .setPositiveButton("OK", new OnClickListener() {
                public void onClick(DialogInterface dialog, int i)
                {
                    upgradeClicked(null);
                }
            })                
            .setNegativeButton("Not Now", new DismissDialogListener())
            .setOnCancelListener(new DismissDialogListener())
            .setCancelable(true)
            .show();
    }    
    
    public String getSettingsSummary()
    {
        StringBuilder builder = new StringBuilder();
        
        if (app.getKeepInInbox())
        {
            builder.append("- New messages kept in Messaging inbox\n");
        }
        else
        {
            builder.append("- New messages not kept in Messaging inbox\n");
        }
        
        if (app.callNotificationsEnabled())
        {
            builder.append("- Call notifications enabled\n");
        }
        else
        {
            builder.append("- Call notifications disabled\n");
        }
        
        List<String> ignoredNumbers = app.getIgnoredPhoneNumbers();
        boolean ignoreShortcodes = app.ignoreShortcodes();
        boolean ignoreNonNumeric = app.ignoreNonNumeric();
        boolean testMode = app.isTestMode();
        
        builder.append("- Send up to " + app.getOutgoingMessageLimit()+ " SMS/hour\n");        
        
        if (ignoredNumbers.isEmpty() && !ignoreShortcodes && !ignoreNonNumeric && !testMode)
        {
            builder.append("- Forward messages from all phone numbers");
        }
        else if (testMode)
        {
            builder.append("- Forward messages only from certain phone numbers");
        }
        else
        {
            builder.append("- Ignore messages from some phone numbers");
        }            
        
        return builder.toString();
    }
    
    public void showSettingsDialog()
    {
        curDialog = SETTINGS_DIALOG;
        
        new AlertDialog.Builder(this)
            .setTitle("Verify Settings")
            .setMessage(getSettingsSummary())
            .setPositiveButton("OK", new DismissDialogListener())
            .setNegativeButton("Change", new OnClickListener() {
                public void onClick(DialogInterface dialog, int i)
                {
                    curDialog = NO_DIALOG;
                    startActivity(new Intent(LogView.this, Prefs.class));
                }
            })                
            .setOnCancelListener(new DismissDialogListener())
            .setCancelable(true)
            .show();
    }
    
    public class DismissDialogListener implements OnClickListener, OnCancelListener
    {
        public void onCancel(DialogInterface dialog)
        {
            curDialog = NO_DIALOG;
            dialog.dismiss();
        }
        
        public void onClick(DialogInterface dialog, int i)
        {
            curDialog = NO_DIALOG;
            dialog.dismiss();
        }
    }
    
    public void upgradeClicked(View v)
    {        
        startActivity(new Intent(Intent.ACTION_VIEW, 
            Uri.parse("market://details?id=" + app.getPackageInfo().applicationInfo.packageName)));
    }
    
    @Override
    public void onDestroy()
    {
        unregisterReceiver(logReceiver);        
        unregisterReceiver(settingsReceiver);
        unregisterReceiver(expansionPacksReceiver);
        
        super.onDestroy();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.settings:
            startActivity(new Intent(this, Prefs.class));
            return true;
        case R.id.check_now:              
            app.checkOutgoingMessages();
            return true;
        case R.id.retry_now:                            
            app.retryStuckMessages();
            return true; 
        case R.id.forward_saved:
            startActivity(new Intent(this, MessagingSmsInbox.class));
            return true;
        case R.id.pending:
            startActivity(new Intent(this, PendingMessages.class));
            return true;
        case R.id.test:            
            app.log("Testing server connection...");
            new TestTask().execute();
            return true;
        case R.id.debug_on:
            debugMode = true;
            loginWebView.setVisibility(View.GONE);
            invalidateOptionsMenu();
            return true;
        case R.id.debug_off:
            debugMode = false;
            loginWebView.setVisibility(View.VISIBLE);
            invalidateOptionsMenu();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }        
    
    // first time the Menu key is pressed
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if (!debugMode) {
            inflater.inflate(R.menu.mainmenu, menu);
        } else {
            inflater.inflate(R.menu.mainmenu_debug, menu);
        }
        return(true);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (debugMode) {
            MenuItem retryItem = menu.findItem(R.id.retry_now);
            int pendingTasks = app.getPendingTaskCount();
            retryItem.setEnabled(pendingTasks > 0);
            retryItem.setTitle("Retry All (" + pendingTasks + ")");
        }
        return true;
    }
    
}