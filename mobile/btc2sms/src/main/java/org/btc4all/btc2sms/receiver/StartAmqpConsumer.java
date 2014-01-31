
package org.btc4all.btc2sms.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.btc4all.btc2sms.App;

public class StartAmqpConsumer extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final App app = (App) context.getApplicationContext();
        
        if (!app.isEnabled())
        {
            return;
        }
     
        app.getAmqpConsumer().startAsync();
    }
}
