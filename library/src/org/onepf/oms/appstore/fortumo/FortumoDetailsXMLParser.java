package org.onepf.oms.appstore.fortumo;

import android.content.Context;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by akarimova on 17.02.14.
 */
public class FortumoDetailsXMLParser {
    private static final String TAG = FortumoDetailsXMLParser.class.getSimpleName();
    public static final String INAPP_PRODUCTS_FILE_NAME = "inapp_products.xml";

    //TAGS
    private static final String FORTUMO_PRODUCTS_TAG = "fortumo-products";
    private static final String PRODUCT_TAG = "product";
    //ATTRS
    private static final String ID_ATTR = "id";
    private static final String SERVICE_ID_ATTR = "service-id";
    private static final String SERVICE_INAPP_SECRET_ATTR = "service-inapp-secret";
    private static final String CONSUMABLE_ATTR = "consumable";

    public Map<String, FortumoSku> parse(Context context) throws XmlPullParserException, IOException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(context.getAssets().open(INAPP_PRODUCTS_FILE_NAME), null);
        FortumoSku sku = null;
        HashMap<String, FortumoSku> map = null;
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagname = parser.getName();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (tagname.equals(FORTUMO_PRODUCTS_TAG)) {
                        map = new HashMap<String, FortumoSku>();
                    } else if (tagname.equalsIgnoreCase(PRODUCT_TAG)) {
                        sku = new FortumoSku(parser.getAttributeValue(null, ID_ATTR), Boolean.parseBoolean(parser.getAttributeValue(null, CONSUMABLE_ATTR)),
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

}
