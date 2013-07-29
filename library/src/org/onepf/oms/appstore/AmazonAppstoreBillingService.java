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
import android.util.Log;
import com.amazon.inapp.purchasing.Offset;
import com.amazon.inapp.purchasing.PurchasingManager;
import org.onepf.oms.AppstoreInAppBillingService;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.appstore.googleUtils.Purchase;

import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 16.04.13
 */
public class AmazonAppstoreBillingService implements AppstoreInAppBillingService {
    private static final String TAG = AmazonAppstoreBillingService.class.getSimpleName();
    
    private final Context mContext;
    private Map<String, IabHelper.OnIabPurchaseFinishedListener> mRequestListeners;
    private String mCurrentUser;
    private Inventory mInventory;
    private IabHelper.OnIabSetupFinishedListener setupListener;

    private CountDownLatch mInventoryRetrived;

    public AmazonAppstoreBillingService(Context context) {
        mContext = context;
        mRequestListeners = new HashMap<String, IabHelper.OnIabPurchaseFinishedListener>();
    }

    @Override
    public void startSetup(IabHelper.OnIabSetupFinishedListener listener) {
        AmazonAppstoreObserver purchasingObserver = new AmazonAppstoreObserver(mContext, this);
        PurchasingManager.registerObserver(purchasingObserver);
        PurchasingManager.initiateGetUserIdRequest();
        this.setupListener = listener;
    }

    @Override
    public void launchPurchaseFlow(Activity act, String sku, String itemType, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        String requestId = PurchasingManager.initiatePurchaseRequest(sku);
        storeRequestListener(requestId, listener);
    }

    @Override
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return false;
    }

    @Override
    public Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus, List<String> moreSubsSkus) {
        Log.d(TAG, "Amazon queryInventory()");
        mInventory = new Inventory();
        mInventoryRetrived = new CountDownLatch(1);
        PurchasingManager.initiatePurchaseUpdatesRequest(Offset.BEGINNING);
        try {
            mInventoryRetrived.await();
        } catch (InterruptedException e) {
            return null;
        }
        if (querySkuDetails) {
            Set<String> querySkus = new HashSet<String>(mInventory.getAllOwnedSkus());
            if (moreItemSkus != null) {
                querySkus.addAll(moreItemSkus);
            }
            if (moreSubsSkus != null) {
                querySkus.addAll(moreSubsSkus);
            }
            if (querySkus.size() > 0) {
                mInventoryRetrived = new CountDownLatch(1);
                PurchasingManager.initiateItemDataRequest(querySkus);
                try {
                    mInventoryRetrived.await();
                } catch (InterruptedException e) {
                    Log.w(TAG, "Amazon SkuDetails fetching interrupted");
                }
            }
        }
        Log.d(TAG, "Amazon queryInventory finished. Inventory size: " + mInventory.getAllOwnedSkus().size());
        return mInventory;
    }

    @Override
    public void consume(Purchase itemInfo) {
        // Nothing to do here
    }

    @Override
    public void dispose() {
        // TODO: free resources
    }

    private void storeRequestListener(String requestId, IabHelper.OnIabPurchaseFinishedListener listener) {
        mRequestListeners.put(requestId, listener);
    }

    public IabHelper.OnIabPurchaseFinishedListener getRequestListener(String requestId) {
        return mRequestListeners.get(requestId);
    }

    public void setCurrentUser(String currentUser) {
        this.mCurrentUser = currentUser;
        if (setupListener != null) {
            setupListener.onIabSetupFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, "Setup successful."));
            setupListener = null;
        }
    }

    public String getCurrentUser() {
        return mCurrentUser;
    }

    public Inventory getInventory() {
        return mInventory;
    }

    public CountDownLatch getInventoryLatch() {
        return mInventoryRetrived;
    }
}
