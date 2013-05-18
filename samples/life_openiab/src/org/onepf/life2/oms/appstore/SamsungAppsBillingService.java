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
import com.samsungapps.plasma.*;
import org.onepf.life2.oms.AppstoreInAppBillingService;
import org.onepf.life2.oms.OpenIabHelper;
import org.onepf.life2.oms.appstore.googleUtils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * User: Boris Minaev
 * Date: 22.04.13
 * Time: 12:29
 */

public class SamsungAppsBillingService implements AppstoreInAppBillingService, PlasmaListener {
    private Plasma mPlasma;

    private int transactionId;
    private Map<Integer, PurchaseInfo> purchases;
    private Map<Integer, Inventory> queryInventorys;
    private CountDownLatch latch;

    private class PurchaseInfo {
        IabHelper.OnIabPurchaseFinishedListener mListener;
        Activity mActivity;
        String mSku;
        String mItemType;
        int mRequestCode;
        String mExtraData;

        public PurchaseInfo(Activity act, String sku, String itemType, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
            mActivity = act;
            mSku = sku;
            mItemType = itemType;
            mRequestCode = requestCode;
            mListener = listener;
            mExtraData = extraData;
        }
    }

    public SamsungAppsBillingService(Context context, String itemGroupId) {
        mPlasma = new Plasma(itemGroupId, (Activity) context);
        mPlasma.setDeveloperFlag(1);
        mPlasma.setPlasmaListener(this);
        transactionId = 0;
        purchases = new HashMap<Integer, PurchaseInfo>();
        queryInventorys = new HashMap<Integer, Inventory>();
    }

    @Override
    public void startSetup(IabHelper.OnIabSetupFinishedListener listener, final IabHelperBillingService billingService) {
        IabResult res = new IabResult(0, "OK");
        listener.onIabSetupFinished(res);
    }

    @Override
    public void launchPurchaseFlow(Activity act, String sku, String itemType, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        PurchaseInfo purchaseInfo = new PurchaseInfo(act, sku, itemType, requestCode, listener, extraData);
        purchases.put(transactionId, purchaseInfo);
        mPlasma.requestPurchaseItem(transactionId++, sku);
    }

    @Override
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return false;
    }

    @Override
    public Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus, List<String> moreSubsSkus) throws IabException {
        mPlasma.requestItemInformationList(transactionId++, 0, Integer.MAX_VALUE);
        latch = new CountDownLatch(1);
        int curTransactionId = transactionId - 1;
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        Inventory res = queryInventorys.get(curTransactionId);
        queryInventorys.remove(curTransactionId);
        return res;
    }

    @Override
    public void consume(Purchase itemInfo) throws IabException {
        // Samsung doesn't support consuming objects
    }

    @Override
    public void onItemInformationListReceived(int transactionId, int statusCode, ArrayList<ItemInformation> itemInformations) {
        latch.countDown();
        if (statusCode != 0) {
            queryInventorys.put(transactionId, null);
        } else {
            Inventory res = new Inventory();
            for (ItemInformation item : itemInformations) {
                SkuDetails skuDetails = new SkuDetails(item.getItemId(), item.getItemName(), item.getItemPriceString());
                res.mSkuMap.put(item.getItemId(), skuDetails);
            }
            queryInventorys.put(transactionId, res);
        }
    }

    @Override
    public void onPurchasedItemInformationListReceived(int i, int i2, ArrayList<PurchasedItemInformation> purchasedItemInformations) {

    }

    @Override
    public void onPurchaseItemInitialized(int transactionId, int statusCode, PurchaseTicket purchaseTicket) {
        if (statusCode != Plasma.STATUS_CODE_SUCCESS) {
            PurchaseInfo purchaseInfo = purchases.get(transactionId);
            if (purchaseInfo != null) {
                purchases.remove(purchaseInfo);
                // TODO: errors
                IabResult iabResult = new IabResult(OpenIabHelper.BILLING_RESPONSE_RESULT_ERROR, "Some error");
                Purchase purchase = null;
                purchaseInfo.mListener.onIabPurchaseFinished(iabResult, purchase);
            }
        }
    }

    @Override
    public void onPurchaseItemFinished(int transactionId, int statusCode, PurchasedItemInformation purchasedItemInformation) {
        PurchaseInfo purchaseInfo = purchases.get(transactionId);
        if (purchaseInfo != null) {
            purchases.remove(purchaseInfo);

            Purchase purchase = new Purchase();
            purchase.setItemType(purchaseInfo.mItemType);
            purchase.setSku(purchaseInfo.mSku);

            IabResult iabResult;
            if (statusCode == Plasma.STATUS_CODE_SUCCESS) {
                iabResult = new IabResult(OpenIabHelper.BILLING_RESPONSE_RESULT_OK, "OK");
            } else {
                // TODO: errors
                iabResult = new IabResult(OpenIabHelper.BILLING_RESPONSE_RESULT_ERROR, "Some error");
            }
            purchaseInfo.mListener.onIabPurchaseFinished(iabResult, purchase);
        }
    }

    @Override
    public void dispose() {
        // TODO: free resources
    }
}
