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

package org.onepf.oms.appstore;

import java.util.LinkedList;
import java.util.Map;

import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.appstore.googleUtils.Purchase;
import org.onepf.oms.appstore.googleUtils.SkuDetails;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.amazon.inapp.purchasing.BasePurchasingObserver;
import com.amazon.inapp.purchasing.GetUserIdResponse;
import com.amazon.inapp.purchasing.Item;
import com.amazon.inapp.purchasing.ItemDataResponse;
import com.amazon.inapp.purchasing.Offset;
import com.amazon.inapp.purchasing.PurchaseResponse;
import com.amazon.inapp.purchasing.PurchaseUpdatesResponse;
import com.amazon.inapp.purchasing.PurchasingManager;
import com.amazon.inapp.purchasing.Receipt;
import com.amazon.inapp.purchasing.SubscriptionPeriod;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 16.04.13
 */
public class AmazonAppstoreObserver extends BasePurchasingObserver {

    private static final String TAG = AmazonAppstoreObserver.class.getSimpleName();
    private final AmazonAppstoreBillingService mBillingService;
    private final Context mContext;
    private Offset mOffset;

    AmazonAppstoreObserver(Context context, AmazonAppstoreBillingService billingService) {
        super(context);
        mContext = context;
        mBillingService = billingService;
    }

    @Override
    public void onSdkAvailable(final boolean isSandboxMode) {
        Log.v(TAG, "onSdkAvailable recieved: Response - " + isSandboxMode);
    }

    @Override
    public void onGetUserIdResponse(final GetUserIdResponse userIdResponse) {
        Log.d(TAG, "onGetUserIdResponse() getUserIdResponse: " + userIdResponse + ", requestId: " + userIdResponse.getRequestId() 
                + ", idRequestStatus: " + userIdResponse.getUserIdRequestStatus());

        if (userIdResponse.getUserIdRequestStatus() == GetUserIdResponse.GetUserIdRequestStatus.SUCCESSFUL) {
            final String userId = userIdResponse.getUserId();
            Log.d(TAG, "Set current userId: " + userId);
            mBillingService.setCurrentUser(userId);
        } else {
            Log.d(TAG, "onGetUserIdResponse() Unable to get user ID");
        }
    }

    @Override
    public void onPurchaseUpdatesResponse(final PurchaseUpdatesResponse purchaseUpdatesResponse) {
        Log.v(TAG, "onPurchaseUpdatesRecived recieved: Response -" + purchaseUpdatesResponse);
        Log.v(TAG, "PurchaseUpdatesRequestStatus:" + purchaseUpdatesResponse.getPurchaseUpdatesRequestStatus());
        Log.v(TAG, "RequestID:" + purchaseUpdatesResponse.getRequestId());
        new PurchaseUpdatesAsyncTask().execute(purchaseUpdatesResponse);
    }

    private class PurchaseUpdatesAsyncTask extends AsyncTask<PurchaseUpdatesResponse, Void, Boolean> {

