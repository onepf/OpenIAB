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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import com.amazon.inapp.purchasing.PurchasingManager;
import org.onepf.life2.oms.AppstoreInAppBillingService;
import org.onepf.life2.oms.AppstoreName;
import org.onepf.life2.oms.OpenSku;
import org.onepf.life2.oms.appstore.googleUtils.IabHelper;
import org.onepf.life2.oms.appstore.googleUtils.IabResult;
import org.onepf.life2.oms.appstore.googleUtils.Inventory;
import org.onepf.life2.oms.appstore.googleUtils.Purchase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 16.04.13
 */
public class AmazonAppstoreBillingService implements AppstoreInAppBillingService {
    private final Context mContext;
    private Map<String, IabHelper.OnIabPurchaseFinishedListener> mRequestListeners;
    private boolean mIsInstaller = false;
    private String mCurrentUser;

    public AmazonAppstoreBillingService(Context context) {
        mContext = context;
        mRequestListeners = new HashMap<>();
    }

    @Override
    public void startSetup(IabHelper.OnIabSetupFinishedListener listener) {
        AmazonAppstoreObserver purchasingObserver = new AmazonAppstoreObserver(mContext, this);
        PurchasingManager.registerObserver(purchasingObserver);
        listener.onIabSetupFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, "Setup successful."));
    }

    @Override
    public void launchPurchaseFlow(Activity act, OpenSku sku, String itemType, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        String requestId = PurchasingManager.initiatePurchaseRequest(sku.getSku(AppstoreName.AMAZON));
        storeRequestListener(requestId, listener);
    }

    @Override
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return false;
    }

    @Override
    public Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus, List<String> moreSubsSkus) {
        return null;
    }

    @Override
    public void consume(Purchase itemInfo) {
    }


    private void storeRequestListener(String requestId, IabHelper.OnIabPurchaseFinishedListener listener) {
        mRequestListeners.put(requestId, listener);
    }

    public IabHelper.OnIabPurchaseFinishedListener getRequestListener(String requestId) {
        return mRequestListeners.get(requestId);
    }

    public void setCurrentUser(String currentUser) {
        mCurrentUser = currentUser;
    }

    public String getCurrentUser() {
        return mCurrentUser;
    }
}
