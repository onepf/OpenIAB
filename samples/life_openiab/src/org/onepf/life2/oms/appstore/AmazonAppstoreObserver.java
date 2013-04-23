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

package org.onepf.life2.oms.appstore;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.amazon.inapp.purchasing.*;
import org.onepf.life2.oms.appstore.googleUtils.IabHelper;
import org.onepf.life2.oms.appstore.googleUtils.IabResult;
import org.onepf.life2.oms.appstore.googleUtils.Inventory;
import org.onepf.life2.oms.appstore.googleUtils.Purchase;

import java.util.LinkedList;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 16.04.13
 */
public class AmazonAppstoreObserver extends BasePurchasingObserver {

    private static final String TAG = "IabHelper";
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
        if (!isSandboxMode) {
            PurchasingManager.initiateGetUserIdRequest();
        }
    }

    @Override
    public void onGetUserIdResponse(final GetUserIdResponse getUserIdResponse) {
        Log.v(TAG, "onGetUserIdResponse recieved: Response -" + getUserIdResponse);
        Log.v(TAG, "RequestId:" + getUserIdResponse.getRequestId());
        Log.v(TAG, "IdRequestStatus:" + getUserIdResponse.getUserIdRequestStatus());
        new GetUserIdAsyncTask().execute(getUserIdResponse);
    }

    private class GetUserIdAsyncTask extends AsyncTask<GetUserIdResponse, Void, Boolean> {
        @Override
        protected Boolean doInBackground(final GetUserIdResponse... params) {
            GetUserIdResponse getUserIdResponse = params[0];

            if (getUserIdResponse.getUserIdRequestStatus() == GetUserIdResponse.GetUserIdRequestStatus.SUCCESSFUL) {
                final String userId = getUserIdResponse.getUserId();

                mBillingService.setCurrentUser(userId);
                return true;
            } else {
                Log.v(TAG, "onGetUserIdResponse: Unable to get user ID.");
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            super.onPostExecute(result);
//            if (result) {
//                if (mOffset == null) {
//                    PurchasingManager.initiatePurchaseUpdatesRequest(Offset.BEGINNING);
//                } else {
//                    PurchasingManager.initiatePurchaseUpdatesRequest(Offset.fromString(mOffset));
//                }
//            }
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
                    final LinkedList<SubscriptionPeriod> currentSubscriptionPeriods = new LinkedList<>();
                    for (final Receipt receipt : purchaseUpdatesResponse.getReceipts()) {

                        final String sku = receipt.getSku();
                        Purchase purchase;
                        switch (receipt.getItemType()) {
                            case ENTITLED:
                                purchase = new Purchase();
                                purchase.setItemType(IabHelper.ITEM_TYPE_INAPP);
                                purchase.setSku(sku);
                                inventory.addPurchase(purchase);
                                break;
                            case SUBSCRIPTION:
                                final SubscriptionPeriod subscriptionPeriod = receipt.getSubscriptionPeriod();
                                if (subscriptionPeriod.getEndDate() == null) {
                                    purchase = new Purchase();
                                    purchase.setItemType(IabHelper.ITEM_TYPE_SUBS);
                                    purchase.setSku(sku);
                                    inventory.addPurchase(purchase);
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
                    return false;
            }
            return false;
        }
    }

    @Override
    public void onPurchaseResponse(final PurchaseResponse purchaseResponse) {
        Log.v(TAG, "onPurchaseResponse recieved");
        Log.v(TAG, "PurchaseRequestStatus:" + purchaseResponse.getPurchaseRequestStatus());
        new PurchaseAsyncTask().execute(purchaseResponse);
    }

    private class PurchaseAsyncTask extends AsyncTask<PurchaseResponse, Void, Boolean> {
        @Override
        protected Boolean doInBackground(final PurchaseResponse... params) {
            final PurchaseResponse purchaseResponse = params[0];
            final String userId = mBillingService.getCurrentUser();
            IabHelper.OnIabPurchaseFinishedListener listener = mBillingService.getRequestListener(purchaseResponse.getRequestId());

            if (!purchaseResponse.getUserId().equals(userId)) {
                mBillingService.setCurrentUser(purchaseResponse.getUserId());
            }
            Purchase purchase = new Purchase();
            IabResult result = null;
            switch (purchaseResponse.getPurchaseRequestStatus()) {
                case SUCCESSFUL:
                    final Receipt receipt = purchaseResponse.getReceipt();
                    purchase.setSku(receipt.getSku());
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
            listener.onIabPurchaseFinished(result, purchase);
            return result != null && result.isSuccess();
        }
    }
}