        @Override
        protected Boolean doInBackground(final PurchaseUpdatesResponse... params) {
            final PurchaseUpdatesResponse purchaseUpdatesResponse = params[0];
            final String userId = mBillingService.getCurrentUser();
            if (!purchaseUpdatesResponse.getUserId().equals(userId)) {
                Log.w(TAG, "Current UserId: " + userId + ", purchase UserId: " + purchaseUpdatesResponse.getUserId());
                mBillingService.getInventoryLatch().countDown();
                return false;
            }
            Inventory inventory = mBillingService.getInventory();

            // TODO: do something with this
            for (final String sku : purchaseUpdatesResponse.getRevokedSkus()) {
                Log.v(TAG, "Revoked Sku:" + sku);
            }

            switch (purchaseUpdatesResponse.getPurchaseUpdatesRequestStatus()) {
                case SUCCESSFUL:
                    SubscriptionPeriod latestSubscriptionPeriod = null;
                    final LinkedList<SubscriptionPeriod> currentSubscriptionPeriods = new LinkedList<SubscriptionPeriod>();
                    for (final Receipt receipt : purchaseUpdatesResponse.getReceipts()) {

                        final String storeSku = receipt.getSku();
                        Purchase purchase;
                        switch (receipt.getItemType()) {
                            case ENTITLED:
                                purchase = new Purchase(OpenIabHelper.NAME_AMAZON);
                                purchase.setItemType(IabHelper.ITEM_TYPE_INAPP);
                                purchase.setSku(OpenIabHelper.getSku(OpenIabHelper.NAME_AMAZON, storeSku));
                                inventory.addPurchase(purchase);
                                Log.d(TAG, "Add to inventory SKU: " + storeSku);
                                break;
                            case SUBSCRIPTION:
                                final SubscriptionPeriod subscriptionPeriod = receipt.getSubscriptionPeriod();
                                if (subscriptionPeriod.getEndDate() == null) {
                                    purchase = new Purchase(OpenIabHelper.NAME_AMAZON);
                                    purchase.setItemType(IabHelper.ITEM_TYPE_SUBS);
                                    purchase.setSku(OpenIabHelper.getSku(OpenIabHelper.NAME_AMAZON, storeSku));
                                    inventory.addPurchase(purchase);
                                    Log.d(TAG, "Add subscription to inventory SKU: " + storeSku);
                                }
//                                final Date startDate = subscriptionPeriod.getStartDate();
//                                if (latestSubscriptionPeriod == null ||
//                                        startDate.after(latestSubscriptionPeriod.getStartDate())) {
//                                    currentSubscriptionPeriods.clear();
//                                    latestSubscriptionPeriod = subscriptionPeriod;
//                                    currentSubscriptionPeriods.add(latestSubscriptionPeriod);
//                                } else if (startDate.equals(latestSubscriptionPeriod.getStartDate())) {
//                                    currentSubscriptionPeriods.add(receipt.getSubscriptionPeriod());
//                                }

                                break;

                        }
                        //printReceipt(receipt);
                    }
                /*
                 * Check the latest subscription periods once all receipts have been read, if there is a subscription
                 * with an existing end date, then the subscription is not active.
                 */
//                    if (latestSubscriptionPeriod != null) {
//                        boolean hasSubscription = true;
//                        for (SubscriptionPeriod subscriptionPeriod : currentSubscriptionPeriods) {
//                            if (subscriptionPeriod.getEndDate() != null) {
//                                hasSubscription = false;
//                                break;
//                            }
//                        }
//                        editor.putBoolean(GameActivity.ORANGE_CELLS, hasSubscription);
//                    }

                    final Offset newOffset = purchaseUpdatesResponse.getOffset();
                    mOffset = newOffset;
                    if (purchaseUpdatesResponse.isMore()) {
                        Log.v(TAG, "Initiating Another Purchase Updates with offset: " + newOffset.toString());
                        PurchasingManager.initiatePurchaseUpdatesRequest(newOffset);
                    } else {
                        mBillingService.getInventoryLatch().countDown();
                    }
                    return true;
                case FAILED:
                    mBillingService.getInventoryLatch().countDown();
                    return false;
            }
            mBillingService.getInventoryLatch().countDown();
            return false;
        }
    }

    @Override
    public void onPurchaseResponse(final PurchaseResponse purchaseResponse) {
        Log.v(TAG, "onPurchaseResponse recieved");
        Log.v(TAG, "PurchaseRequestStatus:" + purchaseResponse.getPurchaseRequestStatus());
        new PurchaseAsyncTask().execute(purchaseResponse);
    }

