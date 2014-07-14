package org.onepf.store.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.onepf.store.BillingBinder;
import org.onepf.store.StoreApplication;
import org.onepf.store.XmlHelper;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class Database {

    static final String INVENTORY_KEY = "inventory";

    long _orderid = 0;

    ArrayList<SkuDetails> _productList = new ArrayList<SkuDetails>();
    ArrayList<Purchase> _purchaseHistory = new ArrayList<Purchase>();

    final Context _context;


    public Database(Context context) {
        _context = context;
        loadInventory();
    }

    private void saveInventory() {
        StringBuilder result = new StringBuilder();
        for (Purchase p : _purchaseHistory) {
            result.append(p.toJson()).append("|");
        }
        SharedPreferences.Editor prefsEditor = _context.getSharedPreferences(INVENTORY_KEY, 0).edit();
        prefsEditor.putString(INVENTORY_KEY, result.toString());
        prefsEditor.commit();
    }

    private void loadInventory() {
        SharedPreferences prefs = _context.getSharedPreferences(INVENTORY_KEY, 0);
        String data = prefs.getString(INVENTORY_KEY, null);

        if (data == null || data.equals("")) return;

        String[] purchases = data.split("\\|");
        for (String json : purchases) {
            try {
                _purchaseHistory.add(new Purchase(json));
            } catch (JSONException e) {
                Log.e(StoreApplication.TAG, "Failed to deserialize purchase.", e);
            }
        }
    }

    public void deserializeFromAmazonJson(String json) throws JSONException {
        if (json == null || json.equals("")) return;

        JSONObject o = new JSONObject(json);
        Iterator<?> keys = o.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            if (o.get(key) instanceof JSONObject) {
                JSONObject product = (JSONObject) o.get(key);
                String sku;
                String type;
                String price;
                String title;
                String description;

                sku = key;
                String configItemType = product.getString("itemType");
                if (configItemType.equals("ENTITLED") || configItemType.equals("CONSUMABLE")) {
                    type = BillingBinder.ITEM_TYPE_INAPP;
                } else if (configItemType.equals("SUBSCRIPTION")) {
                    type = BillingBinder.ITEM_TYPE_SUBS;
                } else {
                    throw new JSONException("Invalid itemType: " + configItemType);
                }
                price = product.getString("price");
                title = product.getString("title");
                description = product.getString("description");

                _productList.add(new SkuDetails(type, sku, title, price, description));
            }
        }
    }
    
    public static final int PRIMARY_COLUMNS_NUMBER = 7;

    public void deserializeFromGoogleCSV(String csv) throws IOException {
        if (csv == null || csv.equals("")) return;

        String[] lines = csv.split("[\\r\\n]+");
        
        final String JUNK = "Product ID,Published State,Purchase Type,Auto Translate,Locale; Title; Description,Auto Fill Prices,Price";
        for (int i = lines[0].equals(JUNK) ? 1 : 0; i < lines.length; ++i) {
            String line = lines[i];
            String[] primaryColumns = line.split("\\s*,\\s*");
            if (primaryColumns.length < PRIMARY_COLUMNS_NUMBER) {
                throw new IOException(csv + ":" + line + ": Invalid primary columns number: " + primaryColumns.length);
            }
            String sku;
            String type;
            String price;
            String title;
            String description;

            sku = primaryColumns[0];

            String[] localeColumns = primaryColumns[4].split("\\s*;\\s*");
            if (localeColumns.length % 3 != 0) {
                throw new IOException(csv + ":" + line + ": Invalid locale columns number: " + localeColumns.length + ". Should be multiple of 3");
            }
            title = localeColumns[1];
            description = localeColumns[2];

            boolean fillPrices = Boolean.parseBoolean(primaryColumns[5]);
            if (fillPrices) {
                price = primaryColumns[6];
            } else {
                String[] priceColumns = primaryColumns[6].split("\\s*;\\s*");
                if (priceColumns.length % 2 != 0) {
                    throw new IOException(csv + ":" + line + ": Invalid price columns number: " + priceColumns.length + ". Should be even");
                }
                price = priceColumns[1];
            }

            // TODO: this is added in order to support subscriptions in the config
            if (primaryColumns.length > PRIMARY_COLUMNS_NUMBER) {
                type = primaryColumns[PRIMARY_COLUMNS_NUMBER];
                if (!type.equals(BillingBinder.ITEM_TYPE_INAPP) && !type.equals(BillingBinder.ITEM_TYPE_SUBS)) {
                    throw new IOException(csv + ":" + line + ": Invalid product type: " + type);
                }
            } else {
                type = BillingBinder.ITEM_TYPE_INAPP;
            }

            _productList.add(new SkuDetails(type, sku, title, price, description));
        }
    }

    public void deserializeFromOnePFXML(String xml) throws Exception {
        if (xml == null || xml.equals("")) return;

        Element store = XmlHelper.loadXMLFromString(xml).getDocumentElement();
        NodeList productList = store.getElementsByTagName("product");
        for (int i = 0; i < productList.getLength(); ++i) {
            String sku;
            String type;
            String price;
            String title;
            String description;

            Element product = (Element) productList.item(i);
            NamedNodeMap attributes = product.getAttributes();

            sku = attributes.getNamedItem("productId").getNodeValue();
            Node typeNode = attributes.getNamedItem("type");
            type = typeNode == null ? BillingBinder.ITEM_TYPE_INAPP : typeNode.getNodeValue();
            Node priceNode = attributes.getNamedItem("price");
            price = priceNode == null ? "0" : priceNode.getNodeValue();
            Node titleNode = attributes.getNamedItem("title");
            title = titleNode == null ? "" : titleNode.getNodeValue();
            Node descriptionNode = attributes.getNamedItem("description");
            description = descriptionNode == null ? "" : descriptionNode.getNodeValue();

            _productList.add(new SkuDetails(type, sku, title, price, description));
        }
    }

    String nextOrderId() {
        return Long.toString(_orderid++);
    }

    String generateToken(String packageName, String sku) {
        return packageName + "." + sku + "." + UUID.randomUUID();
    }

    public SkuDetails getSkuDetails(String sku) {
        for (SkuDetails product : _productList) {
            if (product.getSku().equals(sku)) {
                return product;
            }
        }
        return null;
    }

    // returns null if failed
    public Purchase createPurchase(String packageName, String sku, String developerPayload) {
        SkuDetails skuDetails = getSkuDetails(sku);
        if (skuDetails == null) {
            return null;
        }
        return new Purchase(nextOrderId(), packageName, sku, System.currentTimeMillis(),
                BillingBinder.PURCHASE_STATE_PURCHASED,
                developerPayload, generateToken(packageName, sku));
    }

    public void storePurchase(Purchase purchase) {
        _purchaseHistory.add(purchase);
        saveInventory();

    }

    public int consume(String purchaseToken) {
        for (int i = _purchaseHistory.size() - 1; i >= 0; --i) {
            if (_purchaseHistory.get(i).getToken().equals(purchaseToken)) {
                _purchaseHistory.remove(i);
                saveInventory();
                return BillingBinder.RESULT_OK;
            }
        }
        return BillingBinder.RESULT_ITEM_NOT_OWNED;
    }

    public ArrayList<Purchase> getInventory(String packageName, String type) {
        ArrayList<Purchase> inventory = new ArrayList<Purchase>();
        for (Purchase p : _purchaseHistory) {
            if (getSkuDetails(p.getSku()).getType().equals(type) && p.getPackageName().equals(packageName)) {
                inventory.add(p);
            }
        }
        return inventory;
    }
}
