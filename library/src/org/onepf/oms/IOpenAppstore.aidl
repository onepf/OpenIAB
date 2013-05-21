package org.onepf.oms;

/**
 * User: Boris Minaev
 * Date: 29.04.13
 * Time: 17:28
 */

import android.content.Intent;

interface IOpenAppstore {
    boolean isAppAvailable(String packageName);

    boolean isInstaller(String packageName);

    Intent getServiceIntent(String packageName, int serviceType);

    String getAppstoreName();

    Intent getProductPageIntent(String packageName);

    Intent getRateItPageIntent(String packageName);

    Intent getSameDeveloperPageIntent(String packageName);
    
    boolean areOutsideLinksAllowed();
}