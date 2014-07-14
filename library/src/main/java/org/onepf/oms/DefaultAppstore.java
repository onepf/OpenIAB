package org.onepf.oms;

import android.content.Intent;

public abstract class DefaultAppstore implements Appstore {

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
    
    public String toString() {
        return "Store {name: " + getAppstoreName() + "}";
    }

}
