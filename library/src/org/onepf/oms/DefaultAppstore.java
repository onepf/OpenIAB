package org.onepf.oms;

import android.content.Intent;

public class DefaultAppstore implements Appstore {
    @Override
    public boolean isAppAvailable(String packageName) {
        return false;
    }

    @Override
    public boolean isInstaller(String packageName) {
        return false;
    }

    @Override
    public boolean couldBeInstaller(String packageName) {
        return false;
    }

    @Override
    public Intent getServiceIntent(String packageName, int serviceType) {
        return null;
    }

    @Override
    public String getAppstoreName() {
        return null;
    }

    @Override
    public Intent getProductPageIntent(String packageName) {
        return null;
    }

    @Override
    public Intent getRateItPageIntent(String packageName) {
        return null;
    }

    @Override
    public Intent getSameDeveloperPageIntent(String packageName) {
        return null;
    }

    @Override
    public boolean areOutsideLinksAllowed() {
        return false;
    }

    @Override
    public AppstoreInAppBillingService getInAppBillingService() {
        return null;
    }

    @Override
    public AppstoreType getAppstoreType() {
        return AppstoreType.OPENSTORE;
    }
}
