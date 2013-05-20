/*******************************************************************************
 * Copyright 2013 One Platform Foundation
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 ******************************************************************************/

package org.onepf.oms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import org.onepf.oms.appstore.IabHelperBillingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: Boris Minaev
 * Date: 16.04.13
 * Time: 16:42
 */

public class OpenIabHelper {
    private static final String TAG = "IabHelper";
    private final Context mContext;
    private final AppstoreServiceManager mServiceManager;
    private Appstore mAppstore;
    private AppstoreInAppBillingService mAppstoreBillingService;

    // Is debug logging enabled?
    private static final boolean mDebugLog = false;

    // Is setup done?
    private boolean mSetupDone = false;

    // Is an asynchronous operation in progress?
    // (only one at a time can be in progress)
    private boolean mAsyncInProgress = false;

    // (for logging/debugging)
    // if mAsyncInProgress == true, what asynchronous operation is in progress?
    private String mAsyncOperation = "";

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

    public interface OnOpenIabHelperInitFinished {
        public void onOpenIabHelperInitFinished();
    }

    public OpenIabHelper(Context context, Map<String, String> extra) {
        mContext = context;
        mServiceManager = new AppstoreServiceManager(context, extra);
    }

    public void startSetup(final org.onepf.oms.appstore.googleUtils.IabHelper.OnIabSetupFinishedListener listener, final IabHelperBillingService billingService) {
        mServiceManager.startSetup(new AppstoreServiceManager.OnAppstoreServiceManagerInitFinishedListener() {
            @Override
            public void onAppstoreServiceManagerInitFinishedListener() {
                mAppstore = mServiceManager.getAppstoreForService(AppstoreService.IN_APP_BILLING);
                if (mAppstore == null) {
                    return;
                }
                mAppstoreBillingService = mAppstore.getInAppBillingService();
                Log.d(TAG, "OpenIabHelper use appstore: " + mAppstore.getAppstoreName().name());
                mSetupDone = true;
                // TODO: this is always false!
                if (!mSetupDone) {
                    org.onepf.oms.appstore.googleUtils.IabResult iabResult = new org.onepf.oms.appstore.googleUtils.IabResult(BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE, "Billing isn't supported");
                    listener.onIabSetupFinished(iabResult);
                    return;
                }
                mAppstoreBillingService.startSetup(listener, billingService);
            }
        });
    }

    public void dispose() {
        logDebug("Disposing.");
        checkSetupDone("dispose");
        mAppstoreBillingService.dispose();
    }

    public boolean subscriptionsSupported() {
        // TODO: implement this
        return true;
    }

    public void launchPurchaseFlow(Activity act, org.onepf.oms.OpenSku sku, int requestCode, org.onepf.oms.appstore.googleUtils.IabHelper.OnIabPurchaseFinishedListener listener) {
        launchPurchaseFlow(act, sku, requestCode, listener, "");
    }

    public void launchPurchaseFlow(Activity act, org.onepf.oms.OpenSku sku, int requestCode,
                                   org.onepf.oms.appstore.googleUtils.IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        launchPurchaseFlow(act, sku, ITEM_TYPE_INAPP, requestCode, listener, extraData);
    }

    public void launchSubscriptionPurchaseFlow(Activity act, org.onepf.oms.OpenSku sku, int requestCode,
                                               org.onepf.oms.appstore.googleUtils.IabHelper.OnIabPurchaseFinishedListener listener) {
        launchSubscriptionPurchaseFlow(act, sku, requestCode, listener, "");
    }

    public void launchSubscriptionPurchaseFlow(Activity act, org.onepf.oms.OpenSku sku, int requestCode,
                                               org.onepf.oms.appstore.googleUtils.IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        launchPurchaseFlow(act, sku, ITEM_TYPE_SUBS, requestCode, listener, extraData);
    }

    public void launchPurchaseFlow(Activity act, org.onepf.oms.OpenSku sku, String itemType, int requestCode,
                                   org.onepf.oms.appstore.googleUtils.IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        checkSetupDone("launchPurchaseFlow");
        String skuCurrentStore = sku.getSku(mAppstore.getAppstoreName());
        if (skuCurrentStore == null) {
            // TODO: throw an exception
            Log.e(TAG, "No SKU for current appstore");
            return;
        }
        mAppstoreBillingService.launchPurchaseFlow(act, skuCurrentStore, itemType, requestCode, listener, extraData);
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return mAppstoreBillingService.handleActivityResult(requestCode, resultCode, data);
    }

    public org.onepf.oms.appstore.googleUtils.Inventory queryInventory(boolean querySkuDetails, List<org.onepf.oms.OpenSku> moreSkus) throws org.onepf.oms.appstore.googleUtils.IabException {
        return queryInventory(querySkuDetails, moreSkus, null);
    }

