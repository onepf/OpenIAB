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
import org.onepf.life2.oms.Appstore;
import org.onepf.life2.oms.AppstoreInAppBillingService;
import org.onepf.life2.oms.AppstoreName;
import org.onepf.life2.oms.AppstoreService;

/**
 * User: Boris Minaev
 * Date: 22.04.13
 * Time: 12:28
 */
public class SamsungApps implements Appstore {
    private AppstoreInAppBillingService mBillingService;
    private Context mContext;
    private String mItemGroupId;

    // isDebugMode = true -> always returns Samsung Apps is installer
    private final boolean isDebugMode = false;

    public SamsungApps(Context context, String itemGroupId) {
        mContext = context;
        mItemGroupId = itemGroupId;
    }

    @Override
    public boolean isAppAvailable(String packageName) {
        // TODO: write implementation
        return false;
    }

    @Override
    public boolean isInstaller() {
        // TODO: write normal checker
        return isDebugMode;
    }

    @Override
    public boolean isServiceSupported(AppstoreService appstoreService) {
        if (appstoreService == AppstoreService.IN_APP_BILLING) {
            return isDebugMode;
        }
        // TODO: write implementation
        return false;
    }

    @Override
    public AppstoreInAppBillingService getInAppBillingService() {
        if (mBillingService == null) {
            mBillingService = new SamsungAppsBillingService(mContext, mItemGroupId);
        }
        return mBillingService;
    }

    @Override
    public AppstoreName getAppstoreName() {
        return AppstoreName.SAMSUNG;
    }
}
