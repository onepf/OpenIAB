package com.openiab;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import com.unity3d.player.UnityPlayer;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.*;

import java.util.HashMap;
import java.util.Map;

public class OpenIAB {

    public static final String TAG = "OpenIAB-unity-plugin";
    private static final String EVENT_MANAGER = "OpenIABEventManager";
    private static final String BILLING_SUPPORTED_CALLBACK = "OnBillingSupported";
    private static final String BILLING_NOT_SUPPORTED_CALLBACK = "OnBillingNotSupported";
    private static final String QUERY_INVENTORY_SUCCEEDED_CALLBACK = "OnQueryInventorySucceeded";
    private static final String QUERY_INVENTORY_FAILED_CALLBACK = "OnQueryInventoryFailed";
    private static final String PURCHASE_SUCCEEDED_CALLBACK = "OnPurchaseSucceeded";
    private static final String PURCHASE_FAILED_CALLBACK = "OnPurchaseFailed";
    private static final String CONSUME_PURCHASE_SUCCEEDED_CALLBACK = "OnConsumePurchaseSucceeded";
    private static final String CONSUME_PURCHASE_FAILED_CALLBACK = "OnConsumePurchaseFailed";

    public static final String STORE_GOOGLE = OpenIabHelper.NAME_GOOGLE;
    public static final String STORE_AMAZON = OpenIabHelper.NAME_AMAZON;
    public static final String STORE_TSTORE = OpenIabHelper.NAME_TSTORE;
    public static final String STORE_SAMSUNG = OpenIabHelper.NAME_SAMSUNG;
    public static final String STORE_YANDEX = "YandexPublicKey";

    // (arbitrary) request code for the purchase flow
    private static final int RC_REQUEST = 10001;

    private static OpenIAB _instance;
    private OpenIabHelper _helper;

    public OpenIabHelper getHelper() {
        return _helper;
    }

    public static OpenIAB instance() {
        if (_instance == null) {
            _instance = new OpenIAB();
        }
        return _instance;
    }

    public void mapSku(String sku, String storeName, String storeSku)  {
        OpenIabHelper.mapSku(sku, storeName, storeSku);
    }

