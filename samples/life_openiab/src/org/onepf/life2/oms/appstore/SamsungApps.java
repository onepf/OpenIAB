package org.onepf.life2.oms.appstore;

import android.content.Context;
import org.onepf.life2.oms.Appstore;
import org.onepf.life2.oms.AppstoreInAppBillingService;
import org.onepf.life2.oms.AppstoreName;
import org.onepf.life2.oms.AppstoreService;

/**
 * User: Boris Minaev
 * Date: 22.04.13
 * Time: 12:28
 */
public class SamsungApps implements Appstore {
    private AppstoreInAppBillingService mBillingService;
    private Context mContext;
    private String mItemGroupId;

    // isDebugMode = true -> always returns Samsung Apps is installer
    private final boolean isDebugMode = true;

    public SamsungApps(Context context, String itemGroupId) {
        mContext = context;
        mItemGroupId = itemGroupId;
    }

    @Override
    public boolean isAppPresented(String packageName) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isInstaller() {
        return isDebugMode;
    }

    @Override
    public boolean isServiceSupported(AppstoreService appstoreService) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public AppstoreInAppBillingService getInAppBillingService() {
        if (mBillingService == null) {
            mBillingService = new SamsungAppsBillingService(mContext, mItemGroupId);
        }
        return mBillingService;
    }

    @Override
    public AppstoreName getAppstoreName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
