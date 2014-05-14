package org.onepf.oms.appstore;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import mp.MpUtils;
import mp.PaymentRequest;
import mp.PaymentResponse;
import org.onepf.oms.AppstoreInAppBillingService;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.fortumoUtils.InappBaseProduct;
import org.onepf.oms.appstore.fortumoUtils.InappSubscriptionProduct;
import org.onepf.oms.appstore.fortumoUtils.InappsXMLParser;
import org.onepf.oms.appstore.googleUtils.*;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author akarimova@onepf.org
 * @since 23.12.13
 */
public class FortumoBillingService implements AppstoreInAppBillingService {
    private static final String TAG = FortumoStore.class.getSimpleName();

    private static final String SHARED_PREFS_FORTUMO = "onepf_shared_prefs_fortumo";
    private boolean isNook;


    private static boolean isDebugLog() {
        return OpenIabHelper.isDebugLog();
    }

    private int activityRequestCode;
    private Context context;
    private Map<String, FortumoProduct> inappsMap;
    private IabHelper.OnIabPurchaseFinishedListener purchaseFinishedListener;
    private String developerPayload;

    public FortumoBillingService(Context context, boolean isNook) {
        this.context = context;
        this.isNook = isNook;
    }

    @Override
    public void startSetup(IabHelper.OnIabSetupFinishedListener listener) {
        final IabResult setupResult = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, "Fortumo: successful setup.");
        if (isDebugLog()) {
            Log.d(TAG, "Setup result: " + setupResult);
        }
        listener.onIabSetupFinished(setupResult);
    }

    @Override
    public void launchPurchaseFlow(final Activity act, String sku, String itemType, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        this.purchaseFinishedListener = listener;
        this.activityRequestCode = requestCode;
        this.developerPayload = extraData;
        final FortumoProduct fortumoProduct = inappsMap.get(sku);
        if (null == fortumoProduct) {
            if (isDebugLog()) {
                Log.d(TAG, String.format("launchPurchaseFlow: required sku %s was not defined", sku));
            }
            purchaseFinishedListener.onIabPurchaseFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_DEVELOPER_ERROR, String.format("Required product %s was not defined in xml files.", sku)), null);
        } else {
            final String messageId = getMessageIdInPending(context, fortumoProduct.getProductId());
            if (fortumoProduct.isConsumable() && !TextUtils.isEmpty(messageId) && !messageId.equals("-1")) {
                final PaymentResponse paymentResponse = MpUtils.getPaymentResponse(context, Long.valueOf(messageId));
                IabResult result;
                Purchase purchase = null;
                final int billingStatus = paymentResponse.getBillingStatus();
                if (billingStatus == MpUtils.MESSAGE_STATUS_BILLED) {
                    purchase = purchaseFromPaymentResponse(context, paymentResponse);
                    result = new IabResult(OpenIabHelper.BILLING_RESPONSE_RESULT_OK, "Purchase was successful.");
                    removePendingProduct(context, sku);
                } else if (billingStatus == MpUtils.MESSAGE_STATUS_FAILED || billingStatus == MpUtils.MESSAGE_STATUS_USE_ALTERNATIVE_METHOD) {
                    result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ERROR, "Purchase was failed.");
                    removePendingProduct(context, sku);
                } else {
                    result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ERROR, "Purchase is in pending.");
                }
                purchaseFinishedListener.onIabPurchaseFinished(result, purchase);
            } else {
                PaymentRequest paymentRequest = new PaymentRequest.PaymentRequestBuilder().setService(isNook ? fortumoProduct.getNookServiceId() : fortumoProduct.getServiceId(),
                        isNook ? fortumoProduct.getNookInAppSecret() : fortumoProduct.getInAppSecret()).
                        setConsumable(fortumoProduct.isConsumable()).
                        setProductName(fortumoProduct.getProductId()).
                        setDisplayString(fortumoProduct.getTitle()).
                        build();
                Intent localIntent = paymentRequest.toIntent(act);
                act.startActivityForResult(localIntent, requestCode);
            }
        }
    }

    @Override
    public boolean handleActivityResult(int requestCode, int resultCode, Intent intent) {
        if (activityRequestCode != requestCode) return false;
        if (intent == null) {
            if (isDebugLog()) {
                Log.d(TAG, "handleActivityResult: null intent data");
            }
            purchaseFinishedListener.onIabPurchaseFinished(new IabResult(IabHelper.IABHELPER_BAD_RESPONSE, "Null data in Fortumo IAB result"), null);
        } else {
            int errorCode = IabHelper.BILLING_RESPONSE_RESULT_ERROR;
            String errorMsg = "Purchase error.";
            Purchase purchase = null;
            if (resultCode == Activity.RESULT_OK) {
                PaymentResponse paymentResponse = new PaymentResponse(intent);
                purchase = purchaseFromPaymentResponse(context, paymentResponse);
                purchase.setDeveloperPayload(developerPayload);
                if (paymentResponse.getBillingStatus() == MpUtils.MESSAGE_STATUS_BILLED) {
                    errorCode = IabHelper.BILLING_RESPONSE_RESULT_OK;
                } else if (paymentResponse.getBillingStatus() == MpUtils.MESSAGE_STATUS_PENDING) {
                    if (isDebugLog()) {
                        Log.d(TAG, String.format("handleActivityResult: status pending for %s", paymentResponse.getProductName()));
                    }
                    errorCode = IabHelper.BILLING_RESPONSE_RESULT_ERROR;
                    errorMsg = "Purchase is pending";
                    if (inappsMap.get(paymentResponse.getProductName()).isConsumable()) {
                        addPendingPayment(context, paymentResponse.getProductName(), String.valueOf(paymentResponse.getMessageId()));
                        purchase = null;
                    }
                }
            }
            developerPayload = null;
            final IabResult result = new IabResult(errorCode, errorMsg);
            if (isDebugLog()) {
                Log.d(TAG, "handleActivityResult: " + result);
            }
            purchaseFinishedListener.onIabPurchaseFinished(result, purchase);
        }
        return true;
    }

    @Override
    public Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus, List<String> moreSubsSkus) throws IabException {
        Inventory inventory = new Inventory();
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_FORTUMO, Context.MODE_PRIVATE);
        final Map<String, ?> preferenceMap = sharedPreferences.getAll();
        if (preferenceMap != null) {
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            final Set<String> keySet = preferenceMap.keySet();
            for (String key : keySet) {
                final String value = (String) preferenceMap.get(key);
                if (value != null) {
                    final Long messageId = Long.valueOf(value);
                    final PaymentResponse paymentResponse = MpUtils.getPaymentResponse(context, messageId);
                    if (paymentResponse.getBillingStatus() == MpUtils.MESSAGE_STATUS_BILLED) {
                        Purchase purchase = purchaseFromPaymentResponse(context, paymentResponse);
                        inventory.addPurchase(purchase);
                    } else if (paymentResponse.getBillingStatus() == MpUtils.MESSAGE_STATUS_FAILED) {
                        editor.remove(key);
                    }
                } else {
                    preferenceMap.remove(key);
                }
            }
            editor.commit();
        }
        final Collection<FortumoProduct> inappProducts = inappsMap.values();
        for (FortumoProduct fortumoProduct : inappProducts) {
            if (!fortumoProduct.isConsumable()) {
                final List purchaseHistory = MpUtils.getPurchaseHistory(context, fortumoProduct.getServiceId(), fortumoProduct.getInAppSecret(), 5000);
                if (purchaseHistory != null && purchaseHistory.size() > 0) {
                    for (Object response : purchaseHistory) {
                        PaymentResponse paymentResponse = (PaymentResponse) response;
                        if (paymentResponse.getProductName().equals(fortumoProduct.getProductId())) {
                            inventory.addPurchase(purchaseFromPaymentResponse(context, paymentResponse));
                            if (querySkuDetails) {
                                String fortumoPrice = getSkuPrice(fortumoProduct);
                                inventory.addSkuDetails(fortumoProduct.toSkuDetails(fortumoPrice));
                            }
                            break;
                        }
                    }
                }
            }
        }

        if (querySkuDetails && moreItemSkus != null && moreItemSkus.size() > 0) {
            for (String name : moreItemSkus) {
                final FortumoProduct fortumoProduct = inappsMap.get(name);
                if (fortumoProduct != null) {
                    String fortumoPrice = getSkuPrice(fortumoProduct);
                    inventory.addSkuDetails(fortumoProduct.toSkuDetails(fortumoPrice));
                } else {
                    throw new IabException(IabHelper.BILLING_RESPONSE_RESULT_DEVELOPER_ERROR, String.format("Data %s not found", name));
                }
            }
        }
        return inventory;
    }

    private String getSkuPrice(FortumoProduct fortumoProduct) throws IabException {
        String fortumoPrice = fortumoProduct.getFortumoPrice();
        if (!TextUtils.isEmpty(fortumoPrice)) {
            final String serviceId = isNook ? fortumoProduct.getNookServiceId() : fortumoProduct.getServiceId();
            final String appSecret = isNook ? fortumoProduct.getNookInAppSecret() : fortumoProduct.getInAppSecret();
            MpUtils.fetchPaymentData(context, serviceId,
                    appSecret);
            final List fetchedPriceData = MpUtils.getFetchedPriceData(context, serviceId, appSecret);
            if (fetchedPriceData != null && !fetchedPriceData.isEmpty()) {
                fortumoPrice = (String) fetchedPriceData.get(0);
            }
        }
        return fortumoPrice;
    }

    @Override
    public void consume(Purchase itemInfo) throws IabException {
        removePendingProduct(context, itemInfo.getSku());
    }

    @Override
    public boolean subscriptionsSupported() {
        return false;
    }

    @Override
    public void dispose() {
        purchaseFinishedListener = null;
    }

    boolean setupBilling(boolean isNook) {
        try {
            inappsMap = FortumoBillingService.getFortumoInapps(context, isNook);
        } catch (Exception e) {
            if (isDebugLog()) {
                Log.d(TAG, "billing is not supported due to " + e.getMessage());
            }
            return false;
        }
        return true;
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
        purchase.setItemType(OpenIabHelper.ITEM_TYPE_INAPP);
        return purchase;
    }

    static Map<String, FortumoProduct> getFortumoInapps(Context context, boolean isNook) throws IOException, XmlPullParserException, IabException {
        final Map<String, FortumoProduct> map = new HashMap<String, FortumoProduct>();
        final InappsXMLParser inappsXMLParser = new InappsXMLParser();
        final Pair<List<InappBaseProduct>, List<InappSubscriptionProduct>> parse = inappsXMLParser.parse(context);
        final List<InappBaseProduct> allItems = parse.first;
        final Map<String, FortumoProductParser.FortumoDetails> fortumoSkuDetailsMap = FortumoProductParser.parse(context, isNook);
        for (InappBaseProduct item : allItems) {
            final String productId = item.getProductId();
            final FortumoProductParser.FortumoDetails fortumoDetails = fortumoSkuDetailsMap.get(productId);
            if (fortumoDetails == null) {
                throw new IabException(IabHelper.IABHELPER_ERROR_BASE, "Fortumo inapp product details were not found");
            }
            final String serviceId = isNook ? fortumoDetails.getNookServiceId() : fortumoDetails.getServiceId();
            final String serviceInAppSecret = isNook ? fortumoDetails.getNookInAppSecret() : fortumoDetails.getServiceInAppSecret();
            List fetchedPriceData = MpUtils.getFetchedPriceData(context, serviceId, serviceInAppSecret);
            if (fetchedPriceData == null || fetchedPriceData.size() == 0) {
                final boolean supportedOperator = MpUtils.isSupportedOperator(context, serviceId, serviceInAppSecret);
                if (supportedOperator) {
                    fetchedPriceData = MpUtils.getFetchedPriceData(context, serviceId, serviceInAppSecret);
                } else {
                    throw new IabException(IabHelper.IABHELPER_ERROR_BASE, "Carrier is not supported.");
                }
            }
            String price = null;
            if (fetchedPriceData != null && !fetchedPriceData.isEmpty()) {
                price = (String) fetchedPriceData.get(0);
            }
            FortumoProduct fortumoProduct = new FortumoProduct(item, fortumoDetails, price);
            map.put(productId, fortumoProduct);
        }
        return map;
    }


    static class FortumoProduct extends InappBaseProduct {
        private FortumoProductParser.FortumoDetails fortumoDetails;
        private String fortumoPrice;

        public FortumoProduct(InappBaseProduct otherProduct, FortumoProductParser.FortumoDetails fortumoDetails, String fortumoPrice) {
            super(otherProduct);
            this.fortumoDetails = fortumoDetails;
            this.fortumoPrice = fortumoPrice;
        }

        public String getServiceId() {
            return fortumoDetails.getServiceId();
        }

        public String getInAppSecret() {
            return fortumoDetails.getServiceInAppSecret();
        }

        public SkuDetails toSkuDetails(String price) {
            return new SkuDetails(OpenIabHelper.ITEM_TYPE_INAPP, getProductId(), getTitle(), price, getDescription());
        }

        public String getFortumoPrice() {
            return fortumoPrice;
        }

        public String getNookServiceId() {
            return fortumoDetails.getNookServiceId();
        }

        public String getNookInAppSecret() {
            return fortumoDetails.getNookInAppSecret();
        }


        public boolean isConsumable() {
            return fortumoDetails.isConsumable();
        }

        @Override
        public String toString() {
            return "FortumoProduct{" +
                    "fortumoDetails=" + fortumoDetails +
                    ", fortumoPrice='" + fortumoPrice + '\'' +
                    '}';
        }
    }


    static void addPendingPayment(Context context, String productId, String messageId) {
        final SharedPreferences fortumoSharedPrefs = getFortumoSharedPrefs(context);
        final SharedPreferences.Editor editor = fortumoSharedPrefs.edit();
        editor.putString(productId, messageId);
        editor.commit();
        if (isDebugLog()) {
            Log.d(TAG, String.format("%s was added to pending", productId));
        }
    }

    static String getMessageIdInPending(Context context, String productId) {
        final SharedPreferences fortumoSharedPrefs = getFortumoSharedPrefs(context);
        return fortumoSharedPrefs.getString(productId, null);
    }

    static void removePendingProduct(Context context, String productId) {
        final SharedPreferences fortumoSharedPrefs = getFortumoSharedPrefs(context);
        final SharedPreferences.Editor edit = fortumoSharedPrefs.edit();
        edit.remove(productId);
        edit.commit();
        if (isDebugLog()) {
            Log.d(TAG, String.format("%s was removed from pending", productId));
        }
    }


    static SharedPreferences getFortumoSharedPrefs(Context context) {
        return context.getSharedPreferences(SHARED_PREFS_FORTUMO, Context.MODE_PRIVATE);
    }


    static class FortumoProductParser {
        private static final Pattern skuPattern = Pattern.compile("([a-z]|[0-9]){1}[a-z0-9._]*");

        //TAGS
        private static final String FORTUMO_PRODUCTS_TAG = "fortumo-products";
        private static final String PRODUCT_TAG = "product";
        //ATTRS
        private static final String ID_ATTR = "id";
        private static final String SERVICE_ID_ATTR = "service-id";
        private static final String NOOK_SERVICE_ID_ATTR = "nook-service-id";
        private static final String SERVICE_INAPP_SECRET_ATTR = "service-inapp-secret";
        private static final String NOOK_SERVICE_INAPP_SECRET_ATTR = "nook-service-inapp-secret";
        private static final String CONSUMABLE_ATTR = "consumable";

        private FortumoProductParser() {

        }

        static Map<String, FortumoDetails> parse(Context context, boolean isNook) throws XmlPullParserException, IOException {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(context.getAssets().open(FortumoStore.FORTUMO_DETAILS_FILE_NAME), null);

            HashMap<String, FortumoDetails> map = new HashMap<String, FortumoDetails>();
            FortumoDetails sku = null;
            boolean insideFortumoProducts = false;

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (tagName.equals(FORTUMO_PRODUCTS_TAG)) {
                            insideFortumoProducts = true;
                        } else if (tagName.equalsIgnoreCase(PRODUCT_TAG)) {
                            if (!insideFortumoProducts) {
                                throw new IllegalStateException(String.format("%s is not inside %s", PRODUCT_TAG, FORTUMO_PRODUCTS_TAG));
                            }
                            final String skuValue = parser.getAttributeValue(null, ID_ATTR);
                            if (!skuPattern.matcher(skuValue).matches()) {
                                throw new IllegalStateException(String.format("Wrong SKU: %s. SKU must match \"([a-z]|[0-9]){1}[a-z0-9._]*\".", skuValue));
                            }

                            final String serviceIdAttr = parser.getAttributeValue(null, SERVICE_ID_ATTR);
                            final String serviceInAppSecretAttr = parser.getAttributeValue(null, SERVICE_INAPP_SECRET_ATTR);
                            if (!serviceInfoIsComplete(serviceIdAttr, serviceInAppSecretAttr)) {
                                throw new IllegalStateException(String.format("%s: service data is NOT complete", skuValue));
                            }

                            final String nookServiceIdAttr = parser.getAttributeValue(null, NOOK_SERVICE_ID_ATTR);
                            final String nookServiceInAppSecretAttr = parser.getAttributeValue(null, NOOK_SERVICE_INAPP_SECRET_ATTR);
                            if (!serviceInfoIsComplete(nookServiceIdAttr, nookServiceInAppSecretAttr)) {
                                throw new IllegalStateException(String.format("%s: service data is NOT complete", skuValue));
                            }

                            if (isNook) {
                                if (TextUtils.isEmpty(nookServiceIdAttr) || TextUtils.isEmpty(nookServiceInAppSecretAttr)) {
                                    throw new IllegalStateException("fortumo nook-service-id attribute and nook-service-inapp-secret values must be non-empty!");
                                }
                            } else {
                                if (TextUtils.isEmpty(serviceIdAttr) || TextUtils.isEmpty(serviceInAppSecretAttr)) {
                                    throw new IllegalStateException("fortumo service-id attribute and service-inapp-secret values must be non-empty!");
                                }
                            }
                            sku = new FortumoDetails(skuValue, Boolean.parseBoolean(parser.getAttributeValue(null, CONSUMABLE_ATTR)),
                                    serviceIdAttr, serviceInAppSecretAttr, nookServiceIdAttr, nookServiceInAppSecretAttr);
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        if (tagName.equals(PRODUCT_TAG)) {
                            map.put(sku.getId(), sku);
                            sku = null;
                        } else if (tagName.equals(FORTUMO_PRODUCTS_TAG)) {
                            insideFortumoProducts = false;
                        }
                        break;

                    default:
                        break;
                }
                eventType = parser.next();
            }
            return map;
        }

        private static boolean serviceInfoIsComplete(String serviceId, String appServiceId) {
            return !(TextUtils.isEmpty(serviceId) && !TextUtils.isEmpty(appServiceId))
                    || (TextUtils.isEmpty(appServiceId) && !TextUtils.isEmpty(serviceId));
        }

        static class FortumoDetails {
            private String id;
            private String serviceId;
            private String serviceInAppSecret;
            private String nookServiceId;
            private String nookInAppSecret;
            private boolean consumable;

            public FortumoDetails(String id, boolean consumable, String serviceId, String serviceInAppSecret,
                                  String nookServiceId, String nookInAppSecret) {
                this.id = id;
                this.consumable = consumable;
                this.serviceId = serviceId;
                this.serviceInAppSecret = serviceInAppSecret;
                this.nookServiceId = nookServiceId;
                this.nookInAppSecret = nookInAppSecret;
            }

            public String getId() {
                return id;
            }

            public boolean isConsumable() {
                return consumable;
            }

            public String getServiceId() {
                return serviceId;
            }

            public String getServiceInAppSecret() {
                return serviceInAppSecret;
            }

            public String getNookServiceId() {
                return nookServiceId;
            }

            public String getNookInAppSecret() {
                return nookInAppSecret;
            }

            @Override
            public String toString() {
                return "FortumoDetails{" +
                        "id='" + id + '\'' +
                        ", serviceId='" + serviceId + '\'' +
                        ", serviceInAppSecret='" + serviceInAppSecret + '\'' +
                        ", nookServiceId='" + nookServiceId + '\'' +
                        ", nookInAppSecret='" + nookInAppSecret + '\'' +
                        ", consumable=" + consumable +
                        '}';
            }
        }
    }

}
