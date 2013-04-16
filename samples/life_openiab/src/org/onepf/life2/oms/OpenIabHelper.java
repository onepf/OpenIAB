package org.onepf.life2.oms;

import android.content.Context;
import org.onepf.life2.oms.appstore.googleUtils.IabHelper;

/**
 * User: Boris Minaev
 * Date: 16.04.13
 * Time: 16:42
 */
public class OpenIabHelper {
    Context mContext;
    AppstoreServiceManager mServiceManager;
    Appstore mAppstore;
    AppstoreInAppBillingService mAppstoreBillingService;

    public OpenIabHelper(Context context, String googlePublicKey) {
        mContext = context;
        mServiceManager = AppstoreServiceManager.getInstance(context, googlePublicKey);
        mAppstore = mServiceManager.getAppstoreForService(AppstoreService.APPSTORE_SERVICE_IN_APP_BILLING);
        mAppstoreBillingService = mAppstore.getInAppBillingService();
    }

    public void startSetup(final IabHelper.OnIabSetupFinishedListener listener) {
        mAppstoreBillingService.startSetup(listener);
    }


}
