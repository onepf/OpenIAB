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
import org.onepf.life2.oms.AppstoreInAppBillingService;
import org.onepf.life2.oms.appstore.googleUtils.IabException;
import org.onepf.life2.oms.appstore.googleUtils.IabHelper;
import org.onepf.life2.oms.appstore.googleUtils.Inventory;
import org.onepf.life2.oms.appstore.googleUtils.Purchase;

import java.util.List;

/**
 * User: Boris Minaev
 * Date: 16.04.13
 * Time: 16:09
 */

public class GooglePlayBillingService implements AppstoreInAppBillingService {
    Context mContext;
    private IabHelper mIabHelper;

    public GooglePlayBillingService(Context context, String publicKey) {
        mContext = context;
        mIabHelper = new IabHelper(context, publicKey);
    }

    @Override
    public void startSetup(IabHelper.OnIabSetupFinishedListener listener, final IabHelperBillingService billingService) {
        mIabHelper.startSetup(listener, billingService);
    }

    @Override
    public void launchPurchaseFlow(Activity act, String sku, String itemType, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        mIabHelper.launchPurchaseFlow(act, sku, itemType, requestCode, listener, extraData);
    }

    @Override
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return mIabHelper.handleActivityResult(requestCode, resultCode, data);
    }

    @Override
    public Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus, List<String> moreSubsSkus) throws IabException {
        return mIabHelper.queryInventory(querySkuDetails, moreItemSkus, moreSubsSkus);
    }

    @Override
    public void consume(Purchase itemInfo) throws IabException {
        mIabHelper.consume(itemInfo);
    }

    @Override
    public void dispose() {
        mIabHelper.dispose();
    }
}
