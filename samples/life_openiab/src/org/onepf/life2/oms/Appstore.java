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

package org.onepf.life2.oms;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 16.04.13
 */

public interface Appstore {
    boolean isAppAvailable(String packageName);

    boolean isInstaller();

    boolean isServiceSupported(AppstoreService appstoreService);

    AppstoreInAppBillingService getInAppBillingService();

    AppstoreName getAppstoreName();

    //... other methods that return different Appstore specific services

    //a method to open application product page in this appstore
    //a method to open the reviews page (aka "rate it") in this appstore
    //a method that returns if the appstore allows links to developer website
}
