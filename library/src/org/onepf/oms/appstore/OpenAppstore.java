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
import org.onepf.oms.IOpenInAppBillingService;
import org.onepf.oms.appstore.googleUtils.IabHelper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.vending.billing.IInAppBillingService;

/**
 * 
 * @author Boris Minaev, Oleg Orlov
 * @since 28.05.13
 */
public class OpenAppstore extends DefaultAppstore {
    private static final String TAG = OpenAppstore.class.getSimpleName();
    
    private Context context;
    private IOpenAppstore openAppstoreService;
    private AppstoreInAppBillingService mBillingService;
    
    /** id of OpenStore */
    private final String appstoreName;
    
    /** for debug purposes */
    public  ComponentName componentName;

    /**
     * @param appstoreName TODO
     * @param publicKey - used for signature verification. If <b>null</b> verification is disabled 
     */
    public OpenAppstore(Context context, String appstoreName, IOpenAppstore openAppstoreService, final Intent billingIntent, String publicKey) {
        this.context = context;
        this.appstoreName = appstoreName;
        this.openAppstoreService = openAppstoreService;
        if (billingIntent != null) {
            this.mBillingService = new IabHelper(context, publicKey, this) {
                @Override
                protected Intent getServiceIntent() {
                    return billingIntent;
                }
                @Override
                protected IInAppBillingService getServiceFromBinder(IBinder service) {
                    return new IOpenInAppBillingWrapper(IOpenInAppBillingService.Stub.asInterface(service));
                }
            };
        }
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
        return appstoreName;
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
    
    public String toString() {
        return "OpenStore {name: " + appstoreName + ", component: " + componentName + "}";
        
    }
    
    /** Represent {@link IOpenInAppBillingService} as {@link IInAppBillingService} */
    private static final class IOpenInAppBillingWrapper implements IInAppBillingService {
        private final IOpenInAppBillingService openStoreBilling;

        private IOpenInAppBillingWrapper(IOpenInAppBillingService openStoreBilling) {
            this.openStoreBilling = openStoreBilling;
        }

        @Override
        public IBinder asBinder() {
            return openStoreBilling.asBinder();
        }

        @Override
        public int isBillingSupported(int apiVersion, String packageName, String type) throws RemoteException {
            return openStoreBilling.isBillingSupported(apiVersion, packageName, type);
        }

        @Override
        public Bundle getSkuDetails(int apiVersion, String packageName, String type, Bundle skusBundle) throws RemoteException {
            return openStoreBilling.getSkuDetails(apiVersion, packageName, type, skusBundle);
        }

        @Override
        public Bundle getPurchases(int apiVersion, String packageName, String type, String continuationToken) throws RemoteException {
            return openStoreBilling.getPurchases(apiVersion, packageName, type, continuationToken);
        }

        @Override
        public Bundle getBuyIntent(int apiVersion, String packageName, String sku, String type, String developerPayload) throws RemoteException {
            return openStoreBilling.getBuyIntent(apiVersion, packageName, sku, type, developerPayload);
        }

        @Override
        public int consumePurchase(int apiVersion, String packageName, String purchaseToken) throws RemoteException {
            return openStoreBilling.consumePurchase(apiVersion, packageName, purchaseToken);
        }
    }


}
