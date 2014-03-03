package org.onepf.oms.appstore.fortumo;

import android.content.Context;
import android.util.Pair;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by akarimova on 17.02.14.
 */
public class InappsXMLParser {
    private static final String TAG = InappsXMLParser.class.getSimpleName();
    private static final Pattern countryPattern = Pattern.compile("[A-Z][A-Z]");
    private static final Pattern localePattern = Pattern.compile("[a-z][a-z]_[A-Z][A-Z]");
    private static final Pattern skuPattern = Pattern.compile("([a-z]|[0-9]){1}[a-z0-9._]*");


    //TAGS
    private static final String INAPP_PRODUCTS_TAG = "inapp-products";
    private static final String SUBSCRIPTIONS_TAG = "subscriptions";
    private static final String SUBSCRIPTION_TAG = "subscription";
    private static final String ITEMS_TAG = "items";
    private static final String ITEM_TAG = "item";
    private static final String SUMMARY_TAG = "summary";
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


    public Pair<List<InappBaseProduct>, List<InappSubscriptionProduct>> parse(Context context) throws XmlPullParserException, IOException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(context.getAssets().open(FortumoStore.IN_APP_PRODUCTS_FILE_NAME), null);

        List<InappBaseProduct> itemsList = new ArrayList<InappBaseProduct>();
        List<InappSubscriptionProduct> subscriptionList = new ArrayList<InappSubscriptionProduct>();

        InappBaseProduct currentProduct = null;
        String title = null;
        String text = null;
        String description = null;
        String currentLocale = null;
        String currentCountryCode = null;
        String currentSubPeriod = null;

