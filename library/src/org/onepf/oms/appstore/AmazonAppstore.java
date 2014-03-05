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
import org.onepf.oms.OpenIabHelper;

import android.content.Context;
import android.util.Log;

/**
 * Analize whether app is installed from Amazon Appstore.
 * <p>
 * Uses {@link #hasAmazonClasses()} to determine it techically
 * 
 * @author Oleg Orlov
 * @since 16.04.13
 */
public class AmazonAppstore extends DefaultAppstore {
    private static final String TAG = AmazonAppstore.class.getSimpleName();
    private static final String AMAZON_INSTALLER = "com.amazon.venezia";

    private static boolean isDebugLog() {
        return OpenIabHelper.isDebugLog();
    }
    
    private volatile Boolean sandboxMode;// = false;
    
    private final Context context;
    
    private AmazonAppstoreBillingService mBillingService;

    public AmazonAppstore(Context context) {
        this.context = context;
    }

    @Override
    public boolean isPackageInstaller(String packageName) {
        if (sandboxMode != null) {
            return !sandboxMode;
        }
        boolean amazonIsInstaller = OpenIabHelper.isPackageInstaller(context, AMAZON_INSTALLER);
        sandboxMode = !amazonIsInstaller && !hasAmazonClasses();
        if (isDebugLog()) Log.d(TAG, "isPackageInstaller() sandBox: " + sandboxMode);
        return !sandboxMode;
    }

    /** 
     * Tries to load Amazon <code>com.amazon.android.Kiwi</code> class.
     * <p>
     * Submitted .apk is not published to Amazon as is. It is re-packed with several Amazon-specific
     * classes. We examine such classes to understand whether app is delivered by Amazon 
     */
    public static boolean hasAmazonClasses() {
        boolean result;
        synchronized (AmazonAppstore.class) {
            try {
                ClassLoader localClassLoader = AmazonAppstore.class.getClassLoader();
                localClassLoader.loadClass("com.amazon.android.Kiwi");
                result = true;
            } catch (Throwable localThrowable) {
                if (isDebugLog()) Log.d(TAG, "hasAmazonClasses() cannot load class com.amazon.android.Kiwi ");
                result = false;
            }
        }
        return result;
    }

    /**
     * Cannot assume any app is published in Amazon, so say YES only if Amazon is installer
     */
    @Override
    public boolean isBillingAvailable(String packageName) {
        return isPackageInstaller(packageName);
    }
    
    @Override
    public int getPackageVersion(String packageName) {
        return Appstore.PACKAGE_VERSION_UNDEFINED;
    }
    
    @Override
    public AppstoreInAppBillingService getInAppBillingService() {
        if (mBillingService == null) {
            mBillingService = new AmazonAppstoreBillingService(context);
        }
        return mBillingService;
    }

    @Override
    public String getAppstoreName() {
        return OpenIabHelper.NAME_AMAZON;
    }
}