    public void init(HashMap<String, String> storeKeys) {
        _helper = new OpenIabHelper(UnityPlayer.currentActivity, storeKeys);
        createBroadcasts();

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        Log.d(TAG, "Starting setup.");
        _helper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (result.isFailure()) {
                    // Oh noes, there was a problem.
                    Log.e(TAG, "Problem setting up in-app billing: " + result);
                    UnityPlayer.UnitySendMessage(EVENT_MANAGER, BILLING_NOT_SUPPORTED_CALLBACK, result.getMessage());
                    return;
                }

                // Hooray, IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful.");
                UnityPlayer.UnitySendMessage(EVENT_MANAGER, BILLING_SUPPORTED_CALLBACK, "");
            }
        });
    }

    public void unbindService() {
        if (_helper != null) {
            _helper.dispose();
            _helper = null;
        }
        destroyBroadcasts();
    }

    public boolean areSubscriptionsSupported() {
        return _helper.subscriptionsSupported();
    }

    public void queryInventory() {
        UnityPlayer.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _helper.queryInventoryAsync(_queryInventoryListener);
            }
        });
    }

    public void queryInventory(String[] skus) {
        Log.i(TAG, skus.length + "");
        _helper.queryInventoryAsync(_queryInventoryListener);
    }

    public void purchaseProduct(final String sku, final String developerPayload) {
        try {
            _helper.launchPurchaseFlow(UnityPlayer.currentActivity, sku, RC_REQUEST,
                    _purchaseFinishedListener, developerPayload);
        } catch (java.lang.IllegalStateException e) {
            _purchaseFinishedListener.onIabPurchaseFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE, "Cannot start product purchase. Another operation is in progress."), null);
            return;
        }
        Log.i("OpenIAB", "Starting product purchase.");
    }

    public void purchaseSubscription(final String sku, final String developerPayload) {
        try {
            _helper.launchSubscriptionPurchaseFlow(UnityPlayer.currentActivity, sku, RC_REQUEST, _purchaseFinishedListener, developerPayload);
        } catch (java.lang.IllegalStateException e) {
            _purchaseFinishedListener.onIabPurchaseFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE, "Cannot start subscription purchase. Another operation is in progress."), null);
            return;
        }
        Log.i("OpenIAB", "Starting subscription purchase.");
    }

    public void consumeProduct(final String json) {
        UnityPlayer.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    String appstoreName = jsonObject.getString("appstoreName");
                    String jsonPurchaseInfo = jsonObject.getString("originalJson");
                    Purchase p;
                    if (jsonPurchaseInfo == null || jsonPurchaseInfo.equals("")) {
                        p = new Purchase(appstoreName);
                        p.setSku(jsonObject.getString("sku"));
                    } else {
                        String itemType = jsonObject.getString("itemType");
                        String signature = jsonObject.getString("signature");
                        p = new Purchase(itemType, jsonPurchaseInfo, signature, appstoreName);
                    }
                    _helper.consumeAsync(p, _consumeFinishedListener);
                    //Log.i(TAG, "CONSUME JSON: " + json);
                } catch (org.json.JSONException e) {
                    UnityPlayer.UnitySendMessage(EVENT_MANAGER, CONSUME_PURCHASE_FAILED_CALLBACK, "Invalid json: " + json + ". " + e);
                }
            }
        });
    }

    // TODO: implement
    public void consumeProducts(String[] sku) {
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener _queryInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");
            if (result.isFailure()) {
                UnityPlayer.UnitySendMessage(EVENT_MANAGER, QUERY_INVENTORY_FAILED_CALLBACK, result.getMessage());
                return;
            }

            Log.d(TAG, "Query inventory was successful.");
            String jsonInventory;
            try {
                jsonInventory = inventoryToJson(inventory);
            } catch (JSONException e) {
                UnityPlayer.UnitySendMessage(EVENT_MANAGER, QUERY_INVENTORY_FAILED_CALLBACK, "Couldn't serialize the inventory");
                return;
            }
            UnityPlayer.UnitySendMessage(EVENT_MANAGER, QUERY_INVENTORY_SUCCEEDED_CALLBACK, jsonInventory);
        }
    };

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener _purchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
            if (result.isFailure()) {
                Log.e(TAG, "Error purchasing: " + result);
                UnityPlayer.UnitySendMessage(EVENT_MANAGER, PURCHASE_FAILED_CALLBACK, result.getMessage());
                return;
            }
            Log.d(TAG, "Purchase successful.");
            String jsonPurchase;
            try {
                jsonPurchase = purchaseToJson(purchase);
            } catch (JSONException e) {
                UnityPlayer.UnitySendMessage(EVENT_MANAGER, PURCHASE_FAILED_CALLBACK, "Couldn't serialize the purchase");
                return;
            }
            UnityPlayer.UnitySendMessage(EVENT_MANAGER, PURCHASE_SUCCEEDED_CALLBACK, jsonPurchase);
        }
    };

    // Callback for when a consumption is complete
    IabHelper.OnConsumeFinishedListener _consumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

            String sku = purchase.getSku();
            purchase.setSku(OpenIabHelper.getSku(purchase.getAppstoreName(), sku));

            if (result.isFailure()) {
                Log.e(TAG, "Error while consuming: " + result);
                UnityPlayer.UnitySendMessage(EVENT_MANAGER, CONSUME_PURCHASE_FAILED_CALLBACK, result.getMessage());
                return;
            }
            Log.d(TAG, "Consumption successful. Provisioning.");
            String jsonPurchase;
            try {
                jsonPurchase = purchaseToJson(purchase);
            } catch (JSONException e) {
                UnityPlayer.UnitySendMessage(EVENT_MANAGER, CONSUME_PURCHASE_FAILED_CALLBACK, "Couldn't serialize the purchase");
                return;
            }
            UnityPlayer.UnitySendMessage(EVENT_MANAGER, CONSUME_PURCHASE_SUCCEEDED_CALLBACK, jsonPurchase);
        }
    };


    private String inventoryToJson(Inventory inventory) throws JSONException {
        JSONStringer json = new JSONStringer().object();

        json.key("purchaseMap").array();
        for (Map.Entry<String, Purchase> entry : inventory.mPurchaseMap.entrySet()) {
            json.array();
            json.value(entry.getKey());
            json.value(purchaseToJson(entry.getValue()));
            json.endArray();
        }
        json.endArray();

        json.key("skuMap").array();
        for (Map.Entry<String, SkuDetails> entry : inventory.mSkuMap.entrySet()) {
            json.array();
            json.value(entry.getKey());
            json.value(skuDetailsToJson(entry.getValue()));
            json.endArray();
        }
        json.endArray();

        json.endObject();
        return json.toString();
    }

    private String purchaseToJson(Purchase purchase) throws JSONException {
        return new JSONStringer().object()
                .key("itemType").value(purchase.getItemType())
                .key("orderId").value(purchase.getOrderId())
                .key("packageName").value(purchase.getPackageName())
                .key("sku").value(purchase.getSku())
                .key("purchaseTime").value(purchase.getPurchaseTime())
                .key("purchaseState").value(purchase.getPurchaseState())
                .key("developerPayload").value(purchase.getDeveloperPayload())
                .key("token").value(purchase.getToken())
                .key("originalJson").value(purchase.getOriginalJson())
                .key("signature").value(purchase.getSignature())
                .key("appstoreName").value(purchase.getAppstoreName())
                .endObject().toString();
    }

    private String skuDetailsToJson(SkuDetails skuDetails) throws JSONException {
        return new JSONStringer().object()
                .key("itemType").value(skuDetails.getItemType())
                .key("sku").value(skuDetails.getSku())
                .key("type").value(skuDetails.getType())
                .key("price").value(skuDetails.getPrice())
                .key("title").value(skuDetails.getTitle())
                .key("description").value(skuDetails.getDescription())
                .key("json").value(skuDetails.getJson())
                .endObject().toString();
    }

    private void createBroadcasts() {
        Log.d(TAG, "createBroadcasts");
        IntentFilter filter = new IntentFilter(YANDEX_STORE_ACTION_PURCHASE_STATE_CHANGED);
        UnityPlayer.currentActivity.registerReceiver(_billingReceiver, filter);
    }

    private void destroyBroadcasts() {
        Log.d(TAG, "destroyBroadcasts");
        try {
            UnityPlayer.currentActivity.unregisterReceiver(_billingReceiver);
        } catch (Exception ex) {
            Log.d(TAG, "destroyBroadcasts exception:\n" + ex.getMessage());
        }
    }

    // Yandex specific
    public static final String YANDEX_STORE_SERVICE = "com.yandex.store.service";
    public static final String YANDEX_STORE_ACTION_PURCHASE_STATE_CHANGED = YANDEX_STORE_SERVICE + ".PURCHASE_STATE_CHANGED";

    private BroadcastReceiver _billingReceiver = new BroadcastReceiver() {
        private static final String TAG = "YandexBillingReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive intent: " + intent);

            if (YANDEX_STORE_ACTION_PURCHASE_STATE_CHANGED.equals(action)) {
                purchaseStateChanged(intent);
            }
        }

        private void purchaseStateChanged(Intent data) {
            Log.d(TAG, "purchaseStateChanged intent: " + data);
            _helper.handleActivityResult(RC_REQUEST, Activity.RESULT_OK, data);
        }
    };
}