    public org.onepf.oms.appstore.googleUtils.Inventory queryInventory(boolean querySkuDetails, List<org.onepf.oms.OpenSku> moreItemSkus,
                                    List<org.onepf.oms.OpenSku> moreSubsSkus) throws org.onepf.oms.appstore.googleUtils.IabException {
        checkSetupDone("queryInventory");
        //Map<String, String> skuToOpen = new HashMap<>();
        List<String> moreItemSkusCurrentStore = new ArrayList<String>();
        if (moreItemSkus == null) {
            moreItemSkusCurrentStore = null;
        } else {
            for (org.onepf.oms.OpenSku sku : moreItemSkus) {
                String skuCurrentStore = sku.getSku(mAppstore.getAppstoreName());
                if (skuCurrentStore == null) {
                    // TODO: throw an exception
                    Log.e(TAG, "No matching SKU found for current appstore");
                    return null;
                }
                moreItemSkusCurrentStore.add(skuCurrentStore);
            }
        }
        List<String> moreSubsSkusCurrentStore = new ArrayList<String>();
        if (moreSubsSkus == null) {
            moreSubsSkusCurrentStore = null;
        } else {
            for (org.onepf.oms.OpenSku sku : moreSubsSkus) {
                String skuCurrentStore = sku.getSku(mAppstore.getAppstoreName());
                if (skuCurrentStore == null) {
                    // TODO: throw an exception
                    Log.e(TAG, "No matching SKU found for current appstore");
                    return null;
                }
                moreSubsSkusCurrentStore.add(skuCurrentStore);
            }
        }
        return mAppstoreBillingService.queryInventory(querySkuDetails, moreItemSkusCurrentStore,
                moreSubsSkusCurrentStore);
    }

    public void queryInventoryAsync(final boolean querySkuDetails,
                                    final List<org.onepf.oms.OpenSku> moreSkus,
                                    final org.onepf.oms.appstore.googleUtils.IabHelper.QueryInventoryFinishedListener listener) {
        final Handler handler = new Handler();
        checkSetupDone("queryInventory");
        flagStartAsync("refresh inventory");
        (new Thread(new Runnable() {
            public void run() {
                org.onepf.oms.appstore.googleUtils.IabResult result = new org.onepf.oms.appstore.googleUtils.IabResult(BILLING_RESPONSE_RESULT_OK, "Inventory refresh successful.");
                org.onepf.oms.appstore.googleUtils.Inventory inv = null;
                try {
                    inv = queryInventory(querySkuDetails, moreSkus);
                } catch (org.onepf.oms.appstore.googleUtils.IabException ex) {
                    result = ex.getResult();
                }

                flagEndAsync();

                final org.onepf.oms.appstore.googleUtils.IabResult result_f = result;
                final org.onepf.oms.appstore.googleUtils.Inventory inv_f = inv;
                handler.post(new Runnable() {
                    public void run() {
                        listener.onQueryInventoryFinished(result_f, inv_f);
                    }
                });
            }
        })).start();
    }

    public void queryInventoryAsync(org.onepf.oms.appstore.googleUtils.IabHelper.QueryInventoryFinishedListener listener) {
        queryInventoryAsync(true, null, listener);
    }

    public void queryInventoryAsync(boolean querySkuDetails, org.onepf.oms.appstore.googleUtils.IabHelper.QueryInventoryFinishedListener listener) {
        queryInventoryAsync(querySkuDetails, null, listener);
    }

    public void consume(org.onepf.oms.appstore.googleUtils.Purchase itemInfo) throws org.onepf.oms.appstore.googleUtils.IabException {
        // TODO: need to check store
        checkSetupDone("consume");
        mAppstoreBillingService.consume(itemInfo);
    }

    public void consumeAsync(org.onepf.oms.appstore.googleUtils.Purchase purchase, org.onepf.oms.appstore.googleUtils.IabHelper.OnConsumeFinishedListener listener) {
        List<org.onepf.oms.appstore.googleUtils.Purchase> purchases = new ArrayList<org.onepf.oms.appstore.googleUtils.Purchase>();
        purchases.add(purchase);
        consumeAsyncInternal(purchases, listener, null);
    }

    public void consumeAsync(List<org.onepf.oms.appstore.googleUtils.Purchase> purchases, org.onepf.oms.appstore.googleUtils.IabHelper.OnConsumeMultiFinishedListener listener) {
        consumeAsyncInternal(purchases, null, listener);
    }

    void consumeAsyncInternal(final List<org.onepf.oms.appstore.googleUtils.Purchase> purchases,
                              final org.onepf.oms.appstore.googleUtils.IabHelper.OnConsumeFinishedListener singleListener,
                              final org.onepf.oms.appstore.googleUtils.IabHelper.OnConsumeMultiFinishedListener multiListener) {
        final Handler handler = new Handler();
        flagStartAsync("consume");
        (new Thread(new Runnable() {
            public void run() {
                final List<org.onepf.oms.appstore.googleUtils.IabResult> results = new ArrayList<org.onepf.oms.appstore.googleUtils.IabResult>();
                for (org.onepf.oms.appstore.googleUtils.Purchase purchase : purchases) {
                    try {
                        consume(purchase);
                        results.add(new org.onepf.oms.appstore.googleUtils.IabResult(BILLING_RESPONSE_RESULT_OK, "Successful consume of sku " + purchase.getSku()));
                    } catch (org.onepf.oms.appstore.googleUtils.IabException ex) {
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
        // TODO: why can't be called consume and queryInventory at the same time?
//        if (mAsyncInProgress) {
//            throw new IllegalStateException("Can't start async operation (" +
//                    operation + ") because another async operation(" + mAsyncOperation + ") is in progress.");
//        }
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
        if (mDebugLog) Log.d(TAG, msg);
    }

    void logError(String msg) {
        Log.e(TAG, "In-app billing error: " + msg);
    }

    void logWarn(String msg) {
        Log.w(TAG, "In-app billing warning: " + msg);
    }

}
