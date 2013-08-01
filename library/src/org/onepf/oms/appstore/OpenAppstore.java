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
import org.onepf.oms.IOpenAppstore;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;

/**
 * 
 * 
 * @author Boris Minaev, Oleg Orlov
 * @since 28.05.13
 */
public class OpenAppstore extends DefaultAppstore {
    static final private String TAG = OpenAppstore.class.getSimpleName();
    
    private Context context;
    private IOpenAppstore openAppstoreService;
    private AppstoreInAppBillingService mBillingService;

    public OpenAppstore(IOpenAppstore openAppstoreService, Context context) {
        this.context = context;
        this.openAppstoreService = openAppstoreService;
    }

    /**
     * Prepare everything required to bind to remote billing service
     * <p>
     * <b>TODO:</b> not just prepare, but do bind service and return success or not
     */
    public boolean initBilling(final String publicKey) {
        Intent billingIntent = null;
        try {
            billingIntent = openAppstoreService.getBillingServiceIntent();
        } catch (RemoteException e) {
            Log.e(TAG, "initBilling() Cannot get intent: ", e);
            return false;
        }

        if (billingIntent == null) {
            return false;
        }

        boolean isInstaller = isPackageInstaller(context.getPackageName());
        Log.d(TAG, "isInstaller: " + String.valueOf(isInstaller));

        mBillingService = new OpenAppstoreBillingService(publicKey, context, billingIntent, this);
        return true;
    }

    @Override
    public boolean isPackageInstaller(String packageName) {
        try {
            return openAppstoreService.isPackageInstaller(packageName);
        } catch (RemoteException e) {
            Log.w(TAG, "RemoteException: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isBillingAvailable(String packageName) {
        try {
            return openAppstoreService.isBillingAvailable(packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "isBillingAvailable() packageName: " + packageName, e);
            return false;
        }
    }

    @Override
    public int getPackageVersion(String packageName) {
        try {
            return openAppstoreService.getPackageVersion(packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "getPackageVersion() packageName: " + packageName, e);
            return Appstore.PACKAGE_VERSION_UNDEFINED;
        }
    }

    @Override
    public String getAppstoreName() {
        try {
            return openAppstoreService.getAppstoreName();
        } catch (RemoteException e) {
            Log.e(TAG, "getAppstoreName() AppstoreName is unavailable", e);
            return null;
        }
    }

    @Override
    public Intent getProductPageIntent(String packageName) {
        try {
            return openAppstoreService.getProductPageIntent(packageName);
        } catch (RemoteException e) {
            Log.w(TAG, "RemoteException: " + e.getMessage());
            return null;
        }
    }

    @Override
    public Intent getRateItPageIntent(String packageName) {
        try {
            return openAppstoreService.getRateItPageIntent(packageName);
        } catch (RemoteException e) {
            Log.w(TAG, "RemoteException: " + e.getMessage());
            return null;
        }
    }

    @Override
    public Intent getSameDeveloperPageIntent(String packageName) {
        try {
            return openAppstoreService.getSameDeveloperPageIntent(packageName);
        } catch (RemoteException e) {
            Log.w(TAG, "RemoteException: " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean areOutsideLinksAllowed() {
        try {
            return openAppstoreService.areOutsideLinksAllowed();
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
