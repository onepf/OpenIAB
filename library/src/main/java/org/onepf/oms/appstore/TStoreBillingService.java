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
import org.onepf.oms.AppstoreInAppBillingService;
import org.onepf.oms.appstore.googleUtils.*;
import org.onepf.oms.appstore.tstoreUtils.ParamsBuilder;

import java.util.List;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 16.05.13
 */
public class TStoreBillingService implements AppstoreInAppBillingService {
    private static final String TAG = "IabHelper";
    private final Context mContext;
    private final String mAppId;
    private IapPlugin mPlugin;

    public TStoreBillingService(Context context, String appId) {
        mContext = context;
        mAppId = appId;
    }

    @Override
    public void startSetup(IabHelper.OnIabSetupFinishedListener listener) {
        mPlugin = IapPlugin.getPlugin(mContext);
        listener.onIabSetupFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, "Setup successful."));
    }

    @Override
    public void launchPurchaseFlow(Activity act, String sku, String itemType, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        ParamsBuilder paramsBuilder = new ParamsBuilder();
        paramsBuilder.put(ParamsBuilder.KEY_APPID, mAppId);
        paramsBuilder.put(ParamsBuilder.KEY_PID, sku);
        Bundle req = mPlugin.sendPaymentRequest(paramsBuilder.build(), new TStoreRequestCallback(this, mContext, listener));
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
    public Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus, List<String> moreSubsSkus) throws IabException {
        return null;
    }

    @Override
    public void consume(Purchase itemInfo) throws IabException {
    }

    @Override
    public boolean subscriptionsSupported() {
        //todo check
        return false;
    }

    @Override
    public void dispose() {
    }
}
