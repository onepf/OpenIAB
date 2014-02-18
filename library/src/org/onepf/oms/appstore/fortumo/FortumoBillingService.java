package org.onepf.oms.appstore.fortumo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import mp.MpUtils;
import mp.PaymentRequest;
import mp.PaymentResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onepf.oms.AppstoreInAppBillingService;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.*;
import org.onepf.oms.appstore.onepfUtils.BaseInappProduct;
import org.onepf.oms.appstore.onepfUtils.InappsXMLParser;
import org.onepf.oms.appstore.onepfUtils.SubscriptionInappProduct;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by akarimova on 23.12.13.
 */
public class FortumoBillingService implements AppstoreInAppBillingService {
    private Context context;
    private int activityRequestCode;
    private IabHelper.OnIabPurchaseFinishedListener purchaseFinishedListener;
    private Map<String, FortumoProduct> fortumoInapps;

    public FortumoBillingService(Context context) {
        this.context = context;
    }

    @Override
    public void startSetup(IabHelper.OnIabSetupFinishedListener listener) {
        if (!MpUtils.isPaymentBroadcastEnabled(context)) {
            try {
                Class permissionClass = Class.forName(context.getPackageName() + ".Manifest$permission");
                Field paymentBroadcastPermission = permissionClass.getField("PAYMENT_BROADCAST_PERMISSION");
                String permissionString = (String) paymentBroadcastPermission.get(null);
                MpUtils.enablePaymentBroadcast(context, permissionString);
            } catch (Exception ignored) {
                listener.onIabSetupFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ERROR, "PAYMENT_BROADCAST_PERMISSION is NOT declared."));
                return;
            }
        }
        try {
            fortumoInapps = FortumoBillingService.getFortumoInapps(context);
        } catch (IOException e) {
            listener.onIabSetupFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ERROR, "Parsing error."));
            return;
        } catch (XmlPullParserException e) {
            listener.onIabSetupFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ERROR, "Parsing error."));
            return;
        }
        listener.onIabSetupFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, "Fortumo billing service is ok")); //todo redo
    }

    @Override
    public void launchPurchaseFlow(final Activity act, String sku, String itemType, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        this.purchaseFinishedListener = listener;
        final FortumoProduct fortumoProduct = fortumoInapps.get("product_id1");
        //todo check for null
        this.activityRequestCode = requestCode;
        PaymentRequest paymentRequest = new PaymentRequest.PaymentRequestBuilder().setService(fortumoProduct.getServiceId(), fortumoProduct.getInAppSecret()).
                setConsumable(fortumoProduct.isConsumable()).
                setProductName(fortumoProduct.getProductId()).
                setDisplayString(fortumoProduct.getTitleByLocale(Locale.getDefault().toString())).
                build();
        FortumoStore.startPaymentActivityForResult(act, requestCode, paymentRequest);

//        }
    }

    @Override
    public boolean handleActivityResult(int requestCode, int resultCode, Intent intent) {
        if (activityRequestCode != requestCode) return false;
        if (intent == null) {
            if (purchaseFinishedListener != null) {
                purchaseFinishedListener.onIabPurchaseFinished(new IabResult(IabHelper.IABHELPER_BAD_RESPONSE, "Null data in Fortumo IAB result"), null);
            }
        }
        PaymentResponse paymentResponse = new PaymentResponse(intent);
        Purchase purchase = purchaseFromPaymentResponse(context, paymentResponse);
        int errorCode = IabHelper.BILLING_RESPONSE_RESULT_ERROR;
        String errorMsg = "";
        if (resultCode == Activity.RESULT_OK) {
            if (paymentResponse.getBillingStatus() == MpUtils.MESSAGE_STATUS_BILLED) {
                errorCode = IabHelper.BILLING_RESPONSE_RESULT_OK;
            }
        }
        if (purchaseFinishedListener != null) {
            purchaseFinishedListener.onIabPurchaseFinished(new IabResult(errorCode, errorMsg), purchase);
        }
        return true;
    }

    @Override
    public Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus, List<String> moreSubsSkus) throws IabException {
        Inventory inventory = new Inventory();
//        SharedPreferences sharedPreferences = context.getSharedPreferences(FortumoStore.SHARED_PREFS_FORTUMO, Context.MODE_PRIVATE);
//
//        String messageId = sharedPreferences.getString(FortumoStore.SHARED_PREFS_PAYMENT_TO_HANDLE, "");
//        if (!TextUtils.isEmpty(messageId) && !messageId.equals("-1")) {
//            PaymentResponse paymentResponse = MpUtils.getPaymentResponse(context, Long.valueOf(messageId));
//            if (paymentResponse != null) {
//                Purchase purchase = purchaseFromPaymentResponse(context, paymentResponse);
////                if (isItemConsumable(paymentResponse)) {
////                    addConsumableItem(purchase);
////                }
//                if (true) {
//                    addConsumableItem(purchase);
//                }
//                SharedPreferences.Editor editor = sharedPreferences.edit();
//                editor.remove(FortumoStore.SHARED_PREFS_PAYMENT_TO_HANDLE);
//                editor.commit();
//            }
//        }
//
//        //Non-consumable items from Fortumo
//        List<String> allFortumoSkus = OpenIabHelper.getAllStoreSkus(OpenIabHelper.NAME_FORTUMO);
//        for (String sku : allFortumoSkus) {
//            boolean consumable = FortumoStore.isSkuConsumable(sku);
//            String skuName = FortumoStore.getSkuName(sku);
//            if (!consumable) {
//                List purchaseHistory = MpUtils.getPurchaseHistory(context, FortumoStore.getSkuServiceId(sku), FortumoStore.getSkuAppSecret(sku), 5000);
//                for (int i = 0; i < purchaseHistory.size(); i++) {
//                    PaymentResponse paymentResponse = (PaymentResponse) purchaseHistory.get(i);
//                    if (paymentResponse.getProductName().equals(skuName)) {
//                        inventory.addPurchase(purchaseFromPaymentResponse(context, paymentResponse));
//                        break;
//                    }
//                }
//            }
//        }
//
//        //Consumable items from shared prefs
//        ArrayList<Purchase> consumableSkus = getConsumablePurchases();
//        for (Purchase purchase : consumableSkus) {
//            inventory.addPurchase(purchase);
//        }

//        return inventory;
        return inventory;
    }

    @Override
    public void consume(Purchase itemInfo) throws IabException {
        //do nothing
    }

    @Override
    public void dispose() {
        //do nothing
    }

    /**
     * Returns consumable purchases from shared preferences
     */
    private ArrayList<Purchase> getConsumablePurchases() {
        ArrayList<Purchase> purchases = new ArrayList<Purchase>();
        SharedPreferences sharedPreferences = context.getSharedPreferences(FortumoStore.SHARED_PREFS_FORTUMO, Context.MODE_PRIVATE);
        String consumableSkuString = sharedPreferences.getString(FortumoStore.SHARED_PREFS_FORTUMO_CONSUMABLE_SKUS, "");
        try {
            JSONArray jsonArray;
            if (TextUtils.isEmpty(consumableSkuString)) {
                jsonArray = new JSONArray();
            } else {
                jsonArray = new JSONArray(consumableSkuString);
            }
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = (JSONObject) jsonArray.get(i);
                purchases.add(convertJsonToPurchase(context, object));
            }
        } catch (JSONException e) {
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.remove(FortumoStore.SHARED_PREFS_FORTUMO_CONSUMABLE_SKUS);
            edit.commit();
        }
        return purchases;
    }

    private static Purchase purchaseFromPaymentResponse(Context context, PaymentResponse paymentResponse) {
        Purchase purchase = new Purchase(OpenIabHelper.NAME_FORTUMO);
        purchase.setSku(paymentResponse.getProductName());
        purchase.setPackageName(context.getPackageName());
        String openFortumoSku = OpenIabHelper.getStoreSku(OpenIabHelper.NAME_FORTUMO, paymentResponse.getProductName());
//        purchase.setItemType(FortumoStore.getSkuType(openFortumoSku));
        purchase.setOrderId(paymentResponse.getPaymentCode());
        Date date = paymentResponse.getDate();
        if (date != null) {
            purchase.setPurchaseTime(date.getTime());
        }
        return purchase;
    }

    private static Purchase convertJsonToPurchase(Context context, JSONObject object) throws JSONException {
        Purchase purchase = new Purchase(OpenIabHelper.NAME_FORTUMO);
        purchase.setSku(object.getString("sku"));
        purchase.setOrderId(object.getString("order_id"));
        purchase.setPackageName(context.getPackageName());
        String fortumoSku = OpenIabHelper.getStoreSku(OpenIabHelper.NAME_FORTUMO, object.getString("sku"));
//        purchase.setItemType(FortumoStore.getSkuType(fortumoSku));
        return purchase;
    }


