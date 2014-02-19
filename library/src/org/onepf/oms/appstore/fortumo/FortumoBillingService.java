package org.onepf.oms.appstore.fortumo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import mp.MpUtils;
import mp.PaymentRequest;
import mp.PaymentResponse;
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
        IabResult result = null;
        if (!MpUtils.isPaymentBroadcastEnabled(context)) {
            try {
                Class permissionClass = Class.forName(context.getPackageName() + ".Manifest$permission");
                Field paymentBroadcastPermission = permissionClass.getField("PAYMENT_BROADCAST_PERMISSION");
                String permissionString = (String) paymentBroadcastPermission.get(null);
                MpUtils.enablePaymentBroadcast(context, permissionString);
            } catch (Exception ignored) {
                result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ERROR, "PAYMENT_BROADCAST_PERMISSION is NOT declared.");
            }
        }
        if (result == null) {
            try {
                fortumoInapps = FortumoBillingService.getFortumoInapps(context);
            } catch (IOException e) {
                result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ERROR, "Parsing error.");
            } catch (XmlPullParserException e) {
                result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ERROR, "Parsing error.");
            }
        }
        listener.onIabSetupFinished(result != null ? result : new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, "Fortumo billing service is ok"));
    }

    @Override
    public void launchPurchaseFlow(final Activity act, String sku, String itemType, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        this.purchaseFinishedListener = listener;
        this.activityRequestCode = requestCode;
        final FortumoProduct fortumoProduct = fortumoInapps.get(sku);
        if (fortumoProduct == null) {
            purchaseFinishedListener.onIabPurchaseFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_DEVELOPER_ERROR, "Required sku " + sku + " was not declared in xml files."), null);
        } else {
            PaymentRequest paymentRequest = new PaymentRequest.PaymentRequestBuilder().setService(fortumoProduct.getServiceId(), fortumoProduct.getInAppSecret()).
                    setConsumable(fortumoProduct.isConsumable()).
                    setProductName(fortumoProduct.getProductId()).
                    setDisplayString(fortumoProduct.getTitle()).
                    build();
            FortumoStore.startPaymentActivityForResult(act, requestCode, paymentRequest);
        }
    }

    @Override
    public boolean handleActivityResult(int requestCode, int resultCode, Intent intent) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(FortumoStore.SHARED_PREFS_FORTUMO, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(FortumoStore.SHARED_PREFS_PAYMENT_TO_HANDLE);
        editor.commit();
        if (activityRequestCode != requestCode) return false;
        if (intent == null) {
            purchaseFinishedListener.onIabPurchaseFinished(new IabResult(IabHelper.IABHELPER_BAD_RESPONSE, "Null data in Fortumo IAB result"), null);
        }
        PaymentResponse paymentResponse = new PaymentResponse(intent);
        Purchase purchase = purchaseFromPaymentResponse(context, paymentResponse);
        int errorCode = IabHelper.BILLING_RESPONSE_RESULT_ERROR;
        String errorMsg = "Error during purchase.";
        if (resultCode == Activity.RESULT_OK) {
            if (paymentResponse.getBillingStatus() == MpUtils.MESSAGE_STATUS_BILLED) {
                errorCode = IabHelper.BILLING_RESPONSE_RESULT_OK;
            }
        }
        purchaseFinishedListener.onIabPurchaseFinished(new IabResult(errorCode, errorMsg), purchase);
        return true;
    }

    @Override
    public Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus, List<String> moreSubsSkus) throws IabException {
        Inventory inventory = new Inventory();
        SharedPreferences sharedPreferences = context.getSharedPreferences(FortumoStore.SHARED_PREFS_FORTUMO, Context.MODE_PRIVATE);
        String messageId = sharedPreferences.getString(FortumoStore.SHARED_PREFS_PAYMENT_TO_HANDLE, "");
        if (!TextUtils.isEmpty(messageId) && !messageId.equals("-1")) {
            PaymentResponse paymentResponse = MpUtils.getPaymentResponse(context, Long.valueOf(messageId));
            if (paymentResponse != null) {
                Purchase purchase = purchaseFromPaymentResponse(context, paymentResponse);
                if (purchase.getItemType().equals(OpenIabHelper.ITEM_TYPE_SUBS)) {
                    //todo an exception
                }
                final FortumoProduct fortumoProduct = fortumoInapps.get(purchase.getSku());
                if (fortumoProduct != null && fortumoProduct.isConsumable()) {
                    inventory.addPurchase(purchase);
                }
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove(FortumoStore.SHARED_PREFS_PAYMENT_TO_HANDLE);
                editor.commit();
            }
        }

        final Collection<FortumoProduct> inappProducts = fortumoInapps.values();
        for (FortumoProduct fortumoProduct : inappProducts) {
            if (!fortumoProduct.isConsumable()) {
                final List purchaseHistory = MpUtils.getPurchaseHistory(context, fortumoProduct.getServiceId(), fortumoProduct.getInAppSecret(), 5000); //todo what does the last param mean???
                if (purchaseHistory != null && purchaseHistory.size() > 0) {
                    for (Object response : purchaseHistory) {
                        PaymentResponse paymentResponse = (PaymentResponse) response;
                        if (paymentResponse.getProductName().equals(fortumoProduct.getProductId())) {
                            inventory.addPurchase(purchaseFromPaymentResponse(context, paymentResponse));
                            if (querySkuDetails) {
                                fortumoProduct.toSkuDetails(paymentResponse.getPriceAmount() + " " + paymentResponse.getPriceCurrency());
                            }
                            break;
                        }
                    }
                }
            }
        }

        if (querySkuDetails && moreItemSkus != null && moreItemSkus.size() > 0) {
            for (String name : moreItemSkus) {
                final FortumoProduct fortumoProduct = fortumoInapps.get(name);
                if (fortumoProduct != null) {
                    final List fetchedPriceData = MpUtils.getFetchedPriceData(context, fortumoProduct.getServiceId(), fortumoProduct.getInAppSecret());
                    if (fetchedPriceData != null && fetchedPriceData.size() > 0) {
                        inventory.addSkuDetails(fortumoProduct.toSkuDetails((String) fetchedPriceData.get(0)));
                    } else {
                        //todo throw an exception?
                    }
                } else {
                    //todo throw an exception?
                }
            }
        }
        return inventory;
    }

    @Override
    public void consume(Purchase itemInfo) throws IabException {
        //do nothing
    }

    @Override
    public void dispose() {
        purchaseFinishedListener = null;
    }


    private static Purchase purchaseFromPaymentResponse(Context context, PaymentResponse paymentResponse) {
        Purchase purchase = new Purchase(OpenIabHelper.NAME_FORTUMO);
        purchase.setSku(paymentResponse.getProductName());
        purchase.setPackageName(context.getPackageName()); //todo remove?
        purchase.setOrderId(paymentResponse.getPaymentCode());
        Date date = paymentResponse.getDate();
        if (date != null) {
            purchase.setPurchaseTime(date.getTime());
        }
        purchase.setItemType(OpenIabHelper.ITEM_TYPE_INAPP); //todo probably in the future subs will be supported
        return purchase;
    }

    public static Map<String, FortumoProduct> getFortumoInapps(Context context) throws IOException, XmlPullParserException {
        Map<String, FortumoProduct> map = new HashMap<String, FortumoProduct>();
        //get all products
        InappsXMLParser inappsXMLParser = new InappsXMLParser();
        final List<BaseInappProduct> allProducts = inappsXMLParser.parse(context);
        //get fortumo sku details
        final FortumoDetailsXMLParser fortumoDetailsXMLParser = new FortumoDetailsXMLParser();
        final Map<String, FortumoSku> fortumoSkuDetailsMap = fortumoDetailsXMLParser.parse(context);

        for (BaseInappProduct inappProduct : allProducts) {
            //Fortumo doesn't support subscriptions
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
