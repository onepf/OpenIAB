package org.onepf.life2.yandex;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import org.onepf.life2.GameActivity;
import org.onepf.life2.Market;


import org.onepf.life2.google.GooglePlayHelper;
import org.onepf.life2.oms.appstore.IabHelperBillingService;
import org.onepf.life2.oms.appstore.YandexIabHelperBillingService;

/**
 * Author: Yury Vasileuski
 * Date: 18.05.13
 */

public class YandexStoreHelper extends GooglePlayHelper {
    public static final String YANDEX_STORE_SERVICE = "com.yandex.store.service";
    public static final String YANDEX_STORE_ACTION_PURCHASE_STATE_CHANGED = YANDEX_STORE_SERVICE + ".PURCHASE_STATE_CHANGED";

    public YandexStoreHelper(Context context) {
        super(context);
        IntentFilter filter = new IntentFilter(YANDEX_STORE_ACTION_PURCHASE_STATE_CHANGED);
        context.registerReceiver(mBillingReceiver, filter);
    }

    @Override
    public IabHelperBillingService billingService(Context context) {
        return new YandexIabHelperBillingService(context);
    }

    @Override
    public Market getMarket() {
        return Market.YANDEX_STORE;
    }

    @Override
    public void onDestroy() {
        try {
            mContext.unregisterReceiver(mBillingReceiver);
        } catch (Exception ex) {
        }
        super.onDestroy();
    }

    private BroadcastReceiver mBillingReceiver = new BroadcastReceiver() {
        private static final String TAG = "YandexBillingReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "receive action: " + action);

            if (YANDEX_STORE_ACTION_PURCHASE_STATE_CHANGED.equals(action)) {
                purchaseStateChanged(intent);
            }
        }

        private void purchaseStateChanged(Intent data) {
            mOpenIabHelper.handleActivityResult(RC_REQUEST, Activity.RESULT_OK, data);
        }
    };
}
