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
import android.content.pm.PackageManager;
import org.onepf.life2.oms.Appstore;
import org.onepf.life2.oms.AppstoreInAppBillingService;
import org.onepf.life2.oms.AppstoreName;
import org.onepf.life2.oms.AppstoreService;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 16.04.13
 */

public class GooglePlay implements Appstore {
    private Context mContext;
    private GooglePlayBillingService mBillingService;
    private String mPublicKey;
    // isDebugMode = true |-> always returns app installed via Google Play
    private final boolean isDebugMode = true;

    public GooglePlay(Context context, String publicKey) {
        mContext = context;
        mPublicKey = publicKey;
    }

    @Override
    public boolean isAppAvailable(String packageName) {
        return false;
    }

    @Override
    public boolean isInstaller() {
        if (isDebugMode)
            return true;
        PackageManager packageManager = mContext.getPackageManager();
        String packageName = mContext.getClass().getPackage().getName();
        String installerPackageName = packageManager.getInstallerPackageName(packageName);
        return (installerPackageName != null && installerPackageName.equals("com.android.vending"));
    }

    @Override
    public boolean isServiceSupported(AppstoreService appstoreService) {
        return false;
    }

    @Override
    public AppstoreInAppBillingService getInAppBillingService() {
        if (mBillingService == null) {
            mBillingService = new GooglePlayBillingService(mContext, mPublicKey);
        }
        return mBillingService;
    }

    @Override
    public AppstoreName getAppstoreName() {
        return AppstoreName.GOOGLE;
    }
}
