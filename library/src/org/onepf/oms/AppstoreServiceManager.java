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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import org.onepf.oms.appstore.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 16.04.13
 */
class AppstoreServiceManager {
    private static final String TAG = "IabHelper";
    private static final String BIND_INTENT = "org.onepf.oms.openappstore.BIND";
    List<Appstore> appstores;
    Map<String, String> mExtra;
    private Context mContext;

    public interface OnAppstoreServiceManagerInitFinishedListener {
        public void onAppstoreServiceManagerInitFinishedListener();
    }

    AppstoreServiceManager(Context context, Map<String, String> extra) {
        mContext = context;
        mExtra = extra;
        appstores = new ArrayList<Appstore>();
        appstores.add(new GooglePlay(context, extra.get("GooglePublicKey")));
        appstores.add(new AmazonAppstore(context));
        appstores.add(new SamsungApps(context, extra.get("SamsungGroupId")));
        appstores.add(new TStore(context, extra.get("TStoreAppId")));
    }


    void startSetup(final OnAppstoreServiceManagerInitFinishedListener listener) {
        final OnAppstoreServiceManagerInitFinishedListener initListener = listener;
        PackageManager packageManager = mContext.getPackageManager();
        final Intent intentAppstoreServices = new Intent(BIND_INTENT);
        List<ResolveInfo> infoList = packageManager.queryIntentServices(intentAppstoreServices, 0);
        if (infoList.size() == 0) {
            initListener.onAppstoreServiceManagerInitFinishedListener();
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

                    final OpenAppstore openAppstore = new OpenAppstore(openAppstoreService, mContext);

                    String appstoreName = null;
                    try {
                        appstoreName = openAppstoreService.getAppstoreName();
                    } catch (RemoteException e) {
                        Log.w(TAG, "RemoteException: " + e.getMessage());
                    }
                    String publicKey = mExtra.get(appstoreName);

                    openAppstore.startSetup(publicKey, new OnAppstoreStartSetupFinishListener() {
                        @Override
                        public void onAppstoreStartSetupFinishListener(boolean isOk) {
                            Log.d(TAG, "onAppstoreStartSetupFinishListener: " + String.valueOf(isOk));
                            if (isOk == true) {
                                synchronized (appstores) {
                                    Log.d(TAG, "add new open store by type: " + openAppstore.getAppstoreName());
                                    if (appstores.contains(openAppstore) == false) {
                                        appstores.add(openAppstore);
                                    }
                                }
                            }
                            countDownLatch.countDown();
                            if (countDownLatch.getCount() == 0) {
                                initListener.onAppstoreServiceManagerInitFinishedListener();
                            }
                        }
                    });
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    Log.d(TAG, "appstoresService disconnected for component: " + name.flattenToShortString());
                    //Nothing to do here
                }
            }, Context.BIND_AUTO_CREATE);
        }
    }

    public Appstore getAppstoreForService(AppstoreService appstoreService) {
        //TODO: implement logic to choose app store.
        String packageName = mContext.getPackageName();

        if (appstoreService == AppstoreService.IN_APP_BILLING) {
            Appstore installer = getInstallerAppstore();
            if (installer != null) {
                Log.d(TAG, "Installer appstore: " + installer.getAppstoreName());
                Intent inappIntent = installer.getServiceIntent(packageName, 0);
                if (inappIntent != null) {
                    return installer;
                }
            }

            synchronized (appstores) {
                for (Appstore appstore : appstores) {
                    Intent inappIntent = installer.getServiceIntent(packageName, 0);
                    if (inappIntent != null) {
                        return appstore;
                    }
                }
            }
        }
        return null;
    }

    public Appstore getInstallerAppstore() {
        //TODO: implement logic to choose app store.
        String packageName = mContext.getPackageName();
        Appstore returnAppstore = null;
        synchronized (appstores) {
            for (Appstore appstore : appstores) {
                if (appstore.isInstaller(packageName)) {
                    return appstore;
                }
                // return last appstore if no one is selected (debug mode)
                returnAppstore = appstore;
            }
            for (Appstore appstore : appstores) {
                if (appstore.couldBeInstaller(packageName)) {
                    return appstore;
                }
                // return last appstore if no one is selected (debug mode)
                returnAppstore = appstore;
            }
        }
        if (returnAppstore != null) {
            Log.w(TAG, "getInstallerAppstore returns random appstore");
        }
        return returnAppstore;
    }
}
