package org.onepf.life2.oms.appstore;

import android.content.Context;
import org.onepf.life2.oms.Appstore;
import org.onepf.life2.oms.AppstoreInAppBillingService;
import org.onepf.life2.oms.AppstoreName;
import org.onepf.life2.oms.AppstoreService;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 16.05.13
 */
public class TStore implements Appstore {
    private final Context mContext;
    private TStoreBillingService mBillingService;
    private String mAppId;

    public TStore(Context context, String appId) {
        mContext = context;
        mAppId = appId;
    }

    @Override
    public boolean isAppAvailable(String packageName) {
        // TODO: implement this
        return false;
    }

    @Override
    public boolean isInstaller() {
        // TODO: implement this
        return false;
    }

    @Override
    public boolean isServiceSupported(AppstoreService appstoreService) {
        if (appstoreService == AppstoreService.IN_APP_BILLING) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AppstoreInAppBillingService getInAppBillingService() {
        if (mBillingService == null) {
            mBillingService = new TStoreBillingService(mContext);
        }
        return mBillingService;
    }

    @Override
    public AppstoreName getAppstoreName() {
        return AppstoreName.TSTORE;
    }
}
