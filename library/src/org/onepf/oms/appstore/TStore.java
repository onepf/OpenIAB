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

import android.content.Context;

/**
 * TODO: implement TStore.isInstaller if possible
 * TODO: implement TStore.isBillingAvailable
 * 
 * Author: Ruslan Sayfutdinov
 * Date: 16.05.13
 */
public class TStore extends DefaultAppstore {
    private final Context mContext;
    private TStoreBillingService mBillingService;
    private String mAppId;

    public TStore(Context context, String appId) {
        mContext = context;
        mAppId = appId;
    }

    @Override
    public boolean isPackageInstaller(String packageName) {
        // TODO: implement TStore.isInstaller if possible
        return false;
    }

    @Override
    public boolean isBillingAvailable(String packageName) {
        // TODO: implement TStore.isBillingAvailable
        return false;
    }
    
    @Override
    public int getPackageVersion(String packageName) {
        return Appstore.PACKAGE_VERSION_UNDEFINED;
    }
    
    @Override
    public AppstoreInAppBillingService getInAppBillingService() {
        if (mBillingService == null) {
            mBillingService = new TStoreBillingService(mContext, mAppId);
        }
        return mBillingService;
    }

    @Override
    public String getAppstoreName() {
        return OpenIabHelper.NAME_TSTORE;
    }


}
