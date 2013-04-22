package org.onepf.life2.oms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import org.onepf.life2.oms.appstore.GooglePlayBillingService;
import org.onepf.life2.oms.appstore.googleUtils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // Is setup done?
    boolean mSetupDone = false;

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

    // Billing response codes
    public static final int BILLING_RESPONSE_RESULT_OK = 0;
    public static final int BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3;
    public static final int BILLING_RESPONSE_RESULT_ERROR = 6;

    public OpenIabHelper(Context context, String googlePublicKey, String samsungGroupId) {
        mContext = context;
        mServiceManager = AppstoreServiceManager.getInstance(context, googlePublicKey, samsungGroupId);
        mAppstore = mServiceManager.getAppstoreForService(AppstoreService.APPSTORE_SERVICE_IN_APP_BILLING);
        if (mAppstore == null) {
            return;
        }
        mAppstoreBillingService = mAppstore.getInAppBillingService();
        mSetupDone = true;
    }

    public void startSetup(final IabHelper.OnIabSetupFinishedListener listener) {
        if (!mSetupDone) {
            IabResult iabResult = new IabResult(BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE, "Billing isn't supported");
            listener.onIabSetupFinished(iabResult);
            return;
        }
        mAppstoreBillingService.startSetup(listener);
    }

    public void dispose() {
        logDebug("Disposing.");
        checkSetupDone("dispose");
        if (mAppstore.getAppstoreName() == AppstoreName.GOOGLE) {
            ((GooglePlayBillingService) mAppstoreBillingService).dispose();
        }
    }

    public boolean subscriptionsSupported() {
        return true;
    }

    public void launchPurchaseFlow(Activity act, OpenSku sku, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener) {
        launchPurchaseFlow(act, sku, requestCode, listener, "");
    }

    public void launchPurchaseFlow(Activity act, OpenSku sku, int requestCode,
                                   IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        launchPurchaseFlow(act, sku, ITEM_TYPE_INAPP, requestCode, listener, extraData);
    }

    public void launchSubscriptionPurchaseFlow(Activity act, OpenSku sku, int requestCode,
                                               IabHelper.OnIabPurchaseFinishedListener listener) {
        launchSubscriptionPurchaseFlow(act, sku, requestCode, listener, "");
    }

    public void launchSubscriptionPurchaseFlow(Activity act, OpenSku sku, int requestCode,
                                               IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        launchPurchaseFlow(act, sku, ITEM_TYPE_SUBS, requestCode, listener, extraData);
    }

    public void launchPurchaseFlow(Activity act, OpenSku sku, String itemType, int requestCode,
                                   IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        checkSetupDone("launchPurchaseFlow");
        String skuCurrentStore = sku.getSku(mAppstore.getAppstoreName());
        if (skuCurrentStore == null) {
            // TODO: throw an exception
            return;
        }
        mAppstoreBillingService.launchPurchaseFlow(act, skuCurrentStore, itemType, requestCode, listener, extraData);
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return mAppstoreBillingService.handleActivityResult(requestCode, resultCode, data);
    }

    public Inventory queryInventory(boolean querySkuDetails, List<OpenSku> moreSkus) throws IabException {
        return queryInventory(querySkuDetails, moreSkus, null);
    }

    public Inventory queryInventory(boolean querySkuDetails, List<OpenSku> moreItemSkus,
                                    List<OpenSku> moreSubsSkus) throws IabException {
        checkSetupDone("queryInvenoty");
        Map<String, String> skuToOpen = new HashMap<>();
        List<String> moreItemSkusCurrentStore = new ArrayList();
        if (moreItemSkus == null) {
            moreItemSkusCurrentStore = null;
        } else {
            for (OpenSku sku : moreItemSkus) {
                String skuCurrentStore = sku.getSku(mAppstore.getAppstoreName());
                if (skuCurrentStore == null) {
                    // TODO: throw an exception
                    return null;
                }
                moreItemSkusCurrentStore.add(skuCurrentStore);
            }
        }
        List<String> moreSubsSkusCurrentStore = new ArrayList();
        if (moreSubsSkus == null) {
            moreSubsSkusCurrentStore = null;
        } else {
            for (OpenSku sku : moreSubsSkus) {
                String skuCurrentStore = sku.getSku(mAppstore.getAppstoreName());
                if (skuCurrentStore == null) {
                    // TODO: throw an exception
                    return null;
                }
                moreSubsSkusCurrentStore.add(skuCurrentStore);
            }
        }
        Inventory res = mAppstoreBillingService.queryInventory(querySkuDetails, moreItemSkusCurrentStore,
                moreSubsSkusCurrentStore);
        return res;
    }

    public void queryInventoryAsync(final boolean querySkuDetails,
                                    final List<OpenSku> moreSkus,
                                    final IabHelper.QueryInventoryFinishedListener listener) {
        final Handler handler = new Handler();
        checkSetupDone("queryInventory");
        flagStartAsync("refresh inventory");
        (new Thread(new Runnable() {
            public void run() {
                IabResult result = new IabResult(BILLING_RESPONSE_RESULT_OK, "Inventory refresh successful.");
                Inventory inv = null;
                try {
                    inv = queryInventory(querySkuDetails, moreSkus);
                } catch (IabException ex) {
                    result = ex.getResult();
                }

                flagEndAsync();

                final IabResult result_f = result;
                final Inventory inv_f = inv;
                handler.post(new Runnable() {
                    public void run() {
                        listener.onQueryInventoryFinished(result_f, inv_f);
                    }
                });
            }
        })).start();
    }

    public void queryInventoryAsync(IabHelper.QueryInventoryFinishedListener listener) {
        queryInventoryAsync(true, null, listener);
    }

    public void queryInventoryAsync(boolean querySkuDetails, IabHelper.QueryInventoryFinishedListener listener) {
        queryInventoryAsync(querySkuDetails, null, listener);
    }

    public void consume(Purchase itemInfo) throws IabException {
        // TODO: need to check store
        checkSetupDone("consume");
        mAppstoreBillingService.consume(itemInfo);
    }

    public void consumeAsync(Purchase purchase, IabHelper.OnConsumeFinishedListener listener) {
        List<Purchase> purchases = new ArrayList<Purchase>();
        purchases.add(purchase);
        consumeAsyncInternal(purchases, listener, null);
    }

    public void consumeAsync(List<Purchase> purchases, IabHelper.OnConsumeMultiFinishedListener listener) {
        consumeAsyncInternal(purchases, null, listener);
    }

    void consumeAsyncInternal(final List<Purchase> purchases,
                              final IabHelper.OnConsumeFinishedListener singleListener,
                              final IabHelper.OnConsumeMultiFinishedListener multiListener) {
        final Handler handler = new Handler();
        flagStartAsync("consume");
        (new Thread(new Runnable() {
            public void run() {
                final List<IabResult> results = new ArrayList<IabResult>();
                for (Purchase purchase : purchases) {
                    try {
                        consume(purchase);
                        results.add(new IabResult(BILLING_RESPONSE_RESULT_OK, "Successful consume of sku " + purchase.getSku()));
                    } catch (IabException ex) {
                        results.add(ex.getResult());
                    }
                }

                flagEndAsync();
                if (singleListener != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            singleListener.onConsumeFinished(purchases.get(0), results.get(0));
                        }
                    });
                }
                if (multiListener != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            multiListener.onConsumeMultiFinished(purchases, results);
                        }
                    });
                }
            }
        })).start();
    }

    // Checks that setup was done; if not, throws an exception.
    void checkSetupDone(String operation) {
        if (!mSetupDone) {
            logError("Illegal state for operation (" + operation + "): IAB helper is not set up.");
            throw new IllegalStateException("IAB helper is not set up. Can't perform operation: " + operation);
        }
    }

    void flagStartAsync(String operation) {
        if (mAsyncInProgress) throw new IllegalStateException("Can't start async operation (" +
                operation + ") because another async operation(" + mAsyncOperation + ") is in progress.");
        mAsyncOperation = operation;
        mAsyncInProgress = true;
        logDebug("Starting async operation: " + operation);
    }

    void flagEndAsync() {
        logDebug("Ending async operation: " + mAsyncOperation);
        mAsyncOperation = "";
        mAsyncInProgress = false;
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
