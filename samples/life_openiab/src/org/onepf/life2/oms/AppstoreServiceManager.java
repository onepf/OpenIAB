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

package org.onepf.life2.oms;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import org.onepf.life2.oms.appstore.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 16.04.13
 */
class AppstoreServiceManager {
    private static final String TAG = "IabHelper";
    List<Appstore> appstores;
    private Context mContext;

    public interface OnAppstoreServiceManagerInitFinishedListener {
        public void onAppstoreServiceManagerInitFinishedListener();
    }


    AppstoreServiceManager(Context context, Map<String, String> extra) {
        mContext = context;
        appstores = new ArrayList<Appstore>();
        appstores.add(new GooglePlay(context, extra.get("GooglePublicKey")));
        appstores.add(new AmazonAppstore(context));
        appstores.add(new SamsungApps(context, extra.get("SamsungGroupId")));
        appstores.add(new TStore(context, extra.get("TStoreAppId")));
        appstores.add(new YandexStore(context, extra.get("YandexPublicKey")));
    }


    void startSetup(OnAppstoreServiceManagerInitFinishedListener listener) {
        final OnAppstoreServiceManagerInitFinishedListener mListener = listener;
        final String myPackageName = mContext.getPackageName();
        PackageManager packageManager = mContext.getPackageManager();
        Intent intentAppstoreServices = new Intent("org.onepf.oms.openappstore.BIND");

        List<ResolveInfo> infoList = packageManager.queryIntentServices(intentAppstoreServices, 0);
        final AppstoresServiceSupport appstoresServiceSupport = new AppstoresServiceSupport(infoList.size());
        if (infoList.size() == 0) {
            mListener.onAppstoreServiceManagerInitFinishedListener();
        }
        for (ResolveInfo info : infoList) {
            String packageName = info.serviceInfo.packageName;
            String name = info.serviceInfo.name;
//            Intent intentAppstore = new Intent();
            intentAppstoreServices.setClassName(packageName, name);
            mContext.bindService(intentAppstoreServices, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    Log.d(TAG, "appstoresService connected for component: " + name.flattenToShortString());
                    IOpenAppstore openAppstoreService = IOpenAppstore.Stub.asInterface(service);
                    boolean isInstaller = false;
                    boolean isSupported = false;
                    Intent iabIntent = null;
                    try {
                        isInstaller = openAppstoreService.isInstaller(myPackageName);
                        isSupported = openAppstoreService.isIabServiceSupported(myPackageName);
                        iabIntent = openAppstoreService.getInAppBillingServiceIntent();
                    } catch (RemoteException e) {
                        Log.e(TAG, "RemoteException: " + e.getMessage());
                    }
                    appstoresServiceSupport.add(isInstaller, isSupported, iabIntent);
                    if (appstoresServiceSupport.isReady()) {
                        ServiceFounder serviceFounder = new ServiceFounder() {
                            @Override
                            public void onServiceFound(Intent intent, boolean installer) {
                                // TODO: add found service to appstores
                                mListener.onAppstoreServiceManagerInitFinishedListener();
                            }

                            @Override
                            public void onServiceNotFound() {

                            }
                        };
                        appstoresServiceSupport.getServiceIntent(serviceFounder);
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    Log.d(TAG, "appstoresService disconnected for component: " + name.flattenToShortString());
                    //Nothing to do here
                }
            }, Context.BIND_AUTO_CREATE);
        }
    }

    private static AppstoreServiceManager instance;

    /*
    public static AppstoreServiceManager getInstance(Context context, String publicKey, String samsungGroupId) {
        if (instance == null) {
            instance = new AppstoreServiceManager(context, publicKey, samsungGroupId);
        }
        return instance;
    }
    */

    public Appstore getAppstoreForService(AppstoreService appstoreService) {
        if (appstoreService == AppstoreService.IN_APP_BILLING) {
            Appstore installer = getInstallerAppstore();
            if (installer != null) {
                Log.d(TAG, "Installer appstore: " + installer.getAppstoreName().name());
                if (installer.isServiceSupported(appstoreService)) {
                    return installer;
                }
            }
            for (Appstore appstore : appstores) {
                if (appstore.isServiceSupported(appstoreService)) {
                    return appstore;
                }
            }
        }
        return null;
    }

    public Appstore getInstallerAppstore() {
        for (Appstore appstore : appstores) {
            if (appstore.isInstaller()) {
                return appstore;
            }
        }
        return null;
    }

    public List<Appstore> getAppstoresSupportingAPI(String packageName, AppstoreService appstoreService) {
        return null;
    }
}
