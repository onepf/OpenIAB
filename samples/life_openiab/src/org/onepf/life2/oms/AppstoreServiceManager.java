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

import android.content.Context;
import org.onepf.life2.oms.appstore.AmazonAppstore;
import org.onepf.life2.oms.appstore.GooglePlay;
import org.onepf.life2.oms.appstore.YandexStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 16.04.13
 */
class AppstoreServiceManager {
    List<Appstore> appstores;

    AppstoreServiceManager(Context context, String googlePublicKey) {
        appstores = new ArrayList<Appstore>();
        appstores.add(new GooglePlay(context, googlePublicKey));
        appstores.add(new AmazonAppstore(context));
        appstores.add(new YandexStore(context));
    }

    private static AppstoreServiceManager instance;

    public static AppstoreServiceManager getInstance(Context context, String publicKey) {
        if (instance == null) {
            instance = new AppstoreServiceManager(context, publicKey);
        }
        return instance;
    }

    public Appstore getAppstoreForService(String packageName, AppstoreService appstoreService) {
        return null;
    }

    public Appstore getInstallerAppstore() {
        for (Appstore appstore : appstores) {
            if (appstore.isInstaller()) {
                return appstore;
            }
        }
        return null;
    }

    public List<Appstore> getAppstoresSupportingAPI(String packageName, AppstoreService appstoreService) {
        return null;
    }
}
