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

import android.content.Context;
import org.onepf.oms.*;

/**
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
    public boolean isInstaller(String packageName) {
        // TODO: implement this
        return false;
    }

    public boolean isServiceSupported(AppstoreService appstoreService) {
        if (appstoreService == AppstoreService.IN_APP_BILLING) {
            return true;
        } else {
            return false;
        }
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
        return "AppstoreName.TSTORE";
    }

    @Override
    public AppstoreType getAppstoreType() {
        return AppstoreType.TSTORE;
    }
}
