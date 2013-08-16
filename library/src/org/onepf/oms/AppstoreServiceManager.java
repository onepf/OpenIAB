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

package org.onepf.oms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.onepf.oms.appstore.OpenAppstore;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 16.04.13
 */
public class AppstoreServiceManager {
    private static final String TAG = AppstoreServiceManager.class.getSimpleName();
    private static final String BIND_INTENT = "org.onepf.oms.openappstore.BIND";
    
    private Context mContext;
    
    private List<Appstore> appstores;
    
    private Map<String, String> storeKeys;
    
    /** Developer preferred store names */
    private String[] prefferedStoreNames = new String[] {};

    /**
     * @deprecated use {@link #AppstoreServiceManager(Context, Map, String[], Appstore[])} instead
     * @param appstores - additional stores to process as well as OpenStores
     */
    public AppstoreServiceManager(Context context, ArrayList<Appstore> appstores, Map<String, String> extra) {
        this(context, extra, null, appstores.toArray(new Appstore[0]));
    }

    /**
     * Main constructor
     * 
     * @param context
     * @param storeKeys - map [ storeName -> publicKey ]
     * @param prefferedStoreNames - will be used if package installer cannot be found
     * @param extraStores - extra stores to participate in store elections
     */
    public AppstoreServiceManager(Context context, Map <String, String> storeKeys, String[] prefferedStoreNames, Appstore[] extraStores) {
        this.mContext = context;
        this.storeKeys = storeKeys;
        this.prefferedStoreNames = prefferedStoreNames != null ? prefferedStoreNames : new String[]{};
        this.appstores = extraStores != null ? new ArrayList<Appstore>(Arrays.asList(extraStores)) : new ArrayList<Appstore>();
    }

    /**
     * Discover all OpenStore services, checks them and build {@link #appstores} list<br>
     * 
     * TODO: better to acquire all necessary params before store election
     * @param listener - called back when all OpenStores collected and analyzed
     */
    void startSetup(final OnInitListener listener) {
        final OnInitListener initListener = listener;
        PackageManager packageManager = mContext.getPackageManager();
        final Intent intentAppstoreServices = new Intent(BIND_INTENT);
        List<ResolveInfo> infoList = packageManager.queryIntentServices(intentAppstoreServices, 0);
        if (infoList.size() == 0) {
            initListener.onInitFinished();
        }

        final CountDownLatch countDownLatch = new CountDownLatch(infoList.size());
        for (ResolveInfo info : infoList) {
            String packageName = info.serviceInfo.packageName;
            String name = info.serviceInfo.name;
            Intent intentAppstore = new Intent(intentAppstoreServices);
            intentAppstore.setClassName(packageName, name);
            mContext.bindService(intentAppstore, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    Log.d(TAG, "appstoresService connected for component: " + name.flattenToShortString());
                    IOpenAppstore openAppstoreService = IOpenAppstore.Stub.asInterface(service);

                    String appstoreName = null;
                    try {
                        appstoreName = openAppstoreService.getAppstoreName();
                    } catch (RemoteException e) {
                        Log.e(TAG, "onServiceConnected() ComponentName: " + name, e);
                    }
                    
                    if (appstoreName == null) { // no name - no service
                        Log.e(TAG, "onServiceConnected() Appstore doesn't have name. Skipped. ComponentName: " + name);
                        countDownLatch.countDown();
                        if (countDownLatch.getCount() == 0) {
                            initListener.onInitFinished();
                        }
                        return;
                    }
                    
                    final OpenAppstore openAppstore = new OpenAppstore(openAppstoreService, mContext);

                    String publicKey = storeKeys.get(appstoreName);

                    if (openAppstore.initBilling(publicKey)) {
                        synchronized (appstores) {
                            Log.d(TAG, "onServiceConnected() add new open store by type: " + openAppstore.getAppstoreName());
                            if (appstores.contains(openAppstore) == false) {
                                appstores.add(openAppstore);
                            }
                        }
                    } else {
                        Log.d(TAG, "onServiceConnected(): billing init failed");
                    }

                    countDownLatch.countDown();
                    if (countDownLatch.getCount() == 0) {
                        initListener.onInitFinished();
                    }

                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    Log.d(TAG, "onServiceDisconnected() appstoresService disconnected for component: " + name.flattenToShortString());
                    //Nothing to do here
                }
            }, Context.BIND_AUTO_CREATE);
        }
    }

    
    /**
     * Lookup for requested service in store based on isPackageInstaller() & isBillingAvailable()
     * <p>
     * Scenario:
     * <li>
     * - look for installer: if exists and supports billing service - we done <li>  
     * - rest of stores who support billing considered as candidates<p><li>
     * 
     * - find candidate according to [prefferedStoreNames]. if found - we done<p><li>
     * 
     * - select candidate randomly from 3 groups based on published package version<li> 
     *   - published version == app.versionCode<li>
     *   - published version  > app.versionCode<li>
     *   - published version < app.versionCode
     * 
     */
    public Appstore selectBillingService() {
        String packageName = mContext.getPackageName();
        // candidates:
        Map<String, Appstore> candidates = new HashMap<String, Appstore>();
        //
        for (Appstore appstore : appstores) {
            if (appstore.isBillingAvailable(packageName)) {
                candidates.put(appstore.getAppstoreName(), appstore);
            } else {
                continue; // for billing we cannot select store without billing
            }
            if (appstore.isPackageInstaller(packageName)) {
                return appstore;
            }
        }
        if (candidates.size() == 0) return null;
        
        // lookup for developer preffered stores
        for (int i = 0; i < prefferedStoreNames.length; i++) {
            Appstore candidate = candidates.get(prefferedStoreNames[i]);
            if (candidate != null) {
                return candidate;
            }
        }
        // nothing found. select something that matches package version
        int versionCode = Appstore.PACKAGE_VERSION_UNDEFINED; 
        try {
            versionCode = mContext.getPackageManager().getPackageInfo(packageName, 0).versionCode;
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Are we installed?", e);
        }
        List<Appstore> sameVersion = new ArrayList<Appstore>();
        List<Appstore> higherVersion = new ArrayList<Appstore>();
        for (Appstore candidate : candidates.values()) {
            final int storeVersion = candidate.getPackageVersion(packageName);
            if (storeVersion == versionCode) {
                 sameVersion.add(candidate);
            } else if (storeVersion > versionCode) {
                higherVersion.add(candidate);
            }
        }
        // use random if found stores with same version of package  
        if (sameVersion.size() > 0) {
            return sameVersion.get(new Random().nextInt(sameVersion.size()));
        } else if (higherVersion.size() > 0) {  // or one of higher version
            return higherVersion.get(new Random().nextInt(higherVersion.size()));
        } else {                                // ok, return no matter what
            return new ArrayList<Appstore>(candidates.values()).get(new Random().nextInt(candidates.size())); 
        }
    }
    
    
    public interface OnInitListener {
        public void onInitFinished();
    }

}
