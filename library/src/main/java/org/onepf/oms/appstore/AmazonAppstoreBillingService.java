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

package org.onepf.oms.appstore;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.FulfillmentResult;
import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.ProductDataResponse;
import com.amazon.device.iap.model.ProductType;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.PurchaseUpdatesResponse;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.RequestId;
import com.amazon.device.iap.model.UserData;
import com.amazon.device.iap.model.UserDataResponse;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.onepf.oms.AppstoreInAppBillingService;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.SkuManager;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.appstore.googleUtils.Purchase;
import org.onepf.oms.appstore.googleUtils.SkuDetails;
import org.onepf.oms.util.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

/**
 * Amazon billing service impl
 *
 * @author Ruslan Sayfutdinov, Oleg Orlov, Roman Zhilich
 * @since 16.04.13
 */
public class AmazonAppstoreBillingService implements AppstoreInAppBillingService, PurchasingListener {

    // ========================================================================
    // PURCHASE RESPONSE JSON KEYS
    // ========================================================================
    public static final String JSON_KEY_ORDER_ID = "orderId";
    public static final String JSON_KEY_PRODUCT_ID = "productId";
    public static final String JSON_KEY_RECEIPT_ITEM_TYPE = "itemType";
    public static final String JSON_KEY_PURCHASE_STATUS = "purchaseStatus";
    public static final String JSON_KEY_USER_ID = "userId";
    public static final String JSON_KEY_RECEIPT_PURCHASE_TOKEN = "purchaseToken";

    private final Map<RequestId, IabHelper.OnIabPurchaseFinishedListener> requestListeners =
            new HashMap<RequestId, IabHelper.OnIabPurchaseFinishedListener>();

    private final Context context;

    /**
     * Only for verification all requests are for the same user
     * <p>Not expected to be undefined after setup is completed
     * <p>Initialized at {@link #onUserDataResponse(UserDataResponse)} if GetUserIdRequestStatus.SUCCESSFUL
     * during startSetup().
     */
    private String currentUserId;

    /**
     * To process {@link #queryInventory(boolean, List, List)} request following steps are done:
     * <p>
     * {@link #queryInventory(boolean, List, List)} - initialize inventory object, request purchase data by
     * <code>getPurchaseUpdates()</code> and locks thread on inventoryLatch.
     * After whole purchase data is received request SKU details by <code>getProductData()</code>
     * <br>
     * {@link #onPurchaseUpdatesResponse(PurchaseUpdatesResponse)} - triggered by Amazon SDK.
     * Handles purchases data chunk by chunk. Releases inventoryLatch lock after last chunk is handled.
     * <p>
     * {@link #onProductDataResponse(ProductDataResponse)} - triggered by Amazon SDK.
     * Handles items data. Releases inventoryLatch lock when all data is handled.
     * <p/>
     * <p>NOTES:</p>
     * Amazon SDK may trigger on*Response() before queryInventory() is called. It happens
     * when confirmation of processed purchase was not delivered to application (when applications
     * crashes or relaunched). So inventory object must not be null.
     */
    private final Inventory inventory = new Inventory();
    /**
     * Since {@link RequestId} returned by {@link PurchasingService#getPurchaseUpdates(boolean) } doesn't match
     * the one from {@link PurchasingListener#onPurchaseUpdatesResponse(PurchaseUpdatesResponse)}, we'll just
     * assume separate requests are equal and use simple queue for synchronization
     */
    private final Queue<CountDownLatch> inventoryLatchQueue = new ConcurrentLinkedQueue<CountDownLatch>();

    /**
     * If not null will be notified from
     */
    @Nullable
    private IabHelper.OnIabSetupFinishedListener setupListener;


