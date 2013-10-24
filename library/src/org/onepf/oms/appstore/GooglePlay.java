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

import java.util.List;

import org.onepf.oms.Appstore;
import org.onepf.oms.AppstoreInAppBillingService;
import org.onepf.oms.DefaultAppstore;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.IabHelper;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 16.04.13
 */

public class GooglePlay extends DefaultAppstore {
    private static final String TAG = GooglePlay.class.getSimpleName();

    public  static final String ANDROID_INSTALLER = "com.android.vending";
    private static final String GOOGLE_INSTALLER = "com.google.vending";
    public  static final String VENDING_ACTION = "com.android.vending.billing.InAppBillingService.BIND";
    
    private Context mContext;
    private IabHelper mBillingService;
    private String mPublicKey;
    
    // isDebugMode = true |-> always returns app installed via Google Play
    private final boolean isDebugMode = false;

    public GooglePlay(Context context, String publicKey) {
        mContext = context;
        mPublicKey = publicKey;
    }

    @Override
    public boolean isPackageInstaller(String packageName) {
        if (isDebugMode) {
            return true;
        }
        PackageManager packageManager = mContext.getPackageManager();
        String installerPackageName = packageManager.getInstallerPackageName(packageName);
        return (installerPackageName != null && installerPackageName.equals(ANDROID_INSTALLER));
    }
    
    /**
     * Assume Android app is published in Google Play in any case. 
     * 
     * @return true if Google Play is installed in the system   
     */
    @Override    
    public boolean isBillingAvailable(String packageName) {
        Log.d(TAG, "isBillingAvailable() packageName: " + packageName);
        PackageManager packageManager = mContext.getPackageManager();
        List<PackageInfo> allPackages = packageManager.getInstalledPackages(0);
        for (PackageInfo packageInfo : allPackages) {
            if (packageInfo.packageName.equals(GOOGLE_INSTALLER) || packageInfo.packageName.equals(ANDROID_INSTALLER)) {
                Log.d(TAG, "Google supports billing");
                return true;
            }
        }
        return false;
    }

    @Override
    public int getPackageVersion(String packageName) {
        return Appstore.PACKAGE_VERSION_UNDEFINED;
    }

    @Override
    public AppstoreInAppBillingService getInAppBillingService() {
        if (mBillingService == null) {
            mBillingService = new IabHelper(mContext, mPublicKey, this);
        }
        return mBillingService;
    }

    @Override
    public String getAppstoreName() {
        return OpenIabHelper.NAME_GOOGLE;
    }

}
