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

import android.app.Activity;
import android.content.Intent;

import org.onepf.oms.appstore.googleUtils.IabException;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.appstore.googleUtils.Purchase;

import java.util.List;

/**
 * 
 * @author Oleg Orlov, Boris Minaev
 * @since 16.04.13
 */
public interface AppstoreInAppBillingService {
    /**
     * Establish connection to service.  
     * 
     * @param listener - called in UI-thread when initialization is completed  
     */
    void startSetup(final IabHelper.OnIabSetupFinishedListener listener);

    void launchPurchaseFlow(Activity act, String sku, String itemType, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener, String extraData);

    boolean handleActivityResult(int requestCode, int resultCode, Intent data);

    Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus, List<String> moreSubsSkus) throws IabException;

    void consume(Purchase itemInfo) throws IabException;

    boolean subscriptionsSupported();

    void dispose();
}