    public AmazonAppstoreBillingService(@NotNull Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * @param listener - is triggered when {@link #onUserDataResponse(UserDataResponse)} happens
     */
    @Override
    public void startSetup(IabHelper.OnIabSetupFinishedListener listener) {
        this.setupListener = listener;
        PurchasingService.registerListener(context, this);
        PurchasingService.getUserData();
    }

    @Override
    public void onUserDataResponse(final UserDataResponse userDataResponse) {
        Logger.d("onUserDataResponse() reqId: ", userDataResponse.getRequestId(),
                ", status: ", userDataResponse.getRequestStatus());

        final IabResult iabResult;
        switch (userDataResponse.getRequestStatus()) {
            case SUCCESSFUL:
                final UserData userData = userDataResponse.getUserData();
                final String userId = userData.getUserId();
                this.currentUserId = userId;
                iabResult = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, "Setup successful.");
                Logger.d("Set current userId: ", userId);
                break;
            case FAILED:
                // Fall through
            case NOT_SUPPORTED:
                iabResult = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ERROR, "Unable to get userId");
                Logger.d("onUserDataResponse() Unable to get user ID");
                break;
            default:
                iabResult = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE, "Unknown response code");
        }
        if (setupListener != null) {
            setupListener.onIabSetupFinished(iabResult);
            setupListener = null;
        }
    }

    @Override
    public Inventory queryInventory(boolean querySkuDetails, @Nullable List<String> moreItemSkus, @Nullable List<String> moreSubsSkus) {
        Logger.d("queryInventory() querySkuDetails: ", querySkuDetails, " moreItemSkus: ",
                moreItemSkus, " moreSubsSkus: ", moreSubsSkus);

        final CountDownLatch purchaseUpdatesLatch = new CountDownLatch(1);
        inventoryLatchQueue.offer(purchaseUpdatesLatch);
        PurchasingService.getPurchaseUpdates(true);
        try {
            purchaseUpdatesLatch.await();
        } catch (InterruptedException e) {
            Logger.e("queryInventory() await interrupted");
            return null;
        }

        if (querySkuDetails) {
            final Set<String> querySkus = new HashSet<String>(inventory.getAllOwnedSkus());
            if (moreItemSkus != null) {
                querySkus.addAll(moreItemSkus);
            }
            if (moreSubsSkus != null) {
                querySkus.addAll(moreSubsSkus);
            }
            if (!querySkus.isEmpty()) {
                final HashSet<String> queryStoreSkus = new HashSet<String>(querySkus.size());
                for (String sku : querySkus) {
                    queryStoreSkus.add(SkuManager.getInstance().getStoreSku(OpenIabHelper.NAME_AMAZON, sku));
                }
                final CountDownLatch productDataLatch = new CountDownLatch(1);
                inventoryLatchQueue.offer(productDataLatch);
                PurchasingService.getProductData(queryStoreSkus);
                try {
                    productDataLatch.await();
                } catch (InterruptedException e) {
                    Logger.w("queryInventory() SkuDetails fetching interrupted");
                    return null;
                }
            }
        }
        Logger.d("queryInventory() finished. Inventory size: ", inventory.getAllOwnedSkus().size());
        return inventory;
    }

    @Override
    public void onPurchaseUpdatesResponse(final PurchaseUpdatesResponse purchaseUpdatesResponse) {
        final PurchaseUpdatesResponse.RequestStatus requestStatus = purchaseUpdatesResponse.getRequestStatus();
        final RequestId requestId = purchaseUpdatesResponse.getRequestId();
        Logger.d("onPurchaseUpdatesResponse() reqStatus: ", requestStatus,
                "reqId: ", requestId);

        switch (requestStatus) {
            case SUCCESSFUL:
                for (final String sku : inventory.getAllOwnedSkus()) {
                    inventory.erasePurchase(sku);
                }
                final UserData userData = purchaseUpdatesResponse.getUserData();
                final String userId = userData.getUserId();
                if (!userId.equals(currentUserId)) {
                    Logger.w("onPurchaseUpdatesResponse() Current UserId: ", currentUserId,
                            ", purchase UserId: ", userId);
                    break;
                }
                for (final Receipt receipt : purchaseUpdatesResponse.getReceipts()) {
                    inventory.addPurchase(getPurchase(receipt));
                }
                if (purchaseUpdatesResponse.hasMore()) {
                    PurchasingService.getPurchaseUpdates(false);
                    Logger.d("Initiating Another Purchase Updates with offset: ");
                    return;
                }
                break;
            case FAILED:
                break;
        }
        final CountDownLatch countDownLatch = inventoryLatchQueue.poll();
        if (countDownLatch != null) {
            countDownLatch.countDown();
        }
    }

    @NotNull
    private Purchase getPurchase(@NotNull final Receipt receipt) {
        final String storeSku = receipt.getSku();

        final Purchase purchase = new Purchase(OpenIabHelper.NAME_AMAZON);
        purchase.setSku(SkuManager.getInstance().getSku(OpenIabHelper.NAME_AMAZON, storeSku));

        switch (receipt.getProductType()) {
            case CONSUMABLE:
                // TODO Make sure this behavior is intended
            case ENTITLED:
                purchase.setItemType(IabHelper.ITEM_TYPE_INAPP);
                Logger.d("Add to inventory SKU: ", storeSku);
                break;
            case SUBSCRIPTION:
                // TODO Make sure cancelDate is always available
                purchase.setItemType(IabHelper.ITEM_TYPE_SUBS);
                purchase.setSku(SkuManager.getInstance().getSku(OpenIabHelper.NAME_AMAZON, storeSku));
                Logger.d("Add subscription to inventory SKU: ", storeSku);
                break;
        }
        return purchase;
    }

    @Override
    public void onProductDataResponse(@NotNull final ProductDataResponse productDataResponse) {
        final ProductDataResponse.RequestStatus status = productDataResponse.getRequestStatus();
        final RequestId requestId = productDataResponse.getRequestId();
        Logger.d("onItemDataResponse() reqStatus: ", status,
                ", reqId: ", requestId);

        switch (status) {
            case SUCCESSFUL:
                final Map<String, Product> productData = productDataResponse.getProductData();
                for (final String key : productData.keySet()) {
                    final Product product = productData.get(key);
                    inventory.addSkuDetails(getSkuDetails(product));
                }
                break;
            case FAILED:
                // Fall through
            case NOT_SUPPORTED:
                break;
        }
        final CountDownLatch countDownLatch = inventoryLatchQueue.poll();
        if (countDownLatch != null) {
            countDownLatch.countDown();
        }
    }

    @NotNull
    private SkuDetails getSkuDetails(@NotNull final Product product) {
        final String sku = product.getSku();
        final String price = product.getPrice().toString();
        final String title = product.getTitle();
        final String description = product.getDescription();
        final ProductType productType = product.getProductType();
        Logger.d(String.format("Item: %s\n Type: %s\n SKU: %s\n Price: %s\n Description: %s\n",
                title, productType, sku, price, description));

        final String openIabSkuType = productType == ProductType.SUBSCRIPTION
                ? IabHelper.ITEM_TYPE_SUBS
                : IabHelper.ITEM_TYPE_INAPP;
        final String openIabSku = SkuManager.getInstance().getSku(OpenIabHelper.NAME_AMAZON, sku);
        return new SkuDetails(openIabSkuType, openIabSku, title, price, description);
    }

    /**
     * As for Amazon IAP 2.0, {@link Receipt#getSku()} differs from requested one for subscription.
     * <br>
     * This map is intended to workaround this issue.
     */
    private final Map<RequestId, String> requestSkuMap = new HashMap<RequestId, String>();

    @Override
    public void launchPurchaseFlow(
            final Activity activity,
            final String sku,
            final String itemType,
            final int requestCode,
            final IabHelper.OnIabPurchaseFinishedListener listener,
            final String extraData) {
        final RequestId requestId = PurchasingService.purchase(sku);
        requestSkuMap.put(requestId, sku);
        requestListeners.put(requestId, listener);
    }

    @Override
    public void onPurchaseResponse(@NotNull final PurchaseResponse purchaseResponse) {
        final PurchaseResponse.RequestStatus status = purchaseResponse.getRequestStatus();
        final RequestId requestId = purchaseResponse.getRequestId();
        Logger.d("onPurchaseResponse() PurchaseRequestStatus:", status,
                ", reqId: ", requestId);

        final String requestSku = requestSkuMap.remove(requestId);
        final Purchase purchase = new Purchase(OpenIabHelper.NAME_AMAZON);
        final IabResult result;
        boolean shouldNotifyFulfillment = false;
        switch (status) {
            case SUCCESSFUL:
                final UserData userData = purchaseResponse.getUserData();
                final String userId = userData.getUserId();
                if (!userId.equals(currentUserId)) {
                    Logger.w("onPurchaseResponse() Current UserId: ", currentUserId,
                            ", purchase UserId: ", userId);
                    result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ERROR,
                            "Current UserId doesn't match purchase UserId");
                    break;
                }

                purchase.setOriginalJson(generateOriginalJson(purchaseResponse));

                purchase.setOrderId(requestId.toString());

                final Receipt receipt = purchaseResponse.getReceipt();
                final ProductType productType = receipt.getProductType();

                final String storeSku = receipt.getSku();
                final String sku = SkuManager.getInstance().getSku(OpenIabHelper.NAME_AMAZON,
                        productType == ProductType.SUBSCRIPTION ? requestSku : storeSku
                );
                purchase.setSku(sku);

                final String openIabSkuType = productType == ProductType.SUBSCRIPTION
                        ? IabHelper.ITEM_TYPE_SUBS
                        : IabHelper.ITEM_TYPE_INAPP;
                purchase.setItemType(openIabSkuType);

                result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, "Success");

                shouldNotifyFulfillment = true;
                break;
            case INVALID_SKU:
                result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE, "Invalid SKU");
                break;
            case ALREADY_PURCHASED:
                result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED, "Item is already purchased");
                break;
            case FAILED:
                result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ERROR, "Purchase failed");
                break;
            case NOT_SUPPORTED:
                result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE, "This call is not supported");
                break;
            default:
                result = null;
        }
        final IabHelper.OnIabPurchaseFinishedListener listener = requestListeners.remove(requestId);
        if (listener != null) {
            listener.onIabPurchaseFinished(result, purchase);
            if (shouldNotifyFulfillment) {
                final Receipt receipt = purchaseResponse.getReceipt();
                PurchasingService.notifyFulfillment(receipt.getReceiptId(), FulfillmentResult.FULFILLED);
            }
        } else {
            Logger.e("Something went wrong: PurchaseFinishedListener is not found");
        }
    }

    /**
     * Converts purchase response to json for transfer with purchase object
     * <p/>
     * <pre>
     * {
     * "orderId"           : "purchaseResponse.getRequestId"
     * "productId"         : "receipt.getSku"
     * "purchaseStatus"    : "purchaseRequestStatus.name"
     * "userId"            : "purchaseResponse.getUserId()" // if non-null
     * "itemType"          : "receipt.getItemType().name()" // if non-null
     * "purchaseToken"     : "receipt.getReceiptId()"
     * } </pre>
     *
     * @param purchaseResponse Purchase to convert.
     * @return Generate JSON from purchase.
     */
    private String generateOriginalJson(@NotNull PurchaseResponse purchaseResponse) {
        final JSONObject json = new JSONObject();
        try {
            Receipt receipt = purchaseResponse.getReceipt();
            json.put(JSON_KEY_ORDER_ID, purchaseResponse.getRequestId());
            json.put(JSON_KEY_PRODUCT_ID, receipt.getSku());
            final PurchaseResponse.RequestStatus requestStatus = purchaseResponse.getRequestStatus();
            if (requestStatus != null) {
                json.put(JSON_KEY_PURCHASE_STATUS, requestStatus.name());
            }
            final UserData userData = purchaseResponse.getUserData();
            if (userData != null) {
                json.put(JSON_KEY_USER_ID, userData.getUserId());
            }
            final ProductType productType = receipt.getProductType();
            if (productType != null) {
                json.put(JSON_KEY_RECEIPT_ITEM_TYPE, productType.name());
            }
            json.put(JSON_KEY_RECEIPT_PURCHASE_TOKEN, receipt.getReceiptId());
            Logger.d("generateOriginalJson(): JSON\n", json);
        } catch (JSONException e) {
            Logger.e("generateOriginalJson() failed to generate JSON", e);
        }
        return json.toString();
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
        setupListener = null;
    }

    @Override
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return false;
    }
}
