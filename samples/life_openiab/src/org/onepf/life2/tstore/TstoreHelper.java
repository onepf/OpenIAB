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

package org.onepf.life2.tstore;

import android.os.Bundle;
import android.util.Log;
import com.skplanet.dodo.IapPlugin;
import org.onepf.life2.*;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 05.04.13
 */
public class TstoreHelper extends BasePurchaseHelper {

    private final static int PRIORITY = 20;

    private final GameActivity mActivity;
    private final BillingHelper mBillingHelper;
    private IapPlugin mPlugin;

    public TstoreHelper(GameActivity activity, BillingHelper billingHelper) {
        mActivity = activity;
        mBillingHelper = billingHelper;
        mPlugin = IapPlugin.getPlugin(activity);
    }

    @Override
    public void onBuyOrangeCells() {
        buyRequest(mActivity.getResources().getString(R.string.tstore_orange_cells_pid));
    }

    @Override
    public void onBuyFigures() {
        buyRequest(mActivity.getResources().getString(R.string.tstore_figures_pid));
    }

    @Override
    public void onBuyChanges() {
        buyRequest(mActivity.getResources().getString(R.string.tstore_changes_pid));
    }

    @Override
    public Market getMarket() {
        return Market.TSTORE;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    private void buyRequest(String pid) {
        ParamsBuilder paramsBuilder = new ParamsBuilder();
        paramsBuilder.put(ParamsBuilder.KEY_APPID, mActivity.getResources().getString(R.string.tstore_appid));
        paramsBuilder.put(ParamsBuilder.KEY_PID, pid);
        Bundle req = mPlugin.sendPaymentRequest(paramsBuilder.build(), new RequestCallback(this, mBillingHelper, mActivity));
        if (req == null) {
            Log.e(GameActivity.TAG, "TStore buy request failure");
        } else {
            String mRequestId = req.getString(IapPlugin.EXTRA_REQUEST_ID);
            if (mRequestId == null || mRequestId.length() == 0) {
                Log.e(GameActivity.TAG, "TStore request failure");
            }
        }
    }
}
