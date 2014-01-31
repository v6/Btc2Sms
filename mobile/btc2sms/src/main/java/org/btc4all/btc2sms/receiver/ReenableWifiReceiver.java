package org.btc4all.btc2sms.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

import org.btc4all.btc2sms.App;

public class ReenableWifiReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        App app = (App) context.getApplicationContext();
        
        if (!app.isEnabled())
        {
            return;
        }        
        
        WifiManager wmgr = 
                (WifiManager)app.getSystemService(Context.WIFI_SERVICE);
        
        if (!wmgr.isWifiEnabled())
        {
            app.log("Reenabling Wi-Fi");
            wmgr.setWifiEnabled(true);
        }
    }
}
