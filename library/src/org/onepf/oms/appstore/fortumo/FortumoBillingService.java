package org.onepf.oms.appstore.fortumo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Pair;
import mp.MpUtils;
import mp.PaymentRequest;
import mp.PaymentResponse;
import org.onepf.oms.AppstoreInAppBillingService;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.*;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
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
        try {
            fortumoInapps = FortumoBillingService.getFortumoInapps(context);
        } catch (IOException e) {
            result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ERROR, "Fortumo: parsing error.");
        } catch (XmlPullParserException e) {
            result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ERROR, "Fortumo: parsing error.");
        } catch (IabException e) {
            result = e.getResult();
        }
        listener.onIabSetupFinished(result != null ? result : new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, "Fortumo: successful setup."));
    }

    @Override
    public void launchPurchaseFlow(final Activity act, String sku, String itemType, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        this.purchaseFinishedListener = listener;
        this.activityRequestCode = requestCode;
        final FortumoProduct fortumoProduct = fortumoInapps.get(sku);
        if (fortumoProduct == null) {
            purchaseFinishedListener.onIabPurchaseFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_DEVELOPER_ERROR, "Required sku " + sku + " was not declared in xml files."), null);
        } else {
            if (fortumoProduct.isConsumable()) {
                final String messageIdInPending = FortumoStore.getMessageIdInPending(context, fortumoProduct.getProductId());
                if (!TextUtils.isEmpty(messageIdInPending) && !messageIdInPending.equals("-1")) {
                    final PaymentResponse paymentResponse = MpUtils.getPaymentResponse(context, Long.valueOf(messageIdInPending));
                    if (paymentResponse.getBillingStatus() == MpUtils.MESSAGE_STATUS_BILLED) {
                        final Purchase purchase = purchaseFromPaymentResponse(context, paymentResponse);
                        purchaseFinishedListener.onIabPurchaseFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, "Requested sku status is pending"), purchase);
                    } else if (paymentResponse.getBillingStatus() == MpUtils.MESSAGE_STATUS_FAILED || paymentResponse.getBillingStatus() == MpUtils.MESSAGE_STATUS_USE_ALTERNATIVE_METHOD) {
                        purchaseFinishedListener.onIabPurchaseFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ERROR, "Failed to buy purchase"), null);
                    } else {
                        purchaseFinishedListener.onIabPurchaseFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED, "Requested sku status is pending"), null);
                    }
                } else {
                    PaymentRequest paymentRequest = new PaymentRequest.PaymentRequestBuilder().setService(fortumoProduct.getServiceId(), fortumoProduct.getInAppSecret()).
                            setConsumable(fortumoProduct.isConsumable()).
                            setProductName(fortumoProduct.getProductId()).
                            setDisplayString(fortumoProduct.getTitle()).
                            build();
                    startPaymentActivityForResult(act, requestCode, paymentRequest);
                }
            } else {
                PaymentRequest paymentRequest = new PaymentRequest.PaymentRequestBuilder().setService(fortumoProduct.getServiceId(), fortumoProduct.getInAppSecret()).
                        setConsumable(fortumoProduct.isConsumable()).
                        setProductName(fortumoProduct.getProductId()).
                        setDisplayString(fortumoProduct.getTitle()).
                        build();
                startPaymentActivityForResult(act, requestCode, paymentRequest);
            }
        }
    }

    @Override
    public boolean handleActivityResult(int requestCode, int resultCode, Intent intent) {
        if (activityRequestCode != requestCode) return false;
        if (intent == null) {
            purchaseFinishedListener.onIabPurchaseFinished(new IabResult(IabHelper.IABHELPER_BAD_RESPONSE, "Null data in Fortumo IAB result"), null);
        }
        int errorCode = IabHelper.BILLING_RESPONSE_RESULT_ERROR;
        String errorMsg = "Error during purchase.";
        Purchase purchase = null;
        if (resultCode == Activity.RESULT_OK) {
            PaymentResponse paymentResponse = new PaymentResponse(intent);
            purchase = purchaseFromPaymentResponse(context, paymentResponse);
            if (paymentResponse.getBillingStatus() == MpUtils.MESSAGE_STATUS_BILLED) {
                errorCode = IabHelper.BILLING_RESPONSE_RESULT_OK;
            } else if (paymentResponse.getBillingStatus() == MpUtils.MESSAGE_STATUS_PENDING) {
                errorCode = IabHelper.BILLING_RESPONSE_RESULT_ERROR;
                errorMsg = "Purchase status is pending";
                if (fortumoInapps.get(paymentResponse.getProductName()).isConsumable()) {
                    FortumoStore.addPendingPayment(context, paymentResponse.getProductName(), String.valueOf(paymentResponse.getMessageId()));
                    purchase = null; //todo?
                }
            }
        }
        purchaseFinishedListener.onIabPurchaseFinished(new IabResult(errorCode, errorMsg), purchase);
        return true;
    }

    @Override
    public Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus, List<String> moreSubsSkus) throws IabException {
        Inventory inventory = new Inventory();
        SharedPreferences sharedPreferences = context.getSharedPreferences(FortumoStore.SHARED_PREFS_FORTUMO, Context.MODE_PRIVATE);
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
        final Collection<FortumoProduct> inappProducts = fortumoInapps.values();
        for (FortumoProduct fortumoProduct : inappProducts) {
            if (!fortumoProduct.isConsumable()) {
                final List purchaseHistory = MpUtils.getPurchaseHistory(context, fortumoProduct.getServiceId(), fortumoProduct.getInAppSecret(), 10);
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
                    inventory.addSkuDetails(fortumoProduct.toSkuDetails(fortumoProduct.getFortumoPrice()));
                } else {
                    //todo throw an exception?
                }
            }
        }
        return inventory;
    }

    @Override
    public void consume(Purchase itemInfo) throws IabException {
        FortumoStore.removePendingProduct(context, itemInfo.getSku());
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

    public static Map<String, FortumoProduct> getFortumoInapps(Context context) throws IOException, XmlPullParserException, IabException {
        final Map<String, FortumoProduct> map = new HashMap<String, FortumoProduct>();
        final InappsXMLParser inappsXMLParser = new InappsXMLParser();
        final Pair<List<InappBaseProduct>, List<InappSubscriptionProduct>> parse = inappsXMLParser.parse(context);
        //Fortumo doesn't support subscriptions, we don't work with them.
        final List<InappBaseProduct> allItems = parse.first;
        final FortumoProductCreator fortumoDetailsXMLParser = new FortumoProductCreator();
        final Map<String, FortumoProductCreator.FortumoDetails> fortumoSkuDetailsMap = fortumoDetailsXMLParser.parse(context);
        for (InappBaseProduct item : allItems) {
            final String productId = item.getProductId();
            final FortumoProductCreator.FortumoDetails fortumoDetails = fortumoSkuDetailsMap.get(productId);
            if (fortumoDetails == null) {
                throw new IllegalStateException("Fortumo inapp product details were not found");
            }
            List fetchedPriceData = MpUtils.getFetchedPriceData(context, fortumoDetails.getServiceId(), fortumoDetails.getServiceInAppSecret());
            if (fetchedPriceData == null || fetchedPriceData.size() == 0) {
                final boolean supportedOperator = MpUtils.isSupportedOperator(context, fortumoDetails.getServiceId(), fortumoDetails.getServiceInAppSecret());
                if (supportedOperator) {
                    fetchedPriceData = MpUtils.getFetchedPriceData(context, fortumoDetails.getServiceId(), fortumoDetails.getServiceInAppSecret());
                    if (fetchedPriceData == null || fetchedPriceData.size() == 0) {
                        throw new IabException(IabHelper.IABHELPER_ERROR_BASE, "Can't obtain fortumoPrice details from  the server.");
                    }
                } else {
                    throw new IabException(IabHelper.IABHELPER_ERROR_BASE, "Carrier is not supported.");
                }
            }
            FortumoProduct fortumoProduct = new FortumoProduct(item, fortumoDetails, (String) fetchedPriceData.get(0));
            map.put(productId, fortumoProduct);
        }
        return map;
    }

    static void startPaymentActivityForResult(Activity activity, int requestCode, PaymentRequest paymentRequest) {
        Intent localIntent = paymentRequest.toIntent(activity);
        activity.startActivityForResult(localIntent, requestCode);
    }


    static class FortumoProduct extends InappBaseProduct {
        private boolean consumable;
        private String serviceId;
        private String inAppSecret;
        private String fortumoPrice;

        public FortumoProduct(InappBaseProduct otherProduct, FortumoProductCreator.FortumoDetails fortumoDetails, String fortumoPrice) {
            super(otherProduct);
            this.consumable = fortumoDetails.isConsumable();
            this.serviceId = fortumoDetails.getServiceId();
            this.inAppSecret = fortumoDetails.getServiceInAppSecret();
        }

        public boolean isConsumable() {
            return consumable;
        }

        public void setConsumable(boolean consumable) {
            this.consumable = consumable;
        }

        public String getServiceId() {
            return serviceId;
        }

        public void setServiceId(String serviceId) {
            this.serviceId = serviceId;
        }

        public String getInAppSecret() {
            return inAppSecret;
        }

        public void setInAppSecret(String inAppSecret) {
            this.inAppSecret = inAppSecret;
        }

        public SkuDetails toSkuDetails(String price) {
            return new SkuDetails(OpenIabHelper.ITEM_TYPE_INAPP, getProductId(), getTitle(), price, getDescription());
        }

        public String getFortumoPrice() {
            return fortumoPrice;
        }

        public void setFortumoPrice(String fortumoPrice) {
            this.fortumoPrice = fortumoPrice;
        }
    }


    static class FortumoProductCreator {
        private static final String TAG = FortumoProductCreator.class.getSimpleName();

        //TAGS
        private static final String FORTUMO_PRODUCTS_TAG = "fortumo-products";
        private static final String PRODUCT_TAG = "product";
        //ATTRS
        private static final String ID_ATTR = "id";
        private static final String SERVICE_ID_ATTR = "service-id";
        private static final String SERVICE_INAPP_SECRET_ATTR = "service-inapp-secret";
        private static final String CONSUMABLE_ATTR = "consumable";

        public Map<String, FortumoDetails> parse(Context context) throws XmlPullParserException, IOException {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(context.getAssets().open(FortumoStore.FORTUMO_DETATILS_FILE_NAME), null);
            FortumoDetails sku = null;
            HashMap<String, FortumoDetails> map = null;
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagname = parser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (tagname.equals(FORTUMO_PRODUCTS_TAG)) {
                            map = new HashMap<String, FortumoDetails>();
                        } else if (tagname.equalsIgnoreCase(PRODUCT_TAG)) {
                            sku = new FortumoDetails(parser.getAttributeValue(null, ID_ATTR), Boolean.parseBoolean(parser.getAttributeValue(null, CONSUMABLE_ATTR)),
                                    parser.getAttributeValue(null, SERVICE_ID_ATTR), parser.getAttributeValue(null, SERVICE_INAPP_SECRET_ATTR));
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        if (tagname.equals(PRODUCT_TAG)) {
                            map.put(sku.getId(), sku);
                            sku = null;
                        }
                        break;

                    default:
                        break;
                }
                eventType = parser.next();
            }
            return map;
        }

        static class FortumoDetails {
            private String id;
            private String serviceId;
            private String serviceInAppSecret;
            private boolean consumable;

            public FortumoDetails(String id, boolean consumable, String serviceId, String serviceInAppSecret) {
                this.id = id;
                this.consumable = consumable;
                this.serviceId = serviceId;
                this.serviceInAppSecret = serviceInAppSecret;
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
        }
    }
}
