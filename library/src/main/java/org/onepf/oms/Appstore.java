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

import android.content.Intent;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 16.04.13
 */

public interface Appstore {

    /**
     * If Appstore cannot determine version of published app on it's server
     */
    public static final int PACKAGE_VERSION_UNDEFINED = -1;

    /**
     * Returns true only if actual installer for specified app
     */
    boolean isPackageInstaller(String packageName);
    
    /**
     * Tells whether in-app billing is ready to work with specified app
     * For OpenStore app: if any in-app item for this app published in store
     */
    boolean isBillingAvailable(String packageName);

    /**
     * Returns <code>android:versionCode</code> package. If there are several builds for one package
     * versionCode of the most approptiate to be used  
     */
    int getPackageVersion(String packageName);
    
    String getAppstoreName();

    Intent getProductPageIntent(String packageName);

    Intent getRateItPageIntent(String packageName);

    Intent getSameDeveloperPageIntent(String packageName);

    boolean areOutsideLinksAllowed();

    /**
     * @return helper to interact with store billing service and perform purchases
     */
    AppstoreInAppBillingService getInAppBillingService();


}
