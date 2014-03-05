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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.onepf.oms.Appstore;
import org.onepf.oms.AppstoreInAppBillingService;
import org.onepf.oms.DefaultAppstore;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.IabHelper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.vending.billing.IInAppBillingService;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 16.04.13
 */

public class GooglePlay extends DefaultAppstore {
    private static final String TAG = GooglePlay.class.getSimpleName();

    private static boolean isDebugLog() {
        return OpenIabHelper.isDebugLog();
    }

    public  static final String ANDROID_INSTALLER = "com.android.vending";
    private static final String GOOGLE_INSTALLER = "com.google.vending";
    public  static final String VENDING_ACTION = "com.android.vending.billing.InAppBillingService.BIND";
    
    public  static final int TIMEOUT_BILLING_SUPPORTED = 2000;
    
    private Context context;
    private IabHelper mBillingService;
    private String publicKey;
    private volatile Boolean billingAvailable = null; // undefined until isBillingAvailable() is called
    
    // isDebugMode = true |-> always returns app installed via Google Play
    private final boolean isDebugMode = false;

    public GooglePlay(Context context, String publicKey) {
        this.context = context;
        this.publicKey = publicKey;
    }

    @Override
    public boolean isPackageInstaller(String packageName) {
        if (isDebugMode) {
            return true;
        }
        return OpenIabHelper.isPackageInstaller(context, ANDROID_INSTALLER);
    }
    
    /**
     * Assume Android app is published in Google Play in any case. 
     * <ul><li>
     * - check Google Play package is installed<li>
     * - check Google Play Vending service is available<li>
     * - check Google Play Vending supports v3 items TYPE_IN-APP (false if Google Play account doesn't exist)
     * </ul>
     * @return true if Google Play is installed in the system   
     */
    @Override    
    public boolean isBillingAvailable(final String packageName) {
        if (isDebugLog()) Log.d(TAG, "isBillingAvailable() packageName: " + packageName);
        if (billingAvailable != null) return billingAvailable; // return previosly checked result
        billingAvailable = false;
        if (packageExists(context, ANDROID_INSTALLER) || packageExists(context, GOOGLE_INSTALLER)) {
            final Intent intent = new Intent(GooglePlay.VENDING_ACTION);
            intent.setPackage(GooglePlay.ANDROID_INSTALLER);
            if (!context.getPackageManager().queryIntentServices(intent, 0).isEmpty()) {
                final CountDownLatch latch = new CountDownLatch(1);
                context.bindService(intent, new ServiceConnection() {
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        IInAppBillingService mService = IInAppBillingService.Stub.asInterface(service);
                        int response;
                        try {
                            response = mService.isBillingSupported(3, packageName, IabHelper.ITEM_TYPE_INAPP);
                            if (response == IabHelper.BILLING_RESPONSE_RESULT_OK) {
                                billingAvailable = true;
                            } else {
                                if (isDebugLog()) Log.d(TAG, "isBillingAvailable() Google Play billing unavaiable");
                            }
                        } catch (RemoteException e) {
                            Log.e(TAG, "isBillingAvailable() RemoteException while setting up in-app billing", e);
                        } finally {
                            latch.countDown();
                            context.unbindService(this);
                        }
                    }
                    public void onServiceDisconnected(ComponentName name) {/*do nothing*/}
                }, Context.BIND_AUTO_CREATE);
                try {
                    latch.await(TIMEOUT_BILLING_SUPPORTED, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Log.e(TAG, "isBillingAvailable() billing is not supported. Initialization error. ", e);
                }
            }
        }
        return billingAvailable;
    }

    @Override
    public int getPackageVersion(String packageName) {
        return Appstore.PACKAGE_VERSION_UNDEFINED;
    }

    @Override
    public AppstoreInAppBillingService getInAppBillingService() {
        if (mBillingService == null) {
            mBillingService = new IabHelper(context, publicKey, this);
        }
        return mBillingService;
    }

    @Override
    public String getAppstoreName() {
        return OpenIabHelper.NAME_GOOGLE;
    }

    private boolean packageExists(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            if (isDebugLog()) {Log.d(TAG, String.format("%s package was not found.", packageName));}
            return false;
        }
    }

}
