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
 * Represents an in-app product's listing details.
 */
public class SkuDetails {
    String mItemType;
    String mSku;
    String mType;
    String mPrice;
    String mTitle;
    String mDescription;
    String mJson;

    public SkuDetails(String jsonSkuDetails) throws JSONException {
        this(IabHelper.ITEM_TYPE_INAPP, jsonSkuDetails);
    }

    public SkuDetails(String sku, String name, String price) {
        mItemType = IabHelper.ITEM_TYPE_INAPP;
        mSku = sku;
        mTitle = name;
        mPrice = price;
    }

    public SkuDetails(String itemType, String sku, String name, String price, String description) {
        mItemType = itemType;
        mSku = sku;
        mTitle = name;
        mPrice = price;
        mDescription = description;
    }

    public SkuDetails(String itemType, String jsonSkuDetails) throws JSONException {
        mItemType = itemType;
        mJson = jsonSkuDetails;
        JSONObject o = new JSONObject(mJson);
        mSku = o.optString("productId");
        mType = o.optString("type");
        mPrice = o.optString("price");
        mTitle = o.optString("title");
        mDescription = o.optString("description");
    }

    public String getSku() {
        return mSku;
    }

    public String getType() {
        return mType;
    }

    public String getPrice() {
        return mPrice;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getItemType() {
        return mItemType;
    }

    public String getJson() {
        return mJson;
    }
    
    void setSku(String sku) {
        this.mSku = sku;
    }

    @Override
    public String toString() {
        return String.format("SkuDetails: type = %s, SKU = %s, title = %s, price = %s, description = %s", mItemType, mSku, mTitle, mPrice, mDescription);
    }
}
