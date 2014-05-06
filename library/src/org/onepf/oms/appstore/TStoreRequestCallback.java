/*******************************************************************************
 * Copyright 2013 One Platform Foundation
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 ******************************************************************************/

package org.onepf.oms.appstore;


import android.content.Context;
import android.util.Log;

import com.skplanet.dodo.IapPlugin;
import com.skplanet.dodo.IapResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Purchase;
import org.onepf.oms.appstore.tstoreUtils.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 05.04.13
 */
public class TStoreRequestCallback implements IapPlugin.RequestCallback {
    private static final String TAG = "IabHelper";

    private final Context mContext;
    private final TStoreBillingService mBillingService;
    private final IabHelper.OnIabPurchaseFinishedListener mListener;

    public TStoreRequestCallback(TStoreBillingService billingService, Context context, IabHelper.OnIabPurchaseFinishedListener listener) {
        mBillingService = billingService;
        mContext = context;
        mListener = listener;
    }


    @Override
    public void onError(String reqid, String errcode, String errmsg) {
        Log.e(TAG, "TStore error. onError() identifier:" + reqid + " code:" + errcode + " msg:" + errmsg);
        // TODO: support different error codes
        IabResult result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ERROR, errmsg);
    }

    @Override
    public void onResponse(IapResponse data) {
        if (data == null || data.getContentLength() <= 0) {
            Log.e(TAG, "onResponse() response data is null");
            return;
        }
        Response response = getResponse(data);
        Purchase purchase = new Purchase(OpenIabHelper.NAME_TSTORE);
        IabResult result;
        if (response.result.code.equals("0000")) {
            result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, "Success");
            for (Response.Product product : response.result.product) {
                // TODO: set correct type
                purchase.setItemType(product.type);
                purchase.setSku(OpenIabHelper.getSku(OpenIabHelper.NAME_TSTORE, product.id));
            }
        } else {
            // TODO: support other error codes
            result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ERROR, response.result.message);
        }
        mListener.onIabPurchaseFinished(result, purchase);
    }

    private Response getResponse(IapResponse data) {
        Response response = null;
        try {
            JSONObject jo = new JSONObject(data.getContentToString());
            final String apiVersion = jo.optString("api_version");
            final String identifier = jo.optString("identifier");
            final String method = jo.getString("method");

            JSONObject resultJsonObject = jo.getJSONObject("result");
            final String message = resultJsonObject.optString("message");
            final String code = resultJsonObject.optString("code");
            final String txid = resultJsonObject.optString("txid");
            final String receipt = resultJsonObject.optString("receipt");
            final int count = resultJsonObject.optInt("count");

            List<Response.Product> products = new ArrayList<Response.Product>();
            if (resultJsonObject.has("product")) {
                final JSONArray productJA = resultJsonObject.optJSONArray("product");
                products = new ArrayList<Response.Product>();
                for (int i = 0; i < productJA.length(); i++) {
                    Response.Product product = new Response.Product();
                    final JSONObject productJson = (JSONObject) productJA.get(i);
                    product.appid = productJson.optString("appid");
                    product.endDate = productJson.optString("endDate");
                    product.id = productJson.optString("id");
                    product.kind = productJson.optString("kind");
                    product.name = productJson.optString("name");
                    product.price = productJson.optDouble("price");
                    product.purchasability = productJson.optBoolean("purchasability");
                    product.startDate = productJson.optString("startDate");
                    product.type = productJson.optString("type");
                    product.validity = productJson.optInt("validity");
                    final JSONObject statusJO = productJson.optJSONObject("status");
                    product.status = new Response.Status(statusJO.optString("code"), statusJO.optString("message"));
                    products.add(product);
                }
            }

            Response.Result result = new Response.Result(code, message, txid, receipt, count, products);
            response = new Response(apiVersion, identifier, method, result);
        } catch (JSONException e) {
            Log.e(TAG, "error during JSON parsing", e);
        }
        return response;
    }
}

