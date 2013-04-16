package org.onepf.life2.oms;

import android.content.Context;
import android.util.Log;
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

    // Is debug logging enabled?
    boolean mDebugLog = false;
    String mDebugTag = "IabHelper";

    // Is an asynchronous operation in progress?
    // (only one at a time can be in progress)
    boolean mAsyncInProgress = false;

    // (for logging/debugging)
    // if mAsyncInProgress == true, what asynchronous operation is in progress?
    String mAsyncOperation = "";

    // The request code used to launch purchase flow
    int mRequestCode;

    // The item type of the current purchase flow
    String mPurchasingItemType;

    public OpenIabHelper(Context context, String googlePublicKey) {
        mContext = context;
        mServiceManager = AppstoreServiceManager.getInstance(context, googlePublicKey);
        mAppstore = mServiceManager.getAppstoreForService(AppstoreService.APPSTORE_SERVICE_IN_APP_BILLING);
        mAppstoreBillingService = mAppstore.getInAppBillingService();
    }

    public void startSetup(final IabHelper.OnIabSetupFinishedListener listener) {
        mAppstoreBillingService.startSetup(listener);
    }

    public void dispose() {
        logDebug("Disposing.");
        if (mAppstoreBillingService.get)
            mSetupDone = false;
        if (mServiceConn != null) {
            logDebug("Unbinding from service.");
            if (mContext != null) mContext.unbindService(mServiceConn);
            mServiceConn = null;
            mService = null;
            mPurchaseListener = null;
        }
    }

    void logDebug(String msg) {
        if (mDebugLog) Log.d(mDebugTag, msg);
    }

    void logError(String msg) {
        Log.e(mDebugTag, "In-app billing error: " + msg);
    }

    void logWarn(String msg) {
        Log.w(mDebugTag, "In-app billing warning: " + msg);
    }

}
