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

package org.onepf.life2.samsung;

import android.content.Context;
import android.content.SharedPreferences;
import com.samsungapps.plasma.*;
import org.onepf.life2.BasePurchaseHelper;
import org.onepf.life2.BillingHelper;
import org.onepf.life2.GameActivity;
import org.onepf.life2.Market;

import java.util.ArrayList;

/**
 * User: Boris Minaev
 * Date: 03.04.13
 * Time: 15:54
 */

public class SamsungHelper extends BasePurchaseHelper implements PlasmaListener {
    private final GameActivity mActivity;
    private final BillingHelper mBillingHelper;

    private final static int PRIORITY = 10;
    private final static String ITEM_GROUP_ID = "100000031624";
    private final static String ITEM_50_CELLS_ID = "000000063778";
    private Plasma plasma = null;

    private int transactionId = 0;

    public SamsungHelper(GameActivity activity, BillingHelper billingHelper) {
        mActivity = activity;
        mBillingHelper = billingHelper;
        plasma = new Plasma(ITEM_GROUP_ID, mActivity);
        plasma.setPlasmaListener(this);
        // TODO: remove developer flag
        plasma.setDeveloperFlag(1);
        mBillingHelper.updateHelper(this);
    }

    @Override
    public void onBuyOrangeCells() {
        // subscriptions are not supported
    }

    @Override
    public void onBuyFigures() {
        // non-consumable items are not supported
    }

    @Override
    public void onBuyChanges() {
        // buy 50 changes
        plasma.requestPurchaseItem(transactionId++, ITEM_50_CELLS_ID);
    }

    @Override
    public Market getMarket() {
        return Market.SAMSUNG_APPS;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void onItemInformationListReceived(int i, int i2, ArrayList<ItemInformation> itemInformations) {

    }

    @Override
    public void onPurchasedItemInformationListReceived(int i, int i2, ArrayList<PurchasedItemInformation> purchasedItemInformations) {

    }

    @Override
    public void onPurchaseItemInitialized(int transactionId, int statusCode, PurchaseTicket purchaseTicket) {
        if (statusCode == Plasma.STATUS_CODE_SUCCESS) {
            String purchaseID = purchaseTicket.getPurchaseId();

        }
    }

    @Override
    public void onPurchaseItemFinished(int transactionId, int statusCode, PurchasedItemInformation purchasedItemInformation) {
        if (statusCode == Plasma.STATUS_CODE_SUCCESS) {
            if (purchasedItemInformation.getItemId().equals(ITEM_50_CELLS_ID)) {
                final SharedPreferences settings = getSharedPreferencesForCurrentUser();
                int changes = settings.getInt(GameActivity.CHANGES, 0) + 50;
                final SharedPreferences.Editor editor = getSharedPreferencesEditor();
                editor.putInt(GameActivity.CHANGES, changes);
                editor.commit();
                mActivity.update();
            }
        }
    }

    private SharedPreferences.Editor getSharedPreferencesEditor() {
        return mActivity.getSharedPreferencesForCurrentUser().edit();
    }

    private SharedPreferences getSharedPreferencesForCurrentUser() {
        return mActivity.getSharedPreferences(mActivity.getCurrentUser(),
                Context.MODE_PRIVATE);
    }
}


