package org.onepf.life2.oms;

/**
 * User: Boris Minaev
 * Date: 29.04.13
 * Time: 17:28
 */

import android.content.Intent;
import android.os.IBinder;

interface IOpenAppstore {
    boolean isAppAvailable(String packageName);

    boolean isInstaller(String packageName);

    boolean isIabServiceSupported(String packageName);

    Intent getInAppBillingServiceIntent();

    String getAppstoreName();

}