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

package org.onepf.oms.appstore.googleUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents an in-app billing purchase.
 *
 * Purchase contains all data from receipt, including signature.
 * <p>
 * To verify signature manually - use {@link #getOriginalJson()} and {@link #getSignature()}
 * Appstore name purchase was done through can be accessed via {@link #getAppstoreName()}
 *
 * <p><b>TODO</b>: keep google.Purchase untouched and use extender everywhere 
 * <p><b>TODO</b>: add getStoreSku() to use mapped value in Appstore's inner code
 */
public class Purchase implements Cloneable {
    String mItemType;  // ITEM_TYPE_INAPP or ITEM_TYPE_SUBS
    String mOrderId;
    String mPackageName;
    String mSku;
    long mPurchaseTime;
    int mPurchaseState;
    String mDeveloperPayload;
    String mToken;
    String mOriginalJson;
    String mSignature;
    String appstoreName;

    public Purchase(String appstoreName) {
        if (appstoreName == null) throw new IllegalArgumentException("appstoreName must be defined");
        this.appstoreName = appstoreName;
    }

    public void setItemType(String itemType) {
        mItemType = itemType;
    }

    public void setOrderId(String orderId) {
        mOrderId = orderId;
    }

    public void setPackageName(String packageName) {
        mPackageName = packageName;
    }

    public void setSku(String sku) {
        mSku = sku;
    }

    public void setPurchaseTime(long purchaseTime) {
        mPurchaseTime = purchaseTime;
    }

    public void setPurchaseState(int purchaseState) {
        mPurchaseState = purchaseState;
    }

    public void setDeveloperPayload(String developerPayload) {
        mDeveloperPayload = developerPayload;
    }

    public void setToken(String token) {
        mToken = token;
    }

    public Purchase(String itemType, String jsonPurchaseInfo, String signature, String appstoreName) throws JSONException {
        this.appstoreName = appstoreName;
        mItemType = itemType;
        mOriginalJson = jsonPurchaseInfo;
        JSONObject o = new JSONObject(mOriginalJson);
        mOrderId = o.optString("orderId");
        mPackageName = o.optString("packageName");
        mSku = o.optString("productId");
        mPurchaseTime = o.optLong("purchaseTime");
        mPurchaseState = o.optInt("purchaseState");
        mDeveloperPayload = o.optString("developerPayload");
        mToken = o.optString("token", o.optString("purchaseToken"));
        mSignature = signature;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Somebody forgot to add Cloneable to class", e);
        }
    }

    public String getItemType() {
        return mItemType;
    }

    public String getOrderId() {
        return mOrderId;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public String getSku() {
        return mSku;
    }

    public long getPurchaseTime() {
        return mPurchaseTime;
    }

    public int getPurchaseState() {
        return mPurchaseState;
    }

    public String getDeveloperPayload() {
        return mDeveloperPayload;
    }

    public String getToken() {
        return mToken;
    }

    public String getOriginalJson() {
        return mOriginalJson;
    }

    public String getSignature() {
        return mSignature;
    }

    public String getAppstoreName() {
        return appstoreName;
    }

    @Override
    public String toString() {
        return "PurchaseInfo(type:" + mItemType + "): "
                + "{\"orderId\":" + mOrderId
                + ",\"packageName\":" + mPackageName
                + ",\"productId\":" + mSku
                + ",\"purchaseTime\":" + mPurchaseTime
                + ",\"purchaseState\":" + mPurchaseState
                + ",\"developerPayload\":" + mDeveloperPayload
                + ",\"token\":" + mToken
                + "}";
    }
}
