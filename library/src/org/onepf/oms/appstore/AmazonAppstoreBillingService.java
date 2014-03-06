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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.onepf.oms.AppstoreInAppBillingService;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.appstore.googleUtils.Purchase;
import org.onepf.oms.appstore.googleUtils.SkuDetails;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

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
 * Amazon billing service impl
 * 
 * @author Ruslan Sayfutdinov, Oleg Orlov
 * @since 16.04.13
 */
public class AmazonAppstoreBillingService extends BasePurchasingObserver implements AppstoreInAppBillingService {
    private static final String TAG = AmazonAppstoreBillingService.class.getSimpleName();

    private static boolean isDebugLog(){
        return OpenIabHelper.isDebugLog();
    }
    
    private Map<String, IabHelper.OnIabPurchaseFinishedListener> mRequestListeners = new HashMap<String, IabHelper.OnIabPurchaseFinishedListener>();
    
    /** 
     * Only for verification all requests are for the same user
     * <p>Not expected to be undefined after setup is completed
     * <p>Initialized at {@link #onGetUserIdResponse(GetUserIdResponse)} if GetUserIdRequestStatus.SUCCESSFUL 
     * durint startSetup().
     */
    private String currentUserId;
    
    /** Maintained internally by 
     * <li>{@link #queryInventory(boolean, List, List)}
     * <li>{@link #onPurchaseUpdatesResponse(PurchaseUpdatesResponse)}
     * <li>{@link #onItemDataResponse(ItemDataResponse)}*/
    private Inventory inventory;
    
    /** If not null will be notified from  */
    private IabHelper.OnIabSetupFinishedListener setupListener;

    /** TODO: consider removal inventoryLatch or using carefully */
    private CountDownLatch inventoryLatch;

    public AmazonAppstoreBillingService(Context context) {
        super(context);
    }

    /**
     * @param listener - is triggered when {@link #onGetUserIdResponse(GetUserIdResponse)} happens 
     */
    @Override
    public void startSetup(IabHelper.OnIabSetupFinishedListener listener) {
        PurchasingManager.registerObserver(this);
        this.setupListener = listener;
    }

    @Override
    public void onSdkAvailable(final boolean isSandboxMode) {
        if (isDebugLog()) Log.v(TAG, "onSdkAvailable() isSandBox: " + isSandboxMode);
        PurchasingManager.initiateGetUserIdRequest();
    }

