package org.onepf.oms.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onepf.oms.BillingApplication;
import org.onepf.oms.BillingBinder;

import java.util.ArrayList;
import java.util.UUID;

public class Database {

    long _orderid = 0;

    ArrayList<Application> _applicationList = new ArrayList<Application>();
    ArrayList<Purchase> _purchaseHistory = new ArrayList<Purchase>();

    public Database() {
    }

    public Database(String json) throws JSONException {
        if (json == null || json.equals("")) return;

        JSONObject o = new JSONObject(json);
        JSONArray applicationList = o.getJSONArray("applications");
        for (int i = 0; i < applicationList.length(); ++i) {
            JSONObject app = (JSONObject) applicationList.get(i);
            _applicationList.add(new Application(app.toString()));
        }
    }

    String nextOrderId() {
        return Long.toString(_orderid++);
    }

    String generateToken(String packageName, String sku) {
        return packageName +"."+ sku +"."+ UUID.randomUUID();
    }

    public Application getApplication(String packageName) {
        for (Application app : _applicationList) {
            if (app.getPackageName().equals(packageName)) {
                return app;
            }
        }
        return null;
    }

    public SkuDetails getSkuDetails(String packageName, String sku) {
        Application app = getApplication(packageName);
        return app == null ? null : app.getSkuDetails(sku);
    }

    // returns null if failed
    public Purchase createPurchase(String packageName, String sku, String developerPayload) {
        Application app = getApplication(packageName);
        if (app == null) {
            return null;
        }
        SkuDetails skuDetails = app.getSkuDetails(sku);
        if (skuDetails == null) {
            return null;
        }
        return new Purchase(nextOrderId(), packageName, sku, System.currentTimeMillis(),
                BillingBinder.PURCHASE_STATE_PURCHASED,
                developerPayload, generateToken(packageName, sku));
    }

    public void storePurchase(Purchase purchase) {
        _purchaseHistory.add(purchase);
    }

    public int consume(String purchaseToken) {
        for (int i = _purchaseHistory.size()-1; i >= 0; --i) {
            if (_purchaseHistory.get(i).getToken().equals(purchaseToken)) {
                _purchaseHistory.remove(i);
                return BillingBinder.RESULT_OK;
            }
        }
        return BillingBinder.RESULT_ITEM_NOT_OWNED;
    }
}
