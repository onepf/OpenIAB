package org.onepf.oms.data;

import org.w3c.dom.Element;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

public class Application {

    String _packageName;
    int _version;
    boolean _installed;
    boolean _billingActive;

    ArrayList<SkuDetails> _productList = new ArrayList<SkuDetails>();
    ArrayList<String> _inventoryList = new ArrayList<String>();

    public Application(String name, int version) {
        _packageName = name;
        _version = version;
    }

    public Application(Element xml) {
        _packageName = xml.getAttribute("packageName");

        String version = xml.getAttribute("version");
        _version = version != null ? Integer.parseInt(version) : 0;

        String installed = xml.getAttribute("installed");
        _installed = installed == null || Boolean.parseBoolean(installed);

        String billingActive = xml.getAttribute("billingActive");
        _billingActive = billingActive == null || Boolean.parseBoolean(billingActive);

        NodeList productList = xml.getElementsByTagName("product");
        if (productList != null) {
            for (int i = 0; i < productList.getLength(); ++i) {
                _productList.add(new SkuDetails(productList.item(i)));
            }
        }

        NodeList inventoryList = ((Element) xml.getElementsByTagName("inventory").item(0)).getElementsByTagName("item");
        if (inventoryList != null) {
            for (int i = 0; i < inventoryList.getLength(); ++i) {
                String sku = inventoryList.item(i).getTextContent();
                _inventoryList.add(sku);
            }
        }
    }

    public Application(String json) throws JSONException {
        JSONObject o = new JSONObject(json);

        _packageName = o.getString("packageName");
        _version = o.optInt("version", 0);
        _installed = o.optBoolean("installed", true);
        _billingActive = o.optBoolean("billingActive", true);

        JSONArray products = o.optJSONArray("products");
        if (products != null) {
            for (int i = 0; i < products.length(); ++i) {
                _productList.add(new SkuDetails(products.get(i).toString()));
            }
        }

        JSONArray inventoryList = o.optJSONArray("inventory");
        if (inventoryList != null) {
            for (int i = 0; i < inventoryList.length(); ++i) {
                String sku = (String) inventoryList.get(i);
                _inventoryList.add(sku);
            }
        }
    }

    public String getPackageName() {
        return _packageName;
    }

    public int getVersion() {
        return _version;
    }

    public boolean installed() {
        return _installed;
    }

    public boolean billingActive() {
        return _billingActive;
    }

    public ArrayList<String> getInventoryList() {
        return _inventoryList;
    }

    public SkuDetails getSkuDetails(String sku) {
        for (SkuDetails product : _productList) {
            if (product.getSku().equals(sku)) {
                return product;
            }
        }
        return null;
    }
}
