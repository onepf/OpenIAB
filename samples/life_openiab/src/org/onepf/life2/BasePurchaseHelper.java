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

package org.onepf.life2;

import android.content.Intent;
import android.util.Log;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 25.03.13
 */
public class BasePurchaseHelper {

    public void onBuyChanges() {
        Log.e(GameActivity.TAG, "onBuyChanges call in BasePurchaseHelper");
    }

    public void onBuyOrangeCells() {
        Log.e(GameActivity.TAG, "onBuyOrangeCells call in BasePurchaseHelper");
    }

    public void onBuyFigures() {
        Log.e(GameActivity.TAG, "onBuyFigures call in BasePurchaseHelper");
    }

    public Market getMarket() {
        return Market.UNDEFINED;
    }

    public int getPriority() {
        return 0;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    public void onDestroy() {
    }
}
