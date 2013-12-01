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

import org.onepf.oms.Appstore;
import org.onepf.oms.AppstoreInAppBillingService;
import org.onepf.oms.DefaultAppstore;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.OpenIabHelper.Options;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

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

    private static final int IAP_SIGNATURE_HASHCODE = 0x7a7eaf4b;
    public static final String IAP_PACKAGE_NAME = "com.sec.android.iap";
    public static final String IAP_SERVICE_NAME = "com.sec.android.iap.service.iapService";

    private AppstoreInAppBillingService mBillingService;
    private Context context;
    private Options options;

    // isDebugMode = true -> always returns Samsung Apps is installer
    static final boolean isDebugMode = false;

    public SamsungApps(Context context, Options options) {
        this.context = context;
        this.options = options;
    }

    @Override
    public boolean isPackageInstaller(String packageName) {
        return isDebugMode; // currently there is no reliable way to understand it
    }

    /**
     * @return true if Samsung Apps is installed in the system
     */
    @Override
    public boolean isBillingAvailable(String packageName) {
        boolean iapInstalled = true;
        try {
            PackageManager pm = context.getPackageManager();
            pm.getApplicationInfo(IAP_PACKAGE_NAME, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            iapInstalled = false;
        }
        if (iapInstalled) {
            try {
                Signature[] signatures = context.getPackageManager().getPackageInfo(IAP_PACKAGE_NAME, PackageManager.GET_SIGNATURES).signatures;
                if (signatures[0].hashCode() != IAP_SIGNATURE_HASHCODE) {
                    iapInstalled = false;
                }
            } catch (Exception e) {
                iapInstalled = false;
            }
        }
//        if (!iapInstalled) {
//            Intent intent = new Intent();
//            intent.setData(Uri.parse("samsungapps://ProductDetail/com.sec.android.iap"));
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
//            mContext.startActivity(intent);
//        }
        return isDebugMode || iapInstalled;
    }

    @Override
    public int getPackageVersion(String packageName) {
        return Appstore.PACKAGE_VERSION_UNDEFINED;
    }

    @Override
    public AppstoreInAppBillingService getInAppBillingService() {
        if (mBillingService == null) {
            mBillingService = new SamsungAppsBillingService(context, options);
        }
        return mBillingService;
    }

    @Override
    public String getAppstoreName() {
        return OpenIabHelper.NAME_SAMSUNG;
    }
}
