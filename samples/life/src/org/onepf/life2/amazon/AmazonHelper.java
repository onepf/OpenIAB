/*******************************************************************************
 * Copyright 2013 One Platform Foundation
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 ******************************************************************************/

package org.onepf.life2.amazon;

import com.amazon.inapp.purchasing.PurchasingManager;
import org.onepf.life2.*;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 25.03.13
 */
public class AmazonHelper extends BasePurchaseHelper {
    GameActivity parent;
    BillingHelper mBillingHelper;

    private final static int PRIORITY = 30;

    public AmazonHelper(GameActivity parent, BillingHelper billingHelper) {
        this.parent = parent;
        mBillingHelper = billingHelper;
        PurchasingObserver purchasingObserver = new PurchasingObserver(parent, billingHelper, this);
        PurchasingManager.registerObserver(purchasingObserver);
    }

    @Override
    public void onBuyChanges() {
        String requestId = PurchasingManager.initiatePurchaseRequest(parent.getResources().getString(
                R.string.consumable_sku));
        parent.storeRequestId(requestId, GameActivity.CHANGES);
    }

    @Override
    public void onBuyOrangeCells() {
        String requestId = PurchasingManager.initiatePurchaseRequest(parent.getResources().getString(
                R.string.subscription_sku));
        parent.storeRequestId(requestId, GameActivity.ORANGE_CELLS);
    }

    @Override
    public void onBuyFigures() {
        String requestId = PurchasingManager.initiatePurchaseRequest(parent.getResources().getString(
                R.string.figures_sku));
        parent.storeRequestId(requestId, GameActivity.FIGURES);
    }

    @Override
    public Market getMarket() {
        return Market.AMAZON_APP_STORE;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }
}
