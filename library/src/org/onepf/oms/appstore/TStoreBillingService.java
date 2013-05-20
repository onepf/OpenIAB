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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.skplanet.dodo.IapPlugin;

import java.util.List;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 16.05.13
 */
public class TStoreBillingService implements org.onepf.oms.AppstoreInAppBillingService {
    private static final String TAG = "IabHelper";
    private final Context mContext;
    private final String mAppId;
    private IapPlugin mPlugin;

    public TStoreBillingService(Context context, String appId) {
        mContext = context;
        mAppId = appId;
    }

    @Override
    public void startSetup(org.onepf.oms.appstore.googleUtils.IabHelper.OnIabSetupFinishedListener listener, final IabHelperBillingService billingService) {
        mPlugin = IapPlugin.getPlugin(mContext);
        listener.onIabSetupFinished(new org.onepf.oms.appstore.googleUtils.IabResult(org.onepf.oms.appstore.googleUtils.IabHelper.BILLING_RESPONSE_RESULT_OK, "Setup successful."));
    }

    @Override
    public void launchPurchaseFlow(Activity act, String sku, String itemType, int requestCode, org.onepf.oms.appstore.googleUtils.IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        org.onepf.oms.appstore.tstoreUtils.ParamsBuilder paramsBuilder = new org.onepf.oms.appstore.tstoreUtils.ParamsBuilder();
        paramsBuilder.put(org.onepf.oms.appstore.tstoreUtils.ParamsBuilder.KEY_APPID, mAppId);
        paramsBuilder.put(org.onepf.oms.appstore.tstoreUtils.ParamsBuilder.KEY_PID, sku);
        Bundle req = mPlugin.sendPaymentRequest(paramsBuilder.build(), new org.onepf.oms.appstore.TStoreRequestCallback(this, mContext, listener));
        if (req == null) {
            Log.e(TAG, "TStore buy request failure");
        } else {
            String mRequestId = req.getString(IapPlugin.EXTRA_REQUEST_ID);
            if (mRequestId == null || mRequestId.length() == 0) {
                Log.e(TAG, "TStore request failure");
            }
        }
    }

    @Override
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return false;
    }

    @Override
    public org.onepf.oms.appstore.googleUtils.Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus, List<String> moreSubsSkus) throws org.onepf.oms.appstore.googleUtils.IabException {
        return null;
    }

    @Override
    public void consume(org.onepf.oms.appstore.googleUtils.Purchase itemInfo) throws org.onepf.oms.appstore.googleUtils.IabException {
    }

    @Override
    public void dispose() {
    }
}
