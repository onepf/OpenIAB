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
 * Service interface to implement by OpenStore implementation
 * 
 * @author Boris Minaev, Oleg Orlov
 * @since 29.04.2013
 */
interface IOpenAppstore {

    /**
     * Every OpenStore implementation must provide their name. It's required for core OpenIAB functions 
     */
    String getAppstoreName();
    
    /**
     * OpenStores must provide information about packages it installed. If OpenStore is installer 
     * and supports In-App billing it will be used for purchases
     */
    boolean isPackageInstaller(String packageName);
    
    /**
     * If <b>true</b> OpenIAB assumes In-App items (SKU) for app are published and ready to use
     */
    boolean isBillingAvailable(String packageName);
    
    /**
     * Provides android:versionCode of .apk published in OpenStore
     * @return -1 if UNDEFINED
     */
    int getPackageVersion(String packageName);

    /**
     * Should provide Intent to be used for binding IOpenInAppBillingService
     */
    Intent getBillingServiceIntent();

    Intent getProductPageIntent(String packageName);

    Intent getRateItPageIntent(String packageName);

    Intent getSameDeveloperPageIntent(String packageName);
    
    boolean areOutsideLinksAllowed();
}
