package org.onepf.oms.appstore.onepfUtils;

import android.content.Context;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by akarimova on 17.02.14.
 */
public class InappsXMLParser {
    private static final String TAG = InappsXMLParser.class.getSimpleName();
    public static final String IN_APP_PRODUCTS_FILE_NAME = "inapps_products.xml";

    //TAGS
    private static final String SUBSCRIPTIONS_TAG = "subscriptions";
    private static final String SUBSCRIPTION_TAG = "subscription";
    private static final String ITEMS_TAG = "items";
    private static final String ITEM_TAG = "item";
    private static final String SUMMARY_LOCALIZATION_TAG = "summary-localization";
    private static final String SUMMARY_BASE_TAG = "summary-base";
    private static final String PRICE_BASE_TAG = "price-base";
    private static final String PRICE_LOCAL_TAG = "price-local";
    private static final String COMMON_TITLE_TAG = "title";
    private static final String COMMON_DESCRIPTION_TAG = "description";
    private static final String PRICE_TAG = "price";

    //ATTRS
    private static final String PUBLISH_STATE_ATTR = "publish-state";
    private static final String ID_ATTR = "id";
    private static final String LOCALE_ATTR = "locale";
    private static final String COUNTRY_ATTR = "country";
    private static final String PERIOD_ATTR = "period";
    private static final String AUTOFILL_ATTR = "autofill";

    public List<BaseInappProduct> parse(Context context) throws XmlPullParserException, IOException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(context.getAssets().open(IN_APP_PRODUCTS_FILE_NAME), null);

        BaseInappProduct currentProduct = null;
        List<BaseInappProduct> itemsList = null;
        List<SubscriptionInappProduct> subscriptionList = null;
        String title = null;
        String about = null;
        String currentLocale = null;
        String currentCountryCode = null;
        String currentSubPeriod = null;
        String text = null;

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagName = parser.getName();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    boolean isSubscription = tagName.equals(SUBSCRIPTION_TAG);
                    if (tagName.equals(ITEMS_TAG)) {
                        itemsList = new ArrayList<BaseInappProduct>();
                    } else if (tagName.equals(SUBSCRIPTIONS_TAG)) {
                        subscriptionList = new ArrayList<SubscriptionInappProduct>();
                    } else if (tagName.equals(ITEM_TAG) || isSubscription) {
                        currentProduct = new BaseInappProduct();
                        currentProduct.setProductId(parser.getAttributeValue(null, ID_ATTR));
                        currentProduct.setPublished(parser.getAttributeValue(null, PUBLISH_STATE_ATTR));
                        if (isSubscription) {
                            currentSubPeriod = parser.getAttributeValue(null, PERIOD_ATTR);
                        }
                    } else if (tagName.equals(SUMMARY_LOCALIZATION_TAG)) {
                        currentLocale = parser.getAttributeValue(null, LOCALE_ATTR);
                    } else if (tagName.equals(PRICE_LOCAL_TAG)) {
                        currentCountryCode = parser.getAttributeValue(null, COUNTRY_ATTR);
                    } else if (tagName.equals(PRICE_TAG)) {
                        currentProduct.setAutoFill(Boolean.parseBoolean(parser.getAttributeValue(null, AUTOFILL_ATTR)));
                    }
                    break;
                case XmlPullParser.TEXT:
                    text = parser.getText();
                    break;
                case XmlPullParser.END_TAG:
                    if (tagName.equals(COMMON_TITLE_TAG)) {
                        title = text;
                    } else if (tagName.equals(COMMON_DESCRIPTION_TAG)) {
                        about = text;
                    } else if (tagName.equals(ITEM_TAG)) {
                        itemsList.add(currentProduct);
                        currentProduct = null;
                    } else if (tagName.equals(SUBSCRIPTION_TAG)) {
                        SubscriptionInappProduct subscriptionProduct = new SubscriptionInappProduct(currentProduct, currentSubPeriod);
                        subscriptionList.add(subscriptionProduct);
                        currentSubPeriod = null;
                        currentProduct = null;
                    } else {
                        if (tagName.equals(SUMMARY_BASE_TAG)) {
                            currentProduct.setBaseTitle(title);
                            currentProduct.setBaseDescription(about);
                            title = null;
                            about = null;
                        } else if (tagName.equals(SUMMARY_LOCALIZATION_TAG)) {
                            currentProduct.addTitleLocalization(currentLocale, title);
                            currentProduct.addDescriptionLocalization(currentLocale, about);
                            title = null;
                            about = null;
                            currentLocale = null;
                        } else if (tagName.equals(PRICE_BASE_TAG)) {
                            currentProduct.setBasePrice(Float.valueOf(text));
                        } else if (tagName.equals(PRICE_LOCAL_TAG)) {
                            currentProduct.addCountryPrice(currentCountryCode, Float.valueOf(text));
                            currentCountryCode = null;
                        }
                    }
                    break;
                default:
                    break;
            }
            eventType = parser.next();
        }

        ArrayList<BaseInappProduct> productList = new ArrayList<BaseInappProduct>();
        productList.addAll(itemsList);
        productList.addAll(subscriptionList);
        return productList;
    }

}
