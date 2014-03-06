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
 * @author akarimova@onepf.org
 * @since 17.02.14
 */
public class InappsXMLParser {
    private static final String TAG = InappsXMLParser.class.getSimpleName();
    private static final Pattern countryPattern = Pattern.compile("[A-Z][A-Z]");
    private static final Pattern localePattern = Pattern.compile("[a-z][a-z]_[A-Z][A-Z]");
    private static final Pattern skuPattern = Pattern.compile("([a-z]|[0-9]){1}[a-z0-9._]*");


    //TAGS
    private static final String TAG_INAPP_PRODUCTS = "inapp-products";
    private static final String TAG_SUBSCRIPTIONS = "subscriptions";
    private static final String TAG_SUBSCRIPTION = "subscription";
    private static final String TAG_ITEMS = "items";
    private static final String TAG_ITEM = "item";
    private static final String TAG_SUMMARY = "summary";
    private static final String TAG_SUMMARY_LOCALIZATION = "summary-localization";
    private static final String TAG_SUMMARY_BASE = "summary-base";
    private static final String TAG_PRICE_BASE = "price-base";
    private static final String TAG_PRICE_LOCAL = "price-local";
    private static final String TAG_COMMON_TITLE = "title";
    private static final String TAG_COMMON_DESCRIPTION = "description";
    private static final String TAG_PRICE = "price";

    //ATTRS
    private static final String ATTR_PUBLISH_STATE = "publish-state";
    private static final String ATTR_ID = "id";
    private static final String ATTR_LOCALE = "locale";
    private static final String ATTR_COUNTRY = "country";
    private static final String ATTR_PERIOD = "period";
    private static final String ATTR_AUTOFILL = "autofill";


