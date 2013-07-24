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

import android.content.*;
import android.os.RemoteException;
import android.util.Log;
import org.onepf.oms.*;

/**
 * User: Boris Minaev
 * Date: 28.05.13
 * Time: 2:39
 */
public class OpenAppstore extends DefaultAppstore {
    static final private String TAG = OpenAppstore.class.getSimpleName();
    Context mContext;
    IOpenAppstore mIOpenAppstore;
    AppstoreInAppBillingService mBillingService;
    static final private int BILLING_SERVICE = 0;

    public OpenAppstore(IOpenAppstore iOpenAppstore, Context context) {
        mContext = context;
        mIOpenAppstore = iOpenAppstore;
    }

    /**
     * Prepare everything required to bind to remote billing server
     * <p>
     * <b>TODO:</b> do bind service and return success or not
     */
    public boolean initBilling(final String publicKey) {
        final String contextPackageName = getPackageName();
        Intent billingIntent = getServiceIntent(contextPackageName, BILLING_SERVICE);

        if (billingIntent == null) {
            return false;
        }

        boolean isInstaller = isInstaller(contextPackageName);
        Log.d(TAG, "isInstaller: " + String.valueOf(isInstaller));

        boolean couldBeInstaller = couldBeInstaller(contextPackageName);
        Log.d(TAG, "couldBeInstaller: " + String.valueOf(couldBeInstaller));

        mBillingService = new OpenAppstoreBillingService(publicKey, mContext, billingIntent, this);
        return true;
    }

    private String getPackageName() {
        return mContext.getPackageName();
    }

    @Override
    public boolean isAppAvailable(String packageName) {
        try {
            return mIOpenAppstore.isAppAvailable(getPackageName());
        } catch (RemoteException e) {
            Log.w(TAG, "RemoteException: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isInstaller(String packageName) {
        try {
            return mIOpenAppstore.isInstaller(getPackageName());
        } catch (RemoteException e) {
            Log.w(TAG, "RemoteException: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean couldBeInstaller(String packageName) {
        try {
            return mIOpenAppstore.couldBeInstaller(getPackageName());
        } catch (RemoteException e) {
            Log.w(TAG, "RemoteException: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Intent getServiceIntent(String packageName, int serviceType) {
        try {
            return mIOpenAppstore.getServiceIntent(getPackageName(), serviceType);
        } catch (RemoteException e) {
            Log.w(TAG, "RemoteException: " + e.getMessage());
            return null;
        }
    }

    @Override
    public String getAppstoreName() {
        try {
            return mIOpenAppstore.getAppstoreName();
        } catch (RemoteException e) {
            Log.w(TAG, "RemoteException: " + e.getMessage());
            return null;
        }
    }

    @Override
    public Intent getProductPageIntent(String packageName) {
        try {
            return mIOpenAppstore.getProductPageIntent(getPackageName());
        } catch (RemoteException e) {
            Log.w(TAG, "RemoteException: " + e.getMessage());
            return null;
        }
    }

    @Override
    public Intent getRateItPageIntent(String packageName) {
        try {
            return mIOpenAppstore.getRateItPageIntent(getPackageName());
        } catch (RemoteException e) {
            Log.w(TAG, "RemoteException: " + e.getMessage());
            return null;
        }
    }

    @Override
    public Intent getSameDeveloperPageIntent(String packageName) {
        try {
            return mIOpenAppstore.getSameDeveloperPageIntent(getPackageName());
        } catch (RemoteException e) {
            Log.w(TAG, "RemoteException: " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean areOutsideLinksAllowed() {
        try {
            return mIOpenAppstore.areOutsideLinksAllowed();
        } catch (RemoteException e) {
            Log.w(TAG, "RemoteException: " + e.getMessage());
            return false;
        }
    }

    @Override
    public AppstoreInAppBillingService getInAppBillingService() {
        return mBillingService;
    }
}
