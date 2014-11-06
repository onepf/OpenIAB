/*
 * Copyright 2012-2014 One Platform Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onepf.oms;

import android.content.Intent;

import org.jetbrains.annotations.Nullable;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 16.04.13
 */

public interface Appstore {

    public static final int PACKAGE_VERSION_UNDEFINED = -1;

    /**
     * Checks whether the store is the package installer of an application with the specified name.
     *
     * @param packageName The package of the app to test.
     * @return true if the store is the package installer of the application.
     */
    boolean isPackageInstaller(String packageName);

    /**
     * Checks if billing is supported for an application with the specified name.
     * <br/>
     * Note for Open Store: if any in-app item for the app is published in the store.
     *
     * @param packageName The package name of the application to test.
     * @return true if billing is supported for the specified package.
     */
    boolean isBillingAvailable(String packageName);

    /**
     * Supported only for Open Stores.
     *
     * @param packageName The package of the app to check.
     * @return package version that is available for the current store.
     */
    int getPackageVersion(String packageName);

    /**
     * Returns the unique name for a store. Don't be confused with packages! E.g. name Appland is valid for several stores.
     *
     * @return app store name. E.g. {@link org.onepf.oms.OpenIabHelper#NAME_AMAZON}.
     */
    String getAppstoreName();

    @Nullable
    Intent getProductPageIntent(String packageName);

    @Nullable
    Intent getRateItPageIntent(String packageName);

    @Nullable
    Intent getSameDeveloperPageIntent(String packageName);

    boolean areOutsideLinksAllowed();

    /**
     * @return helper to work with a billing service of the store
     */
    @Nullable
    AppstoreInAppBillingService getInAppBillingService();


}