    /**
     * Make sure that {@link org.onepf.oms.appstore.fortumo.FortumoStore#IN_APP_PRODUCTS_FILE_NAME} is present in the assets folder.
     * @param context to get access to assets
     * @return a set of items and subscriptions
     * @throws XmlPullParserException
     * @throws IOException
     */
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
                    if (tagName.equals(TAG_INAPP_PRODUCTS)) {
                        insideInapps = true;
                    } else if (tagName.equals(TAG_ITEMS)) {
                        if (!insideInapps) {
                            inWrongNode(TAG_ITEMS, TAG_INAPP_PRODUCTS);
                        }
                        insideItems = true;
                    } else if (tagName.equals(TAG_SUBSCRIPTIONS)) {
                        if (!insideInapps) {
                            inWrongNode(TAG_SUBSCRIPTIONS, TAG_INAPP_PRODUCTS);
                        }
                        insideSubs = true;
                    } else if (tagName.equals(TAG_ITEM) || tagName.equals(TAG_SUBSCRIPTION)) {
                        if (tagName.equals(TAG_SUBSCRIPTION)) {
                            if (!insideSubs) {
                                inWrongNode(TAG_SUBSCRIPTION, TAG_SUBSCRIPTIONS);
                            }
                            currentSubPeriod = parser.getAttributeValue(null, ATTR_PERIOD);
                            if (!(InappSubscriptionProduct.ONE_MONTH.equals(currentSubPeriod) || InappSubscriptionProduct.ONE_YEAR.equals(currentSubPeriod))) {
                                throw new IllegalStateException(String.format("Wrong \"period\" value: %s. Must be \"%s\" or \"%s\".", currentSubPeriod, InappSubscriptionProduct.ONE_MONTH,
                                        InappSubscriptionProduct.ONE_YEAR));
                            }
                            insideSub = true;
                        } else {
                            if (!insideItems) {
                                inWrongNode(TAG_ITEMS, TAG_ITEMS);
                            }
                            insideItem = true;
                        }
                        currentProduct = new InappBaseProduct();
                        final String sku = parser.getAttributeValue(null, ATTR_ID);
                        final Matcher matcher = skuPattern.matcher(sku);
                        if (!matcher.matches()) {
                            throw new IllegalStateException(String.format("Wrong SKU ID: %s. SKU must match \"([a-z]|[0-9]){1}[a-z0-9._]*\"", sku));
                        }
                        currentProduct.setProductId(sku);
                        final String publishState = parser.getAttributeValue(null, ATTR_PUBLISH_STATE);
                        if (!(InappBaseProduct.UNPUBLISHED.equals(publishState) || InappBaseProduct.PUBLISHED.equals(publishState))) {
                            throw new IllegalStateException(String.format("Wrong publish state value: %s. Must be \"%s\" or \"%s\"", publishState, InappBaseProduct.UNPUBLISHED, InappBaseProduct.PUBLISHED));
                        }
                        currentProduct.setPublished(publishState);
                    } else if (tagName.equals(TAG_SUMMARY)) {
                        if (!(insideItem || insideSub)) {
                            inWrongNode(TAG_SUMMARY, TAG_ITEM, TAG_SUBSCRIPTION);
                        }
                        insideSummary = true;
                    } else if (tagName.equals(TAG_SUMMARY_BASE)) {
                        if (!insideSummary) {
                            inWrongNode(TAG_SUMMARY_BASE, TAG_SUMMARY);
                        }
                        insideSummaryBase = true;
                    } else if (tagName.equals(TAG_SUMMARY_LOCALIZATION)) {
                        if (!insideSummary) {
                            inWrongNode(TAG_SUMMARY_LOCALIZATION, TAG_SUMMARY);
                        }
                        currentLocale = parser.getAttributeValue(null, ATTR_LOCALE);
                        if (!localePattern.matcher(currentLocale).matches()) {
                            throw new IllegalStateException(String.format("Wrong \"locale\" attribute value: %s. Must match [a-z][a-z]_[A-Z][A-Z].", currentCountryCode));
                        }
                        insideSummaryLocal = true;
                    } else if (tagName.equals(TAG_COMMON_TITLE)) {
                        if (!(insideSummaryBase || insideSummaryLocal)) {
                            inWrongNode(TAG_COMMON_TITLE, TAG_SUMMARY_BASE, TAG_SUMMARY_LOCALIZATION);
                        }
                    } else if (tagName.equals(TAG_COMMON_DESCRIPTION)) {
                        if (!(insideSummaryBase || insideSummaryLocal)) {
                            inWrongNode(TAG_COMMON_DESCRIPTION, TAG_SUMMARY_BASE, TAG_SUMMARY_LOCALIZATION);
                        }
                    } else if (tagName.equals(TAG_PRICE)) {
                        if (!(insideItem || insideSub)) {
                            inWrongNode(TAG_PRICE, TAG_ITEM, TAG_SUBSCRIPTION);
                        }
                        currentProduct.setAutoFill(Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_AUTOFILL)));
                        insidePrice = true;
                    } else if (tagName.equals(TAG_PRICE_BASE)) {
                        if (!insidePrice) {
                            inWrongNode(TAG_PRICE_BASE, TAG_PRICE);
                        }
                    } else if (tagName.equals(TAG_PRICE_LOCAL)) {
                        if (!insidePrice) {
                            inWrongNode(TAG_PRICE_LOCAL, TAG_PRICE);
                        }
                        currentCountryCode = parser.getAttributeValue(null, ATTR_COUNTRY);
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
                    if (tagName.equals(TAG_INAPP_PRODUCTS)) {
                        insideInapps = false;
                    } else if (tagName.equals(TAG_ITEMS)) {
                        insideItems = false;
                    } else if (tagName.equals(TAG_SUBSCRIPTIONS)) {
                        insideSubs = false;
                    } else if (tagName.equals(TAG_ITEM)) {
                        currentProduct.validateItem();
                        itemsList.add(currentProduct);
                        currentProduct = null;
                    } else if (tagName.equals(TAG_SUBSCRIPTION)) {
                        InappSubscriptionProduct subscriptionProduct = new InappSubscriptionProduct(currentProduct, currentSubPeriod);
                        subscriptionProduct.validateItem();
                        subscriptionList.add(subscriptionProduct);
                        currentSubPeriod = null;
                        currentProduct = null;
                        insideSub = false;
                    } else if (tagName.equals(TAG_SUMMARY)) {
                        insideSummary = false;
                    } else if (tagName.equals(TAG_COMMON_TITLE)) {
                        int length = text.length();
                        if (!(length >= 1 && length <= 55)) {
                            throw new IllegalStateException(String.format("Wrong title length: %d. Must be 1-55 symbols", length));
                        }
                        title = text;
                    } else if (tagName.equals(TAG_COMMON_DESCRIPTION)) {
                        int length = text.length();
                        if (!(length >= 1 && length <= 80)) {
                            throw new IllegalStateException(String.format("Wrong description length: %d. Must be 1-80 symbols", length));
                        }
                        description = text;
                    } else if (tagName.equals(TAG_SUMMARY_BASE)) {
                        currentProduct.setBaseTitle(title);
                        currentProduct.setBaseDescription(description);
                        title = null;
                        description = null;
                        insideSummaryBase = false;
                    } else if (tagName.equals(TAG_SUMMARY_LOCALIZATION)) {
                        currentProduct.addTitleLocalization(currentLocale, title);
                        currentProduct.addDescriptionLocalization(currentLocale, description);
                        title = null;
                        description = null;
                        currentLocale = null;
                        insideSummaryLocal = false;
                    } else if (tagName.equals(TAG_PRICE_BASE) || tagName.equals(TAG_PRICE_LOCAL)) {
                        float price;
                        try {
                            price = Float.parseFloat(text);
                        } catch (NumberFormatException e) {
                            throw new IllegalStateException(String.format("Wrong price: %s. Must be decimal.", text));
                        }

                        if (tagName.equals(TAG_PRICE_BASE)) {
                            currentProduct.setBasePrice(price);
                        } else {
                            currentProduct.addCountryPrice(currentCountryCode, price);
                            currentCountryCode = null;
                        }
                    } else if (tagName.equals(TAG_PRICE)) {
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
