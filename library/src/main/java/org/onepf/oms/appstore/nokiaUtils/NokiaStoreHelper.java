/*
 * Copyright 2012-2014 One Platform Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onepf.oms.appstore.nokiaUtils;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.*;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.nokia.payment.iap.aidl.INokiaIAPService;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.onepf.oms.Appstore;
import org.onepf.oms.AppstoreInAppBillingService;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.SkuManager;
import org.onepf.oms.appstore.NokiaStore;
import org.onepf.oms.appstore.googleUtils.*;
import org.onepf.oms.util.CollectionUtils;
import org.onepf.oms.util.Logger;

import java.util.ArrayList;
import java.util.List;

public class NokiaStoreHelper implements AppstoreInAppBillingService {
    //todo why own constants?
    public static final int RESULT_OK = 0;
    public static final int RESULT_USER_CANCELED = 1;
    public static final int RESULT_BILLING_UNAVAILABLE = 3;
    public static final int RESULT_ITEM_UNAVAILABLE = 4;
    public static final int RESULT_DEVELOPER_ERROR = 5;
    public static final int RESULT_ERROR = 6;
    public static final int RESULT_ITEM_ALREADY_OWNED = 7;
    public static final int RESULT_ITEM_NOT_OWNED = 8;
    public static final int RESULT_NO_SIM = 9;

    public static final int RESULT_BAD_RESPONSE = -1002;

    private final Context mContext;
    int mRequestCode;

    @Nullable
    private ServiceConnection mServiceConn = null;
    @Nullable
    private INokiaIAPService mService = null;
    @Nullable
    private IabHelper.OnIabPurchaseFinishedListener mPurchaseListener = null;

    public NokiaStoreHelper(final Context context, final Appstore appstore) {

        this.mContext = context;
    }

    /**
     * Initialization of service. After initialization is completed listener.onIabSetupFinished()
     * must be called in UI thread
     */
    @Override
    public void startSetup(@Nullable final IabHelper.OnIabSetupFinishedListener listener) {
        Logger.i("NokiaStoreHelper.startSetup");

        mServiceConn = new ServiceConnection() {
            @Override
            public void onServiceConnected(final ComponentName name, final IBinder service) {
                Logger.i("NokiaStoreHelper:startSetup.onServiceConnected");
                Logger.d("name = " + name);

                mService = INokiaIAPService.Stub.asInterface(service);

                try {
                    final int response = mService.isBillingSupported(3, getPackageName(), IabHelper.ITEM_TYPE_INAPP);

                    if (response != IabHelper.BILLING_RESPONSE_RESULT_OK) {

                        if (listener != null) {
                            listener.onIabSetupFinished(new NokiaResult(response,
                                    "Error checking for billing support."));
                        }

                        return;
                    }

                } catch (RemoteException e) {

                    if (listener != null) {
                        listener.onIabSetupFinished(
                                new NokiaResult(IabHelper.IABHELPER_REMOTE_EXCEPTION, "RemoteException while setting up in-app billing.")
                        );
                    }

                    Logger.e(e, "Exception: ", e);

                    return;
                }

                if (listener != null) {
                    listener.onIabSetupFinished(new NokiaResult(RESULT_OK, "Setup successful."));
                }

            }

            @Override
            public void onServiceDisconnected(final ComponentName name) {

                Logger.i("NokiaStoreHelper:startSetup.onServiceDisconnected");
                Logger.d("name = ", name);

                mService = null;
            }
        };

        final Intent serviceIntent = getServiceIntent();
        final List<ResolveInfo> servicesList = mContext.getPackageManager().queryIntentServices(serviceIntent, 0);

        if (servicesList == null || servicesList.isEmpty()) {

            if (listener != null) {
                listener.onIabSetupFinished(new NokiaResult(RESULT_BILLING_UNAVAILABLE,
                        "Billing service unavailable on device."));
            }

        } else {
            try {
                mContext.bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
            } catch (SecurityException e) {
                Logger.e("Can't bind to the service", e);
                if (listener != null) {
                    listener.onIabSetupFinished(new NokiaResult(RESULT_BILLING_UNAVAILABLE,
                            "Billing service unavailable on device due to lack of the permission \"com.nokia.payment.BILLING\"."));
                }
            }
        }
    }

    @NotNull
    private Intent getServiceIntent() {
        final Intent intent = new Intent(NokiaStore.VENDING_ACTION);

        intent.setPackage("com.nokia.payment.iapenabler");

        return intent;
    }

    /**
     * Initiate the UI flow for an in-app purchase. Call this method to initiate an in-app purchase,
     * which will involve bringing up the Nokia Store screen. The calling activity will be paused while
     * the user interacts with Nokia Store, and the result will be delivered via the activity's
     * {@link android.app.Activity#onActivityResult} method, at which point you must call
     * this object's {@link #handleActivityResult} method to continue the purchase flow. This method
     * MUST be called from the UI thread of the Activity.
     *
     * @param act         The calling activity.
     * @param sku         The sku of the item to purchase.
     * @param itemType    indicates if it's a product or a subscription (ITEM_TYPE_INAPP or ITEM_TYPE_SUBS)
     * @param requestCode A request code (to differentiate from other responses --
     *                    as in {@link android.app.Activity#startActivityForResult}).
     * @param listener    The listener to notify when the purchase process finishes
     * @param extraData   Extra data (developer payload), which will be returned with the purchase data
     *                    when the purchase completes. This extra data will be permanently bound to that purchase
     *                    and will always be returned when the purchase is queried.
     */
    @SuppressWarnings("MethodWithTooManyParameters")
    @Override
    public void launchPurchaseFlow(@NotNull final Activity act, final String sku, @NotNull final String itemType, final int requestCode,
                                   @Nullable final IabHelper.OnIabPurchaseFinishedListener listener, final String extraData) {

        Logger.i("NokiaStoreHelper.launchPurchaseFlow");

        if (itemType.equals(IabHelper.ITEM_TYPE_SUBS)) {

            final IabResult result = new IabResult(IabHelper.IABHELPER_SUBSCRIPTIONS_NOT_AVAILABLE, "Subscriptions are not available.");

            if (listener != null) {
                listener.onIabPurchaseFinished(result, null);
            }

            return;
        }

        try {
            if (mService == null) {
                if (listener != null) {
                    Logger.e("Unable to buy item, Error response: service is not connected.");
                    NokiaResult result = new NokiaResult(RESULT_ERROR, "Unable to buy item");
                    listener.onIabPurchaseFinished(result, null);
                }
            } else {
                final Bundle buyIntentBundle = mService.getBuyIntent(
                        3, getPackageName(), sku, IabHelper.ITEM_TYPE_INAPP, extraData
                );

                Logger.d("buyIntentBundle = ", buyIntentBundle);

                final int responseCode = buyIntentBundle.getInt("RESPONSE_CODE", 0);
                final PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");

                if (responseCode == RESULT_OK) {
                    mRequestCode = requestCode;
                    mPurchaseListener = listener;

                    final IntentSender intentSender = pendingIntent.getIntentSender();
                    act.startIntentSenderForResult(
                            intentSender, requestCode, new Intent(), 0, 0, 0
                    );
                } else if (listener != null) {
                    final IabResult result = new NokiaResult(responseCode, "Failed to get buy intent.");
                    listener.onIabPurchaseFinished(result, null);
                }
            }
        } catch (RemoteException e) {
            Logger.e(e, "RemoteException: ", e);

            final IabResult result = new NokiaResult(IabHelper.IABHELPER_SEND_INTENT_FAILED, "Failed to send intent.");
            if (listener != null) {
                listener.onIabPurchaseFinished(result, null);
            }

        } catch (IntentSender.SendIntentException e) {
            Logger.e(e, "SendIntentException: ", e);

            final IabResult result = new NokiaResult(IabHelper.IABHELPER_REMOTE_EXCEPTION,
                    "Remote exception while starting purchase flow");

            if (listener != null) {
                listener.onIabPurchaseFinished(result, null);
            }

        }
    }

    /**
     * Handles an activity result that's part of the purchase flow in in-app billing. If you
     * are calling {@link org.onepf.oms.appstore.nokiaUtils.NokiaStoreHelper#launchPurchaseFlow}, then you must call this method from your
     * Activity's {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)} method. This method
     * MUST be called from the UI thread of the Activity.
     *
     * @param requestCode The requestCode as you received it.
     * @param resultCode  The resultCode as you received it.
     * @param data        The data (Intent) as you received it.
     * @return Returns true if the result was related to a purchase flow and was handled;
     * false if the result was not related to a purchase, in which case you should
     * handle it normally.
     */

    @Override
    public boolean handleActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        Logger.i("NokiaStoreHelper.handleActivityResult");

        if (requestCode != mRequestCode) {
            return false;
        }

        IabResult result;

        if (data == null) {
            Logger.e("Null data in IAB activity result.");
            result = new NokiaResult(IabHelper.IABHELPER_BAD_RESPONSE, "Null data in IAB result");

            if (mPurchaseListener != null) {
                mPurchaseListener.onIabPurchaseFinished(result, null);
            }

            return true;
        }

        final int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
        final String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");

        Logger.d("responseCode = ", responseCode);
        Logger.d("purchaseData = ", purchaseData);

        if (resultCode == Activity.RESULT_OK && responseCode == RESULT_OK) {

            processPurchaseSuccess(purchaseData);

        } else if (resultCode == Activity.RESULT_OK) {

            processPurchaseFail(responseCode);

        } else if (resultCode == Activity.RESULT_CANCELED) {

            Logger.d("Purchase canceled - Response: ", responseCode);

            result = new NokiaResult(IabHelper.IABHELPER_USER_CANCELLED, "User canceled.");

            if (mPurchaseListener != null) {
                mPurchaseListener.onIabPurchaseFinished(result, null);
            }

        } else {

            Logger.e("Purchase failed. Result code: ", resultCode);

            result = new NokiaResult(IabHelper.IABHELPER_UNKNOWN_PURCHASE_RESPONSE, "Unknown purchase response.");

            if (mPurchaseListener != null) {
                mPurchaseListener.onIabPurchaseFinished(result, null);
            }
        }

        return true;
    }

    /**
     * Called if purchase has been failed
     *
     * @param responseCode Response code for IabResult
     */
    public void processPurchaseFail(final int responseCode) {

        Logger.d("Result code was OK but in-app billing response was not OK: ", responseCode);

        if (mPurchaseListener != null) {
            final IabResult result = new NokiaResult(responseCode, "Problem purchashing item.");
            mPurchaseListener.onIabPurchaseFinished(result, null);
        }
    }

    /**
     * Called if purchase has been successful
     *
     * @param purchaseData Response code for IabResult
     */
    private void processPurchaseSuccess(final String purchaseData) {
        Logger.i("NokiaStoreHelper.processPurchaseSuccess");
        Logger.d("purchaseData = ", purchaseData);

        Purchase purchase;
        try {
            final JSONObject obj = new JSONObject(purchaseData);

            final String sku = SkuManager.getInstance().getSku(OpenIabHelper.NAME_NOKIA, obj.getString("productId"));

            Logger.d("sku = ", sku);

            purchase = new Purchase(OpenIabHelper.NAME_NOKIA);

            purchase.setItemType(IabHelper.ITEM_TYPE_INAPP);
            purchase.setOrderId(obj.getString("orderId"));
            purchase.setPackageName(obj.getString("packageName"));
            purchase.setSku(sku);
            purchase.setToken(obj.getString("purchaseToken"));
            purchase.setDeveloperPayload(obj.getString("developerPayload"));

        } catch (JSONException e) {
            Logger.e(e, "JSONException: ", e);

            final IabResult result = new NokiaResult(IabHelper.IABHELPER_BAD_RESPONSE, "Failed to parse purchase data.");
            if (mPurchaseListener != null) {
                mPurchaseListener.onIabPurchaseFinished(result, null);
            }

            return;
        }

        if (mPurchaseListener != null) {
            mPurchaseListener.onIabPurchaseFinished(new NokiaResult(RESULT_OK, "Success"), purchase);
        }

    }

    /**
     * Consumes a given in-app product. Consuming can only be done on an item
     * that's owned, and as a result of consumption, the user will no longer own it.
     * This method may block or take long to return. Do not call from the UI thread.
     * For that, see
     * {@link org.onepf.oms.OpenIabHelper#consumeAsync(org.onepf.oms.appstore.googleUtils.Purchase, org.onepf.oms.appstore.googleUtils.IabHelper.OnConsumeFinishedListener)}.
     *
     * @param itemInfo The PurchaseInfo that represents the item to consume.
     * @throws org.onepf.oms.appstore.googleUtils.IabException if there is a problem during consumption.
     */
    @Override
    public void consume(@NotNull final Purchase itemInfo) throws IabException {
        Logger.i("NokiaStoreHelper.consume");

        final String token = itemInfo.getToken();
        final String productId = itemInfo.getSku();
        final String packageName = itemInfo.getPackageName();

        Logger.d("productId = ", productId);
        Logger.d("token = ", token);
        Logger.d("packageName = ", packageName);

        int response = 0;
        try {
            response = mService.consumePurchase(3, packageName, productId, token);
        } catch (RemoteException e) {
            Logger.e(e, "RemoteException: ", e);
        }

        if (response == RESULT_OK) {
            Logger.d("Successfully consumed productId: ", productId);
        } else {
            Logger.d("Error consuming consuming productId ", productId, ". Code: ", response);
            throw new IabException(new NokiaResult(response, "Error consuming productId " + productId));
        }

        Logger.d("consume: done");
    }

    @Override
    public boolean subscriptionsSupported() {

        // Subscriptions are not supported right now
        return false;
    }

    /**
     * Queries the inventory. This will query all owned items from the server, as well as
     * information on additional skus, if specified. This method may block or take long to execute.
     * Do not call from a UI thread. For that, use the non-blocking version {@link org.onepf.oms.OpenIabHelper#queryInventoryAsync(boolean, java.util.List,
     * org.onepf.oms.appstore.googleUtils.IabHelper.QueryInventoryFinishedListener)}
     *
     * @param querySkuDetails if true, SKU details (price, description, etc) will be queried as well
     *                        as purchase information.
     * @param moreItemSkus    additional PRODUCT skus to query information on, regardless of ownership.
     *                        Ignored if null or if querySkuDetails is false.
     * @param moreSubsSkus    additional SUBSCRIPTIONS skus to query information on, regardless of ownership.
     *                        Ignored if null or if querySkuDetails is false.
     * @throws org.onepf.oms.appstore.googleUtils.IabException if a problem occurs while refreshing the inventory.
     */

    @Override
    public Inventory queryInventory(final boolean querySkuDetails, final List<String> moreItemSkus,
                                    final List<String> moreSubsSkus)

            throws IabException {

        final Inventory inventory = new Inventory();

        Logger.i("NokiaStoreHelper.queryInventory");
        Logger.d("querySkuDetails = ", querySkuDetails);
        Logger.d("moreItemSkus = ", moreItemSkus);

        if (querySkuDetails) {
            refreshItemDetails(moreItemSkus, inventory);
        }

        refreshPurchasedItems(moreItemSkus, inventory);

        return inventory;
    }

    private void refreshPurchasedItems(@Nullable final List<String> moreItemSkus, @NotNull final Inventory inventory)
            throws IabException {
        Logger.i("NokiaStoreHelper.refreshPurchasedItems");

        final ArrayList<String> storeSkus = new ArrayList<String>(
                SkuManager.getInstance().getAllStoreSkus(OpenIabHelper.NAME_NOKIA));
        final Bundle storeSkusBundle = new Bundle(32);

        if (moreItemSkus != null) {
            for (final String moreItemSku : moreItemSkus) {
                storeSkus.add(moreItemSku);
            }
        }

        storeSkusBundle.putStringArrayList("ITEM_ID_LIST", storeSkus);

        try {
            if (mService == null) {
                Logger.e("Unable to refresh purchased items.");
                throw new IabException(RESULT_BAD_RESPONSE, "Error refreshing inventory (querying owned items).");
            }

            final Bundle purchasedBundle = mService.getPurchases(
                    3, getPackageName(), OpenIabHelper.ITEM_TYPE_INAPP, storeSkusBundle, null
            );

            final int responseCode = purchasedBundle.getInt("RESPONSE_CODE");
            final ArrayList<String> purchasedItemList = purchasedBundle.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
            final ArrayList<String> purchasedDataList = purchasedBundle.getStringArrayList("INAPP_PURCHASE_DATA_LIST");

            Logger.d("responseCode = ", responseCode);
            Logger.d("purchasedItemList = ", purchasedItemList);
            Logger.d("purchasedDataList = ", purchasedDataList);

            if (responseCode != RESULT_OK) {
                throw new IabException(new NokiaResult(responseCode, "Error refreshing inventory (querying owned items)."));
            }

            processPurchasedList(purchasedDataList, inventory);

        } catch (RemoteException e) {
            Logger.e(e, "Exception: ", e);
        }
    }

    private void processPurchasedList(@NotNull final ArrayList<String> purchasedDataList, @NotNull final Inventory inventory) {
        Logger.i("NokiaStoreHelper.processPurchasedList");

        for (final String data : purchasedDataList) {
            try {
                final JSONObject obj = new JSONObject(data);
                final Purchase purchase = new Purchase(OpenIabHelper.NAME_NOKIA);
                purchase.setItemType(IabHelper.ITEM_TYPE_INAPP);
                purchase.setSku(SkuManager.getInstance().getSku(OpenIabHelper.NAME_NOKIA, obj.getString("productId")));
                purchase.setToken(obj.getString("purchaseToken"));
                purchase.setPackageName(getPackageName());
                purchase.setPurchaseState(0);
                purchase.setDeveloperPayload(obj.optString("developerPayload", ""));
                inventory.addPurchase(purchase);
            } catch (JSONException e) {
                Logger.e(e, "Exception: ", e);
            }
        }
    }

    private void refreshItemDetails(@Nullable final List<String> moreItemSkus, @NotNull final Inventory inventory) throws IabException {
        Logger.i("NokiaStoreHelper.refreshItemDetails");

        final Bundle storeSkusBundle = new Bundle(32);

        final ArrayList<String> combinedStoreSkus = new ArrayList<String>(32);

        List<String> allNokiaStoreSkus = SkuManager.getInstance().getAllStoreSkus(OpenIabHelper.NAME_NOKIA);

        if (!CollectionUtils.isEmpty(allNokiaStoreSkus)) {
            combinedStoreSkus.addAll(allNokiaStoreSkus);
        }

        if (moreItemSkus != null) {
            for (final String moreItemSku : moreItemSkus) {
                combinedStoreSkus.add(SkuManager.getInstance().getStoreSku(OpenIabHelper.NAME_NOKIA, moreItemSku));
            }
        }

        storeSkusBundle.putStringArrayList("ITEM_ID_LIST", combinedStoreSkus);

        try {
            if (mService == null) {
                Logger.e("Unable to refresh item details.");
                throw new IabException(RESULT_BAD_RESPONSE, "Error refreshing item details.");
            }

            final Bundle productDetailBundle = mService.getProductDetails(
                    3, getPackageName(), OpenIabHelper.ITEM_TYPE_INAPP, storeSkusBundle
            );

            final int responseCode = productDetailBundle.getInt("RESPONSE_CODE");
            final List<String> detailsList = productDetailBundle.getStringArrayList("DETAILS_LIST");

            Logger.d("responseCode = ", responseCode);
            Logger.d("detailsList = ", detailsList);

            if (responseCode != RESULT_OK) {
                throw new IabException(new NokiaResult(responseCode, "Error refreshing inventory (querying prices of items)."));
            }

            processDetailsList(detailsList, inventory);

        } catch (RemoteException e) {
            Logger.e(e, "Exception: ", e);
        } catch (JSONException e) {
            Logger.e(e, "Exception: ", e);
        }
    }

    private void processDetailsList(@NotNull final List<String> detailsList, @NotNull final Inventory inventory)
            throws JSONException {

        Logger.i("NokiaStoreHelper.processDetailsList");

        for (final String detailString : detailsList) {
            final JSONObject obj = new JSONObject(detailString);
            inventory.addSkuDetails(new SkuDetails(
                    IabHelper.ITEM_TYPE_INAPP,
                    SkuManager.getInstance().getSku(OpenIabHelper.NAME_NOKIA, obj.getString("productId")),
                    obj.getString("title"),
                    obj.getString("price"),
                    obj.getString("shortdescription")));
        }
    }

    @Override
    public void dispose() {
        Logger.i("NokiaStoreHelper.dispose");

        if (mServiceConn != null) {
            if (mContext != null) {
                mContext.unbindService(mServiceConn);
            }
            mServiceConn = null;
            mService = null;
        }
    }

    public String getPackageName() {
        return mContext.getPackageName();
    }
}