    @Override
    public void onGetUserIdResponse(final GetUserIdResponse userIdResponse) {
        if (isDebugLog()) Log.d(TAG, "onGetUserIdResponse() reqId: " + userIdResponse.getRequestId() + ", status: " + userIdResponse.getUserIdRequestStatus());

        if (userIdResponse.getUserIdRequestStatus() == GetUserIdResponse.GetUserIdRequestStatus.SUCCESSFUL) {
            final String userId = userIdResponse.getUserId();
            if (isDebugLog()) Log.d(TAG, "Set current userId: " + userId);
            this.currentUserId = userId;
            if (setupListener != null) {
                setupListener.onIabSetupFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, "Setup successful."));
                setupListener = null;
            }
        } else {
            if (isDebugLog()) Log.d(TAG, "onGetUserIdResponse() Unable to get user ID");
            if (setupListener != null) {
                setupListener.onIabSetupFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ERROR, "Unable to get userId"));
                setupListener = null;
            }
        }
    }
    
    @Override
    public Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus, List<String> moreSubsSkus) {
        if (isDebugLog()) Log.d(TAG, "queryInventory() querySkuDetails: " + querySkuDetails+ " moreItemSkus: " + moreItemSkus+ " moreSubsSkus: " + moreSubsSkus);
        inventory = new Inventory();
        inventoryLatch = new CountDownLatch(1);
        PurchasingManager.initiatePurchaseUpdatesRequest(Offset.BEGINNING);
        try {
            inventoryLatch.await();
        } catch (InterruptedException e) {
            return null;
        }
        if (querySkuDetails) {
            Set<String> querySkus = new HashSet<String>(inventory.getAllOwnedSkus());
            if (moreItemSkus != null) {
                querySkus.addAll(moreItemSkus);
            }
            if (moreSubsSkus != null) {
                querySkus.addAll(moreSubsSkus);
            }
            if (querySkus.size() > 0) {
                inventoryLatch = new CountDownLatch(1);
                HashSet<String> queryStoreSkus = new HashSet<String>(querySkus.size());
                for (String sku : querySkus) {
                    queryStoreSkus.add(OpenIabHelper.getStoreSku(OpenIabHelper.NAME_AMAZON, sku));
                }
                PurchasingManager.initiateItemDataRequest(queryStoreSkus);
                try {
                    inventoryLatch.await();
                } catch (InterruptedException e) {
                    if (isDebugLog()) Log.w(TAG, "queryInventory() SkuDetails fetching interrupted");
                }
            }
        }
        if (isDebugLog()) Log.d(TAG, "queryInventory() finished. Inventory size: " + inventory.getAllOwnedSkus().size());
        return inventory;
    }

    @Override
    public void onPurchaseUpdatesResponse(final PurchaseUpdatesResponse purchaseUpdatesResponse) {
        if (isDebugLog()) Log.v(TAG, "onPurchaseUpdatesResponse() reqStatus: " + purchaseUpdatesResponse.getPurchaseUpdatesRequestStatus() + "reqId: " + purchaseUpdatesResponse.getRequestId());
        
        if ((currentUserId != null) && !currentUserId.equals(purchaseUpdatesResponse.getUserId())) {
            if (isDebugLog()) Log.w(TAG, "onPurchaseUpdatesResponse() Current UserId: " + currentUserId + ", purchase UserId: " + purchaseUpdatesResponse.getUserId());
            inventoryLatch.countDown();
            return;
        }
        // TODO: do something with this
        for (final String sku : purchaseUpdatesResponse.getRevokedSkus()) {
            if (isDebugLog()) Log.v(TAG, "Revoked Sku:" + sku);
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
                            if (isDebugLog()) Log.d(TAG, "Add to inventory SKU: " + storeSku);
                            break;
                        case SUBSCRIPTION:
                            final SubscriptionPeriod subscriptionPeriod = receipt.getSubscriptionPeriod();
                            if (subscriptionPeriod.getEndDate() == null) {
                                purchase = new Purchase(OpenIabHelper.NAME_AMAZON);
                                purchase.setItemType(IabHelper.ITEM_TYPE_SUBS);
                                purchase.setSku(OpenIabHelper.getSku(OpenIabHelper.NAME_AMAZON, storeSku));
                                inventory.addPurchase(purchase);
                                if (isDebugLog()) Log.d(TAG, "Add subscription to inventory SKU: " + storeSku);
                            }
                            
//                            final Date startDate = subscriptionPeriod.getStartDate();
//                            if (latestSubscriptionPeriod == null || startDate.after(latestSubscriptionPeriod.getStartDate())) {
//                                currentSubscriptionPeriods.clear();
//                                latestSubscriptionPeriod = subscriptionPeriod;
//                                currentSubscriptionPeriods.add(latestSubscriptionPeriod);
//                            } else if (startDate.equals(latestSubscriptionPeriod.getStartDate())) {
//                                currentSubscriptionPeriods.add(receipt.getSubscriptionPeriod());
//                            }

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
                if (purchaseUpdatesResponse.isMore()) {
                    if (isDebugLog()) Log.v(TAG, "Initiating Another Purchase Updates with offset: " + newOffset.toString());
                    PurchasingManager.initiatePurchaseUpdatesRequest(newOffset);
                } else {
                    inventoryLatch.countDown();
                }
                return;
            case FAILED:
                inventoryLatch.countDown();
                return;
        }
        inventoryLatch.countDown();
        return;
    }

    @Override
    public void onItemDataResponse(final ItemDataResponse itemDataResponse) {
        if (isDebugLog()) Log.v(TAG, "onItemDataResponse() reqStatus: " + itemDataResponse.getItemDataRequestStatus()+ ", reqId: " + itemDataResponse.getRequestId());
        switch (itemDataResponse.getItemDataRequestStatus()) {
            case SUCCESSFUL_WITH_UNAVAILABLE_SKUS:
                // Skus that you can not purchase will be here.
                for (final String s : itemDataResponse.getUnavailableSkus()) {
                    if (isDebugLog()) Log.v(TAG, "Unavailable SKU:" + s);
                }
            case SUCCESSFUL:
                // Information you'll want to display about your IAP items is here
                // In this example we'll simply log them.
                final Map<String, Item> items = itemDataResponse.getItemData();
                for (final String key : items.keySet()) {
                    Item i = items.get(key);
                    final String storeSku = i.getSku();
                    if (isDebugLog()) Log.v(TAG, String.format("Item: %s\n Type: %s\n SKU: %s\n Price: %s\n Description: %s\n", i.getTitle(), i.getItemType(), storeSku, i.getPrice(), i.getDescription()));
                    String itemType = i.getItemType() == Item.ItemType.SUBSCRIPTION ? IabHelper.ITEM_TYPE_SUBS : IabHelper.ITEM_TYPE_INAPP;
                    String sku = OpenIabHelper.getSku(OpenIabHelper.NAME_AMAZON, storeSku);
                    SkuDetails skuDetails = new SkuDetails(itemType, sku, i.getTitle(), i.getPrice(), i.getDescription());
                    inventory.addSkuDetails(skuDetails);
                }
                break;
            case FAILED:
                // On failed responses will fail gracefully.
                break;
        }
        inventoryLatch.countDown();
    }
    
    @Override
    public void launchPurchaseFlow(Activity act, String sku, String itemType, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        String requestId = PurchasingManager.initiatePurchaseRequest(sku);
        storeRequestListener(requestId, listener);
    }

    @Override
    public void onPurchaseResponse(final PurchaseResponse purchaseResponse) {
        if (isDebugLog()) Log.v(TAG, "onPurchaseResponse() PurchaseRequestStatus:" + purchaseResponse.getPurchaseRequestStatus());

        IabResult result = null;
        Purchase purchase = new Purchase(OpenIabHelper.NAME_AMAZON);
        
        if ((currentUserId != null) && !currentUserId.equals(purchaseResponse.getUserId())) {
            if (isDebugLog()) Log.w(TAG, "onPurchaseResponse() userId: " + currentUserId + ", purchase.userId: " + purchaseResponse.getUserId());
            result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ERROR, "userId doesn't match purchase.userId");
        } else {
            switch (purchaseResponse.getPurchaseRequestStatus()) {
                case SUCCESSFUL :
                    final Receipt receipt = purchaseResponse.getReceipt();
                    final String storeSku = receipt.getSku();
                    purchase.setSku(OpenIabHelper.getSku(OpenIabHelper.NAME_AMAZON, storeSku));
                    switch (receipt.getItemType()) {
                        case CONSUMABLE :
                        case ENTITLED :
                            purchase.setItemType(IabHelper.ITEM_TYPE_INAPP);
                            break;
                        case SUBSCRIPTION :
                            purchase.setItemType(IabHelper.ITEM_TYPE_SUBS);
                            break;
                    }
                    //printReceipt(purchaseResponse.getReceipt());
                    result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, "Success");
                    break;
                case ALREADY_ENTITLED :
                    result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED, "Already owned");
                    break;
                case FAILED :
                    result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_USER_CANCELED, "Purchase failed");
                    break;
                case INVALID_SKU :
                    result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ERROR, "Invalid sku");
                    break;
            }
        }
        IabHelper.OnIabPurchaseFinishedListener listener = getRequestListener(purchaseResponse.getRequestId());
        if (listener != null) {
            listener.onIabPurchaseFinished(result, purchase);
        } else {
            Log.e(TAG, "Something went wrong: PurchaseFinishedListener is null");
        }
    }
    
    @Override
    public void consume(Purchase itemInfo) {
        // Nothing to do here
    }

    @Override
    public boolean subscriptionsSupported() {
        return true;
    }

    @Override
    public void dispose() {
        
    }

    @Override
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return false;
    }
    
    private void storeRequestListener(String requestId, IabHelper.OnIabPurchaseFinishedListener listener) {
        mRequestListeners.put(requestId, listener);
    }

    public IabHelper.OnIabPurchaseFinishedListener getRequestListener(String requestId) {
        return mRequestListeners.get(requestId);
    }
}