//    private static boolean isItemConsumable(PaymentResponse paymentResponse) {
//        boolean consumable = false;
//        List<String> allStoreSkus = OpenIabHelper.getAllStoreSkus(OpenIabHelper.NAME_FORTUMO);
//        for (String fortumoSku : allStoreSkus) {
//            if (FortumoStore.getSkuName(fortumoSku).equals(paymentResponse.getProductName())) {
//                consumable = FortumoStore.isSkuConsumable(fortumoSku);
//                break;
//            }
//        }
//        return consumable;
//    }

    public static Map<String, FortumoProduct> getFortumoInapps(Context context) throws IOException, XmlPullParserException {
        Map<String, FortumoProduct> map = new HashMap<String, FortumoProduct>();
        //get all products
        InappsXMLParser inappsXMLParser = new InappsXMLParser();
        final List<BaseInappProduct> allProducts = inappsXMLParser.parse(context);
        //get fortumo sku details
        final FortumoDetailsXMLParser fortumoDetailsXMLParser = new FortumoDetailsXMLParser();
        final Map<String, FortumoSku> fortumoSkuDetailsMap = fortumoDetailsXMLParser.parse(context);

        for (BaseInappProduct inappProduct : allProducts) {
            final boolean isItem = !(inappProduct instanceof SubscriptionInappProduct);
            if (isItem) {
                final String productId = inappProduct.getProductId();
                final FortumoSku fortumoSku = fortumoSkuDetailsMap.get(productId);
                if (fortumoSku == null) {
                    throw new IllegalStateException("Fortumo sku details were not found");
                }
                FortumoProduct fortumoProduct = new FortumoProduct(inappProduct, fortumoSku.isConsumable(), fortumoSku.getServiceId(), fortumoSku.getServiceInAppSecret());
                map.put(productId, fortumoProduct);
            }
        }
        return map;
    }
}
