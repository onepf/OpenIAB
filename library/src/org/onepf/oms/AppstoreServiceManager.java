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
        final OnAppstoreServiceManagerInitFinishedListener mListener = listener;
        PackageManager packageManager = mContext.getPackageManager();
        Intent intentAppstoreServices = new Intent(BIND_INTENT);

        List<ResolveInfo> infoList = packageManager.queryIntentServices(intentAppstoreServices, 0);
        if (infoList.size() == 0) {
            mListener.onAppstoreServiceManagerInitFinishedListener();
        }
        final CountDownLatch countDownLatch = new CountDownLatch(infoList.size());
        for (ResolveInfo info : infoList) {
            String packageName = info.serviceInfo.packageName;
            String name = info.serviceInfo.name;
            Intent intentAppstore = new Intent();
            intentAppstore.setClassName(packageName, name);
            mContext.bindService(intentAppstore, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    IOpenAppstore openAppstoreService = IOpenAppstore.Stub.asInterface(service);
                    String appstoreName = null;
                    try {
                        appstoreName = openAppstoreService.getAppstoreName();
                       } catch (RemoteException e) {
                        Log.e(TAG, "RemoteException: " + e.getMessage());
                    }
                    String publicKey = mExtra.get(appstoreName);
                    final OpenAppstore openAppstore = new OpenAppstore(openAppstoreService, mContext, appstoreName);
                    openAppstore.startSetup(publicKey, new OnAppstoreStartSetupFinishListener() {
                        @Override
                        public void onAppstoreStartSetupFinishListener(boolean isOk) {
                            synchronized (appstores) {
                                appstores.add(openAppstore);
                            }
                            countDownLatch.countDown();
                            if (countDownLatch.getCount() == 0) {
                                listener.onAppstoreServiceManagerInitFinishedListener();
                            }
                        }
                    });
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
