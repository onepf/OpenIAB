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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.RemoteException;
import com.android.vending.billing.IInAppBillingService;
import org.onepf.oms.*;

import java.util.List;

/**
 * User: Boris Minaev
 * Date: 28.05.13
 * Time: 2:39
 */
public class OpenAppstore implements Appstore{
    Context mContext;
    IOpenAppstore mIOpenAppstore;
    String mName;
    AppstoreInAppBillingService mBillingService;
    final int BILLING_SERVICE = 0;

    public OpenAppstore(IOpenAppstore iOpenAppstore, Context context, String name) {
        mContext = context;
        mIOpenAppstore = iOpenAppstore;
        mName = name;
    }

    public void startSetup(final String publicKey, final OnAppstoreStartSetupFinishListener listener) {
        try {
            Intent billingIntent = mIOpenAppstore.getServiceIntent(mContext.getPackageName(), BILLING_SERVICE);
            PackageManager packageManager = mContext.getPackageManager();
            List<ResolveInfo> infoList = packageManager.queryIntentServices(billingIntent, 0);
            if (infoList.size() != 1) {
                listener.onAppstoreStartSetupFinishListener(false);
            }
            for (ResolveInfo info : infoList) {
                String packageName = info.serviceInfo.packageName;
                String name = info.serviceInfo.name;
                Intent intentAppstore = new Intent();
                intentAppstore.setClassName(packageName, name);
                mContext.bindService(intentAppstore, new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        IInAppBillingService openInAppBillingService = IInAppBillingService.Stub.asInterface(service);
                        mBillingService = new OpenAppstoreBillingService(openInAppBillingService, publicKey, mContext);
                        listener.onAppstoreStartSetupFinishListener(true);
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        listener.onAppstoreStartSetupFinishListener(false);
                    }
                }, Context.BIND_AUTO_CREATE);
            }
        } catch (RemoteException e) {
            listener.onAppstoreStartSetupFinishListener(false);
        }
    }

    @Override
    public boolean isAppAvailable(String packageName) {
        try {
            return mIOpenAppstore.isAppAvailable(mContext.getPackageName());
        } catch (RemoteException e) {
            return false;
        }
    }

    @Override
    public boolean isInstaller() {
        try {
            return mIOpenAppstore.isInstaller(mContext.getPackageName());
        } catch (RemoteException e) {
            return false;
        }
    }

    @Override
    public boolean isServiceSupported(AppstoreService appstoreService) {
        try {
            return mIOpenAppstore.isServiceSupported(mContext.getPackageName(), BILLING_SERVICE);
        } catch (RemoteException e) {
            return false;
        }
    }

    @Override
    public AppstoreInAppBillingService getInAppBillingService() {
        return mBillingService;
    }

    @Override
    public AppstoreName getAppstoreName() {
        return AppstoreName.OPENSTORE;
    }

    public String getStringName() {
        return mName;
    }
}
