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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import com.skubit.android.billing.IBillingService;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.onepf.oms.Appstore;
import org.onepf.oms.AppstoreInAppBillingService;
import org.onepf.oms.DefaultAppstore;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.skubitUtils.SkubitIabHelper;
import org.onepf.oms.util.CollectionUtils;
import org.onepf.oms.util.Logger;
import org.onepf.oms.util.Utils;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SkubitAppstore extends DefaultAppstore {

    public static final String SKUBIT_INSTALLER = "com.skubit.android";

    public static final String VENDING_ACTION = "com.skubit.android.billing.IBillingService.BIND";

    public static final int TIMEOUT_BILLING_SUPPORTED = 2000;

    @Nullable
    protected final Context context;

    @Nullable
    protected AppstoreInAppBillingService mBillingService;

    @Nullable
    private volatile Boolean billingAvailable = null;

    protected final boolean isDebugMode = false;

    public SkubitAppstore(@Nullable Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }
        this.context = context;
    }

    public String getInstaller() {
        return SKUBIT_INSTALLER;
    }

    public String getAction() {
        return VENDING_ACTION;
    }

    @Override
    public boolean isPackageInstaller(String packageName) {
        if (isDebugMode) {
            return true;
        }
        return Utils.isPackageInstaller(context, SKUBIT_INSTALLER);
    }

    /**
     * @return true if Skubit is installed in the system
     */
    @Override
    public boolean isBillingAvailable(final String packageName) {
        Logger.d("isBillingAvailable() packageName: ", packageName);
        if (billingAvailable != null) {
            return billingAvailable;
        }

        if (Utils.uiThread()) {
            throw new IllegalStateException("Must no be called from UI thread.");
        }

        if (TextUtils.isEmpty(packageName)) {
            throw new IllegalArgumentException("packageName is null");
        }

        billingAvailable = false;
        if (packageExists(context, SKUBIT_INSTALLER)) {
            final Intent intent = new Intent(getAction());
            intent.setPackage(getInstaller());
            final List<ResolveInfo> infoList = context.getPackageManager().queryIntentServices(intent, 0);
            if (!CollectionUtils.isEmpty(infoList)) {
                final CountDownLatch latch = new CountDownLatch(1);
                final ServiceConnection serviceConnection = new ServiceConnection() {
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        IBillingService mService = IBillingService.Stub.asInterface(service);
                        int response;
                        try {
                            response = mService.isBillingSupported(1, packageName, IabHelper.ITEM_TYPE_INAPP);
                            if (response == IabHelper.BILLING_RESPONSE_RESULT_OK) {
                                billingAvailable = true;
                            } else {
                                Logger.d("isBillingAvailable() Google Play billing unavaiable");
                            }
                        } catch (RemoteException e) {
                            Logger.e("isBillingAvailable() RemoteException while setting up in-app billing", e);
                        } finally {
                            latch.countDown();
                            context.unbindService(this);
                        }
                    }

                    public void onServiceDisconnected(ComponentName name) {/*do nothing*/}
                };
                if (context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)) {
                    try {
                        latch.await(TIMEOUT_BILLING_SUPPORTED, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ignore) {
                    }
                }
                Logger.e("isBillingAvailable() billing is not supported. Initialization error.");
            }
        }
        return billingAvailable;
    }

    @Override
    public int getPackageVersion(String packageName) {
        return Appstore.PACKAGE_VERSION_UNDEFINED;
    }

    @Nullable
    @Override
    public synchronized AppstoreInAppBillingService getInAppBillingService() {
        if (mBillingService == null) {
            mBillingService = new SkubitIabHelper(context, null, this);
        }
        return mBillingService;
    }

    @Override
    public String getAppstoreName() {
        return OpenIabHelper.NAME_SKUBIT;
    }

    private boolean packageExists(@NotNull Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            Logger.d(packageName, " package was not found.");
            return false;
        }
    }

}