        boolean insideInapps = false;
        boolean insideItems = false;
        boolean insideItem = false;
        boolean insideSubs = false;
        boolean insideSub = false;
        boolean insideSummary = false;
        boolean insideSummaryBase = false;
        boolean insideSummaryLocal = false;
        boolean insidePrice = false;

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagName = parser.getName();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (tagName.equals(INAPP_PRODUCTS_TAG)) {
                        insideInapps = true;
                    } else if (tagName.equals(ITEMS_TAG)) {
                        if (!insideInapps) {
                            inWrongNode(ITEMS_TAG, INAPP_PRODUCTS_TAG);
                        }
                        insideItems = true;
                    } else if (tagName.equals(SUBSCRIPTIONS_TAG)) {
                        if (!insideInapps) {
                            inWrongNode(SUBSCRIPTIONS_TAG, INAPP_PRODUCTS_TAG);
                        }
                        insideSubs = true;
                    } else if (tagName.equals(ITEM_TAG) || tagName.equals(SUBSCRIPTION_TAG)) {
                        if (tagName.equals(SUBSCRIPTION_TAG)) {
                            if (!insideSubs) {
                                inWrongNode(SUBSCRIPTION_TAG, SUBSCRIPTIONS_TAG);
                            }
                            currentSubPeriod = parser.getAttributeValue(null, PERIOD_ATTR);
                            if (!(InappSubscriptionProduct.ONE_MONTH.equals(currentSubPeriod) || InappSubscriptionProduct.ONE_YEAR.equals(currentSubPeriod))) {
                                throw new IllegalStateException(String.format("Wrong \"period\" value: %s. Must be \"%s\" or \"%s\".", currentSubPeriod, InappSubscriptionProduct.ONE_MONTH,
                                        InappSubscriptionProduct.ONE_YEAR));
                            }
                            insideSub = true;
                        } else {
                            if (!insideItems) {
                                inWrongNode(ITEMS_TAG, ITEMS_TAG);
                            }
                            insideItem = true;
                        }
                        currentProduct = new InappBaseProduct();
                        final String sku = parser.getAttributeValue(null, ID_ATTR);
                        final Matcher matcher = skuPattern.matcher(sku);
                        if (!matcher.matches()) {
                            throw new IllegalStateException(String.format("Wrong SKU ID: %s. SKU must match \"([a-z]|[0-9]){1}[a-z0-9._]*\"", sku));
                        }
                        currentProduct.setProductId(sku);
                        final String publishState = parser.getAttributeValue(null, PUBLISH_STATE_ATTR);
                        if (!(InappBaseProduct.UNPUBLISHED.equals(publishState) || InappBaseProduct.PUBLISHED.equals(publishState))) {
                            throw new IllegalStateException(String.format("Wrong publish state value: %s. Must be \"%s\" or \"%s\"", publishState, InappBaseProduct.UNPUBLISHED, InappBaseProduct.PUBLISHED));
                        }
                        currentProduct.setPublished(publishState);
                    } else if (tagName.equals(SUMMARY_TAG)) {
                        if (!(insideItem || insideSub)) {
                            inWrongNode(SUMMARY_TAG, ITEM_TAG, SUBSCRIPTION_TAG);
                        }
                        insideSummary = true;
                    } else if (tagName.equals(SUMMARY_BASE_TAG)) {
                        if (!insideSummary) {
                            inWrongNode(SUMMARY_BASE_TAG, SUMMARY_TAG);
                        }
                        insideSummaryBase = true;
                    } else if (tagName.equals(SUMMARY_LOCALIZATION_TAG)) {
                        if (!insideSummary) {
                            inWrongNode(SUMMARY_LOCALIZATION_TAG, SUMMARY_TAG);
                        }
                        currentLocale = parser.getAttributeValue(null, LOCALE_ATTR);
                        if (!localePattern.matcher(currentLocale).matches()) {
                            throw new IllegalStateException(String.format("Wrong \"locale\" attribute value: %s. Must match [a-z][a-z]_[A-Z][A-Z].", currentCountryCode));
                        }
                        insideSummaryLocal = true;
                    } else if (tagName.equals(COMMON_TITLE_TAG)) {
                        if (!(insideSummaryBase || insideSummaryLocal)) {
                            inWrongNode(COMMON_TITLE_TAG, SUMMARY_BASE_TAG, SUMMARY_LOCALIZATION_TAG);
                        }
                    } else if (tagName.equals(COMMON_DESCRIPTION_TAG)) {
                        if (!(insideSummaryBase || insideSummaryLocal)) {
                            inWrongNode(COMMON_DESCRIPTION_TAG, SUMMARY_BASE_TAG, SUMMARY_LOCALIZATION_TAG);
                        }
                    } else if (tagName.equals(PRICE_TAG)) {
                        if (!(insideItem || insideSub)) {
                            inWrongNode(PRICE_TAG, ITEM_TAG, SUBSCRIPTION_TAG);
                        }
                        currentProduct.setAutoFill(Boolean.parseBoolean(parser.getAttributeValue(null, AUTOFILL_ATTR)));
                        insidePrice = true;
                    } else if (tagName.equals(PRICE_BASE_TAG)) {
                        if (!insidePrice) {
                            inWrongNode(PRICE_BASE_TAG, PRICE_TAG);
                        }
                    } else if (tagName.equals(PRICE_LOCAL_TAG)) {
                        if (!insidePrice) {
                            inWrongNode(PRICE_LOCAL_TAG, PRICE_TAG);
                        }
                        currentCountryCode = parser.getAttributeValue(null, COUNTRY_ATTR);
                        final Matcher matcher = countryPattern.matcher(currentCountryCode);
                        if (!matcher.matches()) {
                            throw new IllegalStateException(String.format("Wrong \"country\" attribute value: %s. Must match [A-Z][A-Z].", currentCountryCode));
                        }
                    }
                    break;
                case XmlPullParser.TEXT:
                    text = parser.getText();
                    break;
                case XmlPullParser.END_TAG:
                    if (tagName.equals(INAPP_PRODUCTS_TAG)) {
                        insideInapps = false;
                    } else if (tagName.equals(ITEMS_TAG)) {
                        insideItems = false;
                    } else if (tagName.equals(SUBSCRIPTIONS_TAG)) {
                        insideSubs = false;
                    } else if (tagName.equals(ITEM_TAG)) {
                        itemsList.add(currentProduct);
                        currentProduct = null;
                    } else if (tagName.equals(SUBSCRIPTION_TAG)) {
                        InappSubscriptionProduct subscriptionProduct = new InappSubscriptionProduct(currentProduct, currentSubPeriod);
                        subscriptionList.add(subscriptionProduct);
                        currentSubPeriod = null;
                        currentProduct = null;
                        insideSub = false;
                    } else if (tagName.equals(SUMMARY_TAG)) {
                        insideSummary = false;
                    } else if (tagName.equals(COMMON_TITLE_TAG)) {
                        int length = text.length();
                        if (!(length >= 1 && length <= 55)) {
                            throw new IllegalStateException(String.format("Wrong title length: %d. Must be 1-55 symbols", length));
                        }
                        title = text;
                    } else if (tagName.equals(COMMON_DESCRIPTION_TAG)) {
                        int length = text.length();
                        if (!(length >= 1 && length <= 80)) {
                            throw new IllegalStateException(String.format("Wrong description length: %d. Must be 1-80 symbols", length));
                        }
                        description = text;
                    } else if (tagName.equals(SUMMARY_BASE_TAG)) {
                        currentProduct.setBaseTitle(title);
                        currentProduct.setBaseDescription(description);
                        title = null;
                        description = null;
                        insideSummaryBase = false;
                    } else if (tagName.equals(SUMMARY_LOCALIZATION_TAG)) {
                        currentProduct.addTitleLocalization(currentLocale, title);
                        currentProduct.addDescriptionLocalization(currentLocale, description);
                        title = null;
                        description = null;
                        currentLocale = null;
                        insideSummaryLocal = false;
                    } else if (tagName.equals(PRICE_BASE_TAG) || tagName.equals(PRICE_LOCAL_TAG)) {
                        float price;
                        try {
                            price = Float.parseFloat(text);
                        } catch (NumberFormatException e) {
                            throw new IllegalStateException(String.format("Wrong price: %s. Must be decimal.", text));
                        }

                        if (tagName.equals(PRICE_BASE_TAG)) {
                            currentProduct.setBasePrice(price);
                        } else {
                            currentProduct.addCountryPrice(currentCountryCode, price);
                            currentCountryCode = null;
                        }
                    } else if (tagName.equals(PRICE_TAG)) {
                        insidePrice = false;
                    }
                    break;
                default:
                    break;
            }
            eventType = parser.next();
        }

        return new Pair<List<InappBaseProduct>, List<InappSubscriptionProduct>>(itemsList, subscriptionList);
    }


    private static void inWrongNode(String childTagName, String rightParentTag) {
        throw new IllegalStateException(String.format("%s is not inside %s", childTagName, rightParentTag));
    }

    private static void inWrongNode(String childTagName, String rightParentTag, String otherRightParentTag) {
        throw new IllegalStateException(String.format("%s is not inside %s or %s", childTagName, rightParentTag, otherRightParentTag));
    }
}
