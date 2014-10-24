/*
 * Copyright 2012-2014 One Platform Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onepf.oms.appstore;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.onepf.oms.*;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.util.Logger;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.vending.billing.IInAppBillingService;

/**
 * @author Boris Minaev, Oleg Orlov
 * @since 28.05.13
 */
public class OpenAppstore extends DefaultAppstore {

    private Context context;
    private ServiceConnection serviceConn;
    private IOpenAppstore openAppstoreService;
    @Nullable
    private AppstoreInAppBillingService mBillingService;

    /**
     * id of OpenStore
     */
    private final String appstoreName;

    /**
     * for debug purposes
     */
    public ComponentName componentName;

    /**
     * @param publicKey - used for signature verification. If <b>null</b> verification is disabled
     */
    public OpenAppstore(@NotNull Context context, String appstoreName, IOpenAppstore openAppstoreService, @Nullable final Intent billingIntent, String publicKey, ServiceConnection serviceConn) {
        this.context = context;
        this.appstoreName = appstoreName;
        this.openAppstoreService = openAppstoreService;
        this.serviceConn = serviceConn;
        if (billingIntent != null) {
            this.mBillingService = new IabHelper(context, publicKey, this) {
                @Nullable
                @Override
                protected Intent getServiceIntent() {
                    return billingIntent;
                }

                @Nullable
                @Override
                protected IInAppBillingService getServiceFromBinder(IBinder service) {
                    return new IOpenInAppBillingWrapper(IOpenInAppBillingService.Stub.asInterface(service));
                }

                @Override
                public void dispose() {
                    super.dispose();
                    OpenAppstore.this.context.unbindService(OpenAppstore.this.serviceConn);
                }

            };
        }
    }

    @Override
    public boolean isPackageInstaller(String packageName) {
        try {
            return openAppstoreService.isPackageInstaller(packageName);
        } catch (RemoteException e) {
            Logger.w("RemoteException: ", e);
            return false;
        }
    }

    @Override
    public boolean isBillingAvailable(String packageName) {
        try {
            return openAppstoreService.isBillingAvailable(packageName);
        } catch (RemoteException e) {
            Logger.e(e, "isBillingAvailable() packageName: ", packageName);
            return false;
        }
    }

    @Override
    public int getPackageVersion(String packageName) {
        try {
            return openAppstoreService.getPackageVersion(packageName);
        } catch (RemoteException e) {
            Logger.e(e, "getPackageVersion() packageName: ", packageName);
            return Appstore.PACKAGE_VERSION_UNDEFINED;
        }
    }

    @Override
    public String getAppstoreName() {
        return appstoreName;
    }

    @Nullable
    @Override
    public Intent getProductPageIntent(String packageName) {
        try {
            return openAppstoreService.getProductPageIntent(packageName);
        } catch (RemoteException e) {
            Logger.w("RemoteException: ", e);
            return null;
        }
    }

    @Nullable
    @Override
    public Intent getRateItPageIntent(String packageName) {
        try {
            return openAppstoreService.getRateItPageIntent(packageName);
        } catch (RemoteException e) {
            Logger.w("RemoteException", e);
            return null;
        }
    }

    @Nullable
    @Override
    public Intent getSameDeveloperPageIntent(String packageName) {
        try {
            return openAppstoreService.getSameDeveloperPageIntent(packageName);
        } catch (RemoteException e) {
            Logger.w("RemoteException", e);
            return null;
        }
    }

    @Override
    public boolean areOutsideLinksAllowed() {
        try {
            return openAppstoreService.areOutsideLinksAllowed();
        } catch (RemoteException e) {
            Logger.w("RemoteException", e);
            return false;
        }
    }

    @Nullable
    @Override
    public AppstoreInAppBillingService getInAppBillingService() {
        return mBillingService;
    }

    @NotNull
    public String toString() {
        return "OpenStore {name: " + appstoreName + ", component: " + componentName + "}";
    }

    /**
     * Represent {@link IOpenInAppBillingService} as {@link IInAppBillingService}
     */
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
