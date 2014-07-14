/* Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onepf.store.data;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.onepf.store.AppstoreBinder;
import org.onepf.store.StoreApplication;

/**
 * Represents an in-app billing purchase.
 */
public class Purchase implements Cloneable {

    String _orderId;
    String _packageName;
    String _sku;
    long _purchaseTime;
    int _purchaseState;
    String _developerPayload;
    String _token;

    public Purchase(String orderId, String packageName, String sku, long purchaseTime, int purchaseState, String developerPayload, String token) {
        _orderId = orderId;
        _packageName = packageName;
        _sku = sku;
        _purchaseTime = purchaseTime;
        _purchaseState = purchaseState;
        _developerPayload = developerPayload;
        _token = token;
    }

    public Purchase(String json) throws JSONException {
        JSONObject o = new JSONObject(json);
        _orderId = o.getString("orderId");
        _packageName = o.getString("packageName");
        _sku = o.getString("productId");
        _purchaseTime = o.getLong("purchaseTime");
        _purchaseState = o.getInt("purchaseState");
        _developerPayload = o.getString("developerPayload");
        _token = o.getString("purchaseToken");
    }

    public String toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("orderId", _orderId);
            o.put("packageName", _packageName);
            o.put("productId", _sku);
            o.put("purchaseTime", _purchaseTime);
            o.put("purchaseState", _purchaseState);
            o.put("developerPayload", _developerPayload);
            o.put("purchaseToken", _token);
        } catch (Exception e) {
            Log.e(StoreApplication.TAG, "Couldn't serialize " + getClass().getSimpleName());
            return "";
        }
        return o.toString();
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Somebody forgot to add Cloneable to class", e);
        }
    }

    public String getOrderId() {
        return _orderId;
    }

    public String getPackageName() {
        return _packageName;
    }

    public String getSku() {
        return _sku;
    }

    public long getPurchaseTime() {
        return _purchaseTime;
    }

    public int getPurchaseState() {
        return _purchaseState;
    }

    public String getDeveloperPayload() {
        return _developerPayload;
    }

    public String getToken() {
        return _token;
    }

    @Override
    public String toString() {
        return "PurchaseInfo: " + toJson();
    }
}
