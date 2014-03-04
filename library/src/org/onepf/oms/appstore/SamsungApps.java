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
import android.util.Log;
import org.onepf.oms.Appstore;
import org.onepf.oms.AppstoreInAppBillingService;
import org.onepf.oms.DefaultAppstore;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.OpenIabHelper.Options;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.text.TextUtils;
import org.onepf.oms.appstore.googleUtils.IabException;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Inventory;

import java.util.concurrent.CountDownLatch;

/**
 * <p>
 * {@link #isPackageInstaller(String)} - there is no known reliable way to understand 
 * SamsungApps is installer of Application   
 * If you want SamsungApps to be used for purhases specify it in preffered stores by
 * {@link OpenIabHelper#OpenIabHelper(Context, java.util.Map, String[])} </p>   
 * 
 * Supported purchase details   
 * <pre>
 * PurchaseInfo(type:inapp): {
 *     "orderId"            :TPMTID20131011RUI0515895,    // Samsung's payment id
 *     "packageName"        :org.onepf.trivialdrive,
 *     "productId"          :sku_gas,
 *     "purchaseTime"       :1381508784209,               // time in millis
 *     "purchaseState"      :0,                           // will be always zero
 *     "developerPayload"   :,                            // available only in Purchase which return in OnIabPurchaseFinishedListener and
 *                                                        // in OnConsumeFinishedListener. In other places it's equal empty string
 *     "token"              :3218a5f30dd56ca459b16155a207e8af7b2cfe80a54f2aed846b2bbbd547c400
 * }
 * </pre>
 *
 * @author Ruslan Sayfutdinov
 * @since 10.10.2013
 */
public class SamsungApps extends DefaultAppstore {
    private static final String TAG = SamsungApps.class.getSimpleName();
    private static final String SAMSUNG_INSTALLER = "com.sec.android.app.samsungapps";

    private static final int IAP_SIGNATURE_HASHCODE = 0x7a7eaf4b;
    public static final String IAP_PACKAGE_NAME = "com.sec.android.iap";
    public static final String IAP_SERVICE_NAME = "com.sec.android.iap.service.iapService";

    private AppstoreInAppBillingService billingService;
    private Activity activity;
    private Options options;
    
    // isSamsungTestMode = true -> always returns Samsung Apps is installer and billing is available
    public static boolean isSamsungTestMode;
    private boolean debugLog;
    
    private Boolean isBillingAvailable;

    public SamsungApps(Activity activity, Options options) {
        this.activity = activity;
        this.options = options;
    }

    @Override
    public boolean isPackageInstaller(String packageName) {
        return OpenIabHelper.isPackageInstaller(activity, SAMSUNG_INSTALLER) || isSamsungTestMode;
    }

    /**
     * @return true if Samsung Apps is installed in the system and a returned inventory contains info about the app's skus
     */
    @Override
    public boolean isBillingAvailable(String packageName) {
        if (isBillingAvailable != null) {
            return isBillingAvailable;
        }
        
        if (isSamsungTestMode) {
            if (debugLog) Log.d(TAG, "isBillingAvailable() billing is supported in test mode.");
            isBillingAvailable = true;
            return isBillingAvailable;
        }

        boolean iapInstalled = false;
        
        try {
            PackageManager pm = activity.getPackageManager();
            pm.getApplicationInfo(IAP_PACKAGE_NAME, PackageManager.GET_META_DATA);
            Signature[] signatures = activity.getPackageManager().getPackageInfo(IAP_PACKAGE_NAME, PackageManager.GET_SIGNATURES).signatures;
            if (signatures[0].hashCode() == IAP_SIGNATURE_HASHCODE) {
                iapInstalled = true;
            }
        } catch (Exception e) {
            if (debugLog) Log.d(TAG, "isBillingAvailable() Samsung IAP Service is not installed");
        }

        isBillingAvailable = false;
        if (!iapInstalled) {
            return isBillingAvailable;
        }

        final CountDownLatch mainLatch = new CountDownLatch(1);
        getInAppBillingService().startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(final IabResult result) {
                if (result.isSuccess()) {
                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                Inventory inventory = getInAppBillingService().queryInventory(true, OpenIabHelper.getAllStoreSkus(OpenIabHelper.NAME_SAMSUNG), null);
                                if (inventory.mSkuMap != null && inventory.mSkuMap.size() > 0) {
                                    isBillingAvailable = true;
                                }
                            } catch (IabException e) {
                                Log.e(TAG, "isBillingAvailable() failed", e);
                            } finally {
                                getInAppBillingService().dispose();
                                mainLatch.countDown();
                            }
                        }
                    }).start();
                } else {
                    getInAppBillingService().dispose();
                    mainLatch.countDown();
                }
            }
        });

        try {
            mainLatch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "isBillingAvailable() interrupted", e);
        }

        return isBillingAvailable;
    }

    @Override
    public int getPackageVersion(String packageName) {
        return Appstore.PACKAGE_VERSION_UNDEFINED;
    }

    @Override
    public AppstoreInAppBillingService getInAppBillingService() {
        if (billingService == null) {
            billingService = new SamsungAppsBillingService(activity, options);
        }
        return billingService;
    }

    @Override
    public String getAppstoreName() {
        return OpenIabHelper.NAME_SAMSUNG;
    }

    public static void checkSku(String sku) {
        String[] skuParts = sku.split("/");
        if (skuParts.length != 2) {
            throw new IllegalArgumentException("Samsung SKU must contain ITEM_GROUP_ID and ITEM_ID.");
        }
        for (int i = 0; i < skuParts.length; i++) {
            if (!TextUtils.isDigitsOnly(skuParts[i])) {
                if (i == 0) {
                    throw new IllegalArgumentException("Samsung SKU must contain numeric ITEM_GROUP_ID.");
                } else if (i == 1) {
                    throw new IllegalArgumentException("Samsung SKU must contain numeric ITEM_ID.");
                }
            }
        }
    }
}
