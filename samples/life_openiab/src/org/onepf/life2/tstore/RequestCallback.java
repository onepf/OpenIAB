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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;
import com.skplanet.dodo.IapPlugin;
import com.skplanet.dodo.IapResponse;
import org.onepf.life2.BillingHelper;
import org.onepf.life2.GameActivity;
import org.onepf.life2.R;

import java.util.Date;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 05.04.13
 */
public class RequestCallback implements IapPlugin.RequestCallback {

    private static final String START_DATE = "start_date";

    private final GameActivity mActivity;
    private final TstoreHelper mHelper;
    private final BillingHelper mBillingHelper;

    public RequestCallback(TstoreHelper helper, BillingHelper billingHelper, GameActivity parent) {
        mHelper = helper;
        mBillingHelper = billingHelper;
        mActivity = parent;
    }


    @Override
    public void onError(String reqid, String errcode, String errmsg) {
        Log.e(GameActivity.TAG, "TStore error. onError() identifier:" + reqid + " code:" + errcode + " msg:" + errmsg);
    }

    @Override
    public void onResponse(IapResponse data) {
        mBillingHelper.updateHelper(mHelper);
        if (data == null || data.getContentLength() <= 0) {
            Log.e(GameActivity.TAG, "onResponse() response data is null");
            return;
        }
        Response response = new GsonConverter().fromJson(data.getContentToString());
        if (response.result.code.equals("0000")) {
            for (Response.Product product : response.result.product) {
                Resources resources = mActivity.getResources();
                final SharedPreferences settings = getSharedPreferencesForCurrentUser();
                final SharedPreferences.Editor editor = getSharedPreferencesEditor();
                if (product.id.equals(resources.getString(R.string.tstore_changes_pid))) {
                    int numClicks = settings.getInt(GameActivity.CHANGES, 0);
                    editor.putInt(GameActivity.CHANGES, numClicks + 50);
                } else if (product.id.equals(resources.getString(R.string.tstore_orange_cells_pid))) {
                    editor.putBoolean(GameActivity.ORANGE_CELLS, true);
                    editor.putLong(START_DATE, new Date().getTime());
                } else if (product.id.equals(resources.getString(R.string.tstore_figures_pid))) {
                    editor.putBoolean(GameActivity.FIGURES, true);
                } else {
                    Log.d(GameActivity.TAG, "TStore: unknown PID");
                }
                editor.commit();
            }
        }
    }

    private SharedPreferences getSharedPreferencesForCurrentUser() {
        return mActivity.getSharedPreferences(mActivity.getCurrentUser(), Context.MODE_PRIVATE);
    }

    private SharedPreferences.Editor getSharedPreferencesEditor() {
        return getSharedPreferencesForCurrentUser().edit();
    }
}
