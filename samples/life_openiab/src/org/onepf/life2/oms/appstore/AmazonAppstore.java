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
import android.util.Log;
import org.onepf.life2.oms.Appstore;
import org.onepf.life2.oms.AppstoreInAppBillingService;
import org.onepf.life2.oms.AppstoreName;
import org.onepf.life2.oms.AppstoreService;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 16.04.13
 */
public class AmazonAppstore implements Appstore {
    private static final String TAG = "IabHelper";
    private static volatile boolean IS_SANDBOX_MODE;
    private static volatile boolean IS_SANDBOX_MODE_CHECKED;
    private final Context mContext;
    private AmazonAppstoreBillingService mBillingService;

    public AmazonAppstore(Context context) {
        mContext = context;
    }

    @Override
    public boolean isAppAvailable(String packageName) {
        return false;
    }

    @Override
    public boolean isInstaller() {
        if (IS_SANDBOX_MODE_CHECKED) {
            return !IS_SANDBOX_MODE;
        }
        synchronized (AmazonAppstore.class) {
            if (IS_SANDBOX_MODE_CHECKED) {
                return !IS_SANDBOX_MODE;
            }
            try {
                ClassLoader localClassLoader = AmazonAppstore.class.getClassLoader();
                localClassLoader.loadClass("com.amazon.android.Kiwi");
                IS_SANDBOX_MODE = false;
            } catch (Throwable localThrowable) {
                IS_SANDBOX_MODE = true;
            }
            IS_SANDBOX_MODE_CHECKED = true;
        }
        Log.d(TAG, "IS_SANDBOX_MODE: " + IS_SANDBOX_MODE);
        return !IS_SANDBOX_MODE;
        //return mBillingService.getIsInstaller();
    }

    @Override
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
            mBillingService = new AmazonAppstoreBillingService(mContext);
        }
        return mBillingService;
    }

    @Override
    public AppstoreName getAppstoreName() {
        return AppstoreName.AMAZON;
    }
}
