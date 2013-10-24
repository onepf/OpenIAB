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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onepf.oms.appstore.AmazonAppstore;
import org.onepf.oms.appstore.GooglePlay;
import org.onepf.oms.appstore.SamsungApps;
import org.onepf.oms.appstore.TStore;
import org.onepf.oms.appstore.googleUtils.IabException;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabHelper.OnIabSetupFinishedListener;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.appstore.googleUtils.Purchase;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

/**
 * User: Boris Minaev
 * Date: 16.04.13
 * Time: 16:42
 */

public class OpenIabHelper {
    private static final String TAG = "OpenIabHelper";
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
        
    public static final String NAME_GOOGLE = "com.google.play";
    public static final String NAME_AMAZON = "com.amazon.apps";
    public static final String NAME_TSTORE = "com.tmobile.store";
    public static final String NAME_SAMSUNG = "com.samsung.apps";

    /** 
     * NOTE: used as sync object in related methods<br>
     * 
     * storeName -> [ ... {app_sku1 -> store_sku1}, ... ]
     */
    private static final Map <String, Map<String, String>> sku2storeSkuMappings = new HashMap<String, Map <String, String>>();

    /** 
     * storeName -> [ ... {store_sku1 -> app_sku1}, ... ]
     */
    private static final Map <String, Map<String, String>> storeSku2skuMappings = new HashMap<String, Map <String, String>>();
        
    /**
     * Map sku and storeSku for particular store.
     * 
     * TODO: returns smth with nice API like:
     * <pre>
     * mapSku(sku).store(GOOGLE_PLAY).to(SKU_MY1_GOOGLE).and(AMAZON_APPS).to(SKU_MY1_AMAZON)
     * or
     * mapStore(AMAZON_APPS).sku(SKU_MY2).to(SKU_MY2_AMAZON).sku(SKU_MY3).to(SKU_MY3_AMAZON)
     * </pre>
     * 
     * @param sku - and
     * @param storeSku - shouldn't duplicate already mapped values
     * @param storeName - @see {@link IOpenAppstore#getAppstoreName()} or {@link #NAME_AMAZON} {@link #NAME_GOOGLE} {@link #NAME_TSTORE}
     */
    public static void mapSku(String sku, String storeName, String storeSku) {
        synchronized (sku2storeSkuMappings) {
            Map<String, String> skuMap = sku2storeSkuMappings.get(storeName);
            if (skuMap == null) {
                skuMap = new HashMap<String, String>();
                sku2storeSkuMappings.put(storeName, skuMap);
            }
            if (skuMap.get(sku) != null) {
                throw new IllegalArgumentException("Already specified sku: " + sku + ", storeSku: " + skuMap.get(sku));
            }
            Map<String, String> storeSkuMap = storeSku2skuMappings.get(storeName);
            if (storeSkuMap == null) {
                storeSkuMap = new HashMap<String, String>();
                storeSku2skuMappings.put(storeName, storeSkuMap);
            }
            if (storeSkuMap.get(storeSku) != null) {
                throw new IllegalArgumentException("Already specified storeSku: " + storeSku + ", sku: " + storeSkuMap.get(storeSku));
            }
            skuMap.put(sku, storeSku);
            storeSkuMap.put(storeSku, sku);
        }
    }
    
    public static String getStoreSku(final String appstoreName, String sku) {
        synchronized (sku2storeSkuMappings) {
            String currentStoreSku = sku;
            Map<String, String> skuMap = sku2storeSkuMappings.get(appstoreName);
            if (skuMap != null && skuMap.get(sku) != null) {
                currentStoreSku = skuMap.get(sku);
                Log.d(TAG, "getStoreSku() using mapping for sku: " + sku + " -> " + currentStoreSku);
            }
            return currentStoreSku;
        }
    }
    
    public static String getSku(final String appstoreName, String storeSku) {
        synchronized (sku2storeSkuMappings) {
            String sku = storeSku;
            Map<String, String> skuMap = storeSku2skuMappings.get(appstoreName);
            if (skuMap != null && skuMap.get(sku) != null) {
                sku = skuMap.get(sku);
                Log.d(TAG, "getSku() restore sku from storeSku: " + storeSku + " -> " + sku);
            }
            return sku;
        }
    }

    public static List<String> getAllStoreSkus(final String appstoreName) {
        Map<String, String> skuMap = sku2storeSkuMappings.get(appstoreName);
        List<String> result = new ArrayList<String>();
        if (skuMap != null) {
            result.addAll(skuMap.values());
        }
        return result;
    }

    public interface OnOpenIabHelperInitFinished {
        public void onOpenIabHelperInitFinished();
    }

