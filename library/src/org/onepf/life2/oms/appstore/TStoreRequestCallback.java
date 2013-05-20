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

package org.onepf.life2.oms.appstore;


import android.content.Context;
import android.util.Log;
import com.skplanet.dodo.IapPlugin;
import com.skplanet.dodo.IapResponse;
import org.onepf.life2.oms.appstore.googleUtils.IabHelper;
import org.onepf.life2.oms.appstore.googleUtils.IabResult;
import org.onepf.life2.oms.appstore.googleUtils.Purchase;
import org.onepf.life2.oms.appstore.tstoreUtils.GsonConverter;
import org.onepf.life2.oms.appstore.tstoreUtils.Response;

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
        Response response = new GsonConverter().fromJson(data.getContentToString());
        Purchase purchase = new Purchase();
        IabResult result = null;
        if (response.result.code.equals("0000")) {
            result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, "Success");
            for (Response.Product product : response.result.product) {
                // TODO: set correct type
                purchase.setItemType(product.type);
                purchase.setSku(product.id);
            }
        } else {
            // TODO: support other error codes
            result = new IabResult(IabHelper.BILLING_RESPONSE_RESULT_ERROR, response.result.message);
        }
        mListener.onIabPurchaseFinished(result, purchase);
    }
}

