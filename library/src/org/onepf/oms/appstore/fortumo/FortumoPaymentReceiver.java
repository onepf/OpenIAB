package org.onepf.oms.appstore.fortumo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Created by akarimova on 23.12.13.
 */
public class FortumoPaymentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(FortumoStore.SHARED_PREFS_FORTUMO, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(FortumoStore.SHARED_PREFS_PAYMENT_TO_HANDLE, intent.getStringExtra("message_id"));
        editor.commit();
    }

}
