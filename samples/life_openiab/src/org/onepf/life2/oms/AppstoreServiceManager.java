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
import android.util.Log;
import org.onepf.life2.oms.appstore.AmazonAppstore;
import org.onepf.life2.oms.appstore.GooglePlay;
import org.onepf.life2.oms.appstore.SamsungApps;

import java.util.ArrayList;
import java.util.List;

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


    AppstoreServiceManager(Context context, String googlePublicKey, String samsungGroupId) {
        mContext = context;
        appstores = new ArrayList<Appstore>();
        appstores.add(new GooglePlay(context, googlePublicKey));
        appstores.add(new AmazonAppstore(context));
        appstores.add(new SamsungApps(context, samsungGroupId));
    }

    ;

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
            Intent intentAppstore = new Intent();
            intentAppstore.setClassName(packageName, name);
            mContext.bindService(intentAppstore, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    IOpenAppstore openAppstoreService = IOpenAppstore.Stub.asInterface(service);
                    boolean isInstaller = openAppstoreService.isInstaller(myPackageName);
                    boolean isSupported = openAppstoreService.isIabServiceSupported(myPackageName);
                    Intent iabIntent = openAppstoreService.getInAppBillingServiceIntent();
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