    private class PurchaseAsyncTask extends AsyncTask<PurchaseResponse, Void, Pair<IabHelper.OnIabPurchaseFinishedListener, Pair<IabResult, Purchase>>> {
        @Override
        protected Pair<IabHelper.OnIabPurchaseFinishedListener, Pair<IabResult, Purchase>> doInBackground(final PurchaseResponse... params) {
            final PurchaseResponse purchaseResponse = params[0];
            final String userId = mBillingService.getCurrentUser();

            if (!purchaseResponse.getUserId().equals(userId)) {
                mBillingService.setCurrentUser(purchaseResponse.getUserId());
            }
            Purchase purchase = new Purchase(OpenIabHelper.NAME_AMAZON);
            IabResult result = null;
            switch (purchaseResponse.getPurchaseRequestStatus()) {
                case SUCCESSFUL:
                    final Receipt receipt = purchaseResponse.getReceipt();
                    final String storeSku = receipt.getSku();
                    purchase.setSku(OpenIabHelper.getSku(OpenIabHelper.NAME_AMAZON, storeSku));
                    switch (receipt.getItemType()) {
                        case CONSUMABLE:
                        case ENTITLED:
                            purchase.setItemType(IabHelper.ITEM_TYPE_INAPP);
                            break;
                        case SUBSCRIPTION:
                            purchase.setItemType(IabHelper.ITEM_TYPE_SUBS);
                            break;
                    }

                    //printReceipt(purchaseResponse.getReceipt());
                    result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, "Success");
                    break;
                case ALREADY_ENTITLED:
                    result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED, "Already owned");
                    break;
                case FAILED:
                    result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_USER_CANCELED, "Purchase failed");
                    break;
                case INVALID_SKU:
                    result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ERROR, "Invalid sku");
                    break;
            }
            IabHelper.OnIabPurchaseFinishedListener listener = mBillingService.getRequestListener(purchaseResponse.getRequestId());
            Log.d(TAG, "Result message: " + result.getMessage() + ", SKU: " + purchase.getSku());
            return new Pair<IabHelper.OnIabPurchaseFinishedListener, Pair<IabResult, Purchase>>(listener, new Pair<IabResult, Purchase>(result, purchase));
        }

        @Override
        protected void onPostExecute(final Pair<IabHelper.OnIabPurchaseFinishedListener, Pair<IabResult, Purchase>> result) {
            if (result.first != null) {
                result.first.onIabPurchaseFinished(result.second.first, result.second.second);
            } else {
                Log.e(TAG, "Something went wrong: PurchaseFinishedListener is null");
            }
        }
    }

    @Override
    public void onItemDataResponse(final ItemDataResponse itemDataResponse) {
        Log.v(TAG, "onItemDataResponse recieved");
        Log.v(TAG, "ItemDataRequestStatus" + itemDataResponse.getItemDataRequestStatus());
        Log.v(TAG, "ItemDataRequestId" + itemDataResponse.getRequestId());
        new ItemDataAsyncTask().execute(itemDataResponse);
    }

    private class ItemDataAsyncTask extends AsyncTask<ItemDataResponse, Void, Void> {
        @Override
        protected Void doInBackground(final ItemDataResponse... params) {
            final ItemDataResponse itemDataResponse = params[0];

            switch (itemDataResponse.getItemDataRequestStatus()) {
                case SUCCESSFUL_WITH_UNAVAILABLE_SKUS:
                    // Skus that you can not purchase will be here.
                    for (final String s : itemDataResponse.getUnavailableSkus()) {
                        Log.v(TAG, "Unavailable SKU:" + s);
                    }
                case SUCCESSFUL:
                    // Information you'll want to display about your IAP items is here
                    // In this example we'll simply log them.
                    Inventory inventory = mBillingService.getInventory();
                    final Map<String, Item> items = itemDataResponse.getItemData();
                    for (final String key : items.keySet()) {
                        Item i = items.get(key);
                        Log.v(TAG, String.format("Item: %s\n Type: %s\n SKU: %s\n Price: %s\n Description: %s\n", i.getTitle(), i.getItemType(), i.getSku(), i.getPrice(), i.getDescription()));
                        String itemType = i.getItemType() == Item.ItemType.SUBSCRIPTION ? IabHelper.ITEM_TYPE_INAPP : IabHelper.ITEM_TYPE_INAPP;
                        SkuDetails skuDetails = new SkuDetails(itemType, i.getSku(), i.getTitle(), i.getPrice(), i.getDescription());
                        inventory.addSkuDetails(skuDetails);
                    }
                    break;
                case FAILED:
                    // On failed responses will fail gracefully.
                    break;
            }
            mBillingService.getInventoryLatch().countDown();
            return null;
        }
    }
}
