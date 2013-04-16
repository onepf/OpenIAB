package org.onepf.life2.oms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.onepf.life2.oms.appstore.GooglePlayBillingService;
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

    // Item types
    public static final String ITEM_TYPE_INAPP = "inapp";
    public static final String ITEM_TYPE_SUBS = "subs";

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
        if (mAppstore.getAppstoreName() == AppstoreName.APPSTORE_GOOGLE) {
            ((GooglePlayBillingService) mAppstoreBillingService).dispose();
        }
    }

    public boolean subscriptionsSupported() {
        return true;
    }

    public void launchPurchaseFlow(Activity act, String sku, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener) {
        launchPurchaseFlow(act, sku, requestCode, listener, "");
    }

    public void launchPurchaseFlow(Activity act, String sku, int requestCode,
                                   IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        launchPurchaseFlow(act, sku, ITEM_TYPE_INAPP, requestCode, listener, extraData);
    }

    public void launchSubscriptionPurchaseFlow(Activity act, String sku, int requestCode,
                                               IabHelper.OnIabPurchaseFinishedListener listener) {
        launchSubscriptionPurchaseFlow(act, sku, requestCode, listener, "");
    }

    public void launchSubscriptionPurchaseFlow(Activity act, String sku, int requestCode,
                                               IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        launchPurchaseFlow(act, sku, ITEM_TYPE_SUBS, requestCode, listener, extraData);
    }

    public void launchPurchaseFlow(Activity act, String sku, String itemType, int requestCode,
                                   IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        mAppstoreBillingService.launchPurchaseFlow(act, sku, itemType, requestCode, listener, extraData);
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return mAppstoreBillingService.handleActivityResult(requestCode, resultCode, data);
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