    /**
     * @deprecated TODO: need to add limited list of stores to avoid store elections 
     */
    public OpenIabHelper(Context context, AppstoreServiceManager manager, Map<String, String> extra) {
        mContext = context;
        mServiceManager = manager;
    }

    public OpenIabHelper(Context context, Map<String, String> storeKeys) {
        this(context, storeKeys, null);
    }
    
    public OpenIabHelper(Context context, Map<String, String> storeKeys, String[] prefferedStores) {
        this.mContext = context;
        this.mServiceManager = new AppstoreServiceManager(context, storeKeys, prefferedStores, new Appstore[] {
                    new GooglePlay(context, storeKeys.get(OpenIabHelper.NAME_GOOGLE))
                ,   new AmazonAppstore(context)
                ,   new SamsungApps(context)
                ,   new TStore(context, storeKeys.get(OpenIabHelper.NAME_TSTORE))
        });
    }

    /**
     *  Discover available stores and select the best billing service. Calls listener when service is found
     */
    public void startSetup(final IabHelper.OnIabSetupFinishedListener listener) {
        mServiceManager.startSetup(new AppstoreServiceManager.OnInitListener() {
            @Override
            public void onInitFinished() {// called in UI when the last openstore service is connected and analyzed  
                
                mAppstore = mServiceManager.selectBillingService();
                if (mAppstore == null) {
                    IabResult iabResult = new IabResult(BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE, "Billing isn't supported");
                    listener.onIabSetupFinished(iabResult);
                    return;
                }
                
                mAppstoreBillingService = mAppstore.getInAppBillingService(); 
                mAppstoreBillingService.startSetup(new OnIabSetupFinishedListener() {
                    public void onIabSetupFinished(IabResult result) {
                        // TODO: if result is not ok, is setupDone true?
                        mSetupDone = true;
                        listener.onIabSetupFinished(result);
                    }
                });
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
        checkSetupDone("launchPurchaseFlow");
        String storeSku = getStoreSku(mAppstore.getAppstoreName(), sku);
        mAppstoreBillingService.launchPurchaseFlow(act, storeSku, itemType, requestCode, listener, extraData);
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return mAppstoreBillingService.handleActivityResult(requestCode, resultCode, data);
    }

    /**
     * See {@link #queryInventory(boolean, List, List)} for details
     */
    public Inventory queryInventory(boolean querySkuDetails, List<String> moreSkus) throws IabException {
        return queryInventory(querySkuDetails, moreSkus, null);
    }

    /**
     * Queries the inventory. This will query all owned items from the server, as well as
     * information on additional skus, if specified. This method may block or take long to execute.
     * Do not call from a UI thread. For that, use the non-blocking version {@link #refreshInventoryAsync}.
     *
     * @param querySkuDetails if true, SKU details (price, description, etc) will be queried as well
     *                        as purchase information.
     * @param moreItemSkus    additional PRODUCT skus to query information on, regardless of ownership.
     *                        Ignored if null or if querySkuDetails is false.
     * @param moreSubsSkus    additional SUBSCRIPTIONS skus to query information on, regardless of ownership.
     *                        Ignored if null or if querySkuDetails is false.
     * @throws IabException if a problem occurs while refreshing the inventory.
     */
    public Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus, List<String> moreSubsSkus) throws IabException {
        checkSetupDone("queryInventory");

        List<String> moreItemStoreSkus = null;
        if (moreItemSkus != null) {
            moreItemStoreSkus = new ArrayList<String>();
            for (String sku : moreItemSkus) {
                String storeSku = getStoreSku(mAppstore.getAppstoreName(), sku);
                moreItemStoreSkus.add(storeSku);
            }
        }
        List<String> moreSubsStoreSkus = null;
        if (moreSubsSkus != null) {
            moreSubsStoreSkus = new ArrayList<String>();
            for (String sku : moreSubsSkus) {
                String storeSku = getStoreSku(mAppstore.getAppstoreName(), sku);
                moreSubsStoreSkus.add(storeSku);
            }
        }
        return mAppstoreBillingService.queryInventory(querySkuDetails, moreItemStoreSkus, moreSubsStoreSkus);
    }

    public void queryInventoryAsync(final boolean querySkuDetails,
                                    final List<String> moreSkus,
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
        checkSetupDone("consume");
        Purchase purchaseStoreSku = (Purchase) itemInfo.clone(); // TODO: use Purchase.getStoreSku()
        purchaseStoreSku.setSku(getStoreSku(mAppstore.getAppstoreName(), itemInfo.getSku()));
        mAppstoreBillingService.consume(purchaseStoreSku);
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
