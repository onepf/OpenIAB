/*******************************************************************************
 * Copyright 2013-2014 One Platform Foundation
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 ******************************************************************************/

package org.onepf.life;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.appstore.googleUtils.Purchase;

import java.util.HashMap;
import java.util.Map;

public class GameActivity extends Activity implements LifeViewListener {
    public static final String TAG = "Life";
    private static final int REQUEST_CODE = 10001;

    //shared preferences
    private static final String PREF = "game_preferences";
    private static final String PREF_HAS_ORANGE_CELLS = "pref_has_orange_cells";
    private static final String PREF_HAS_FIGURES = "pref_has_figures";
    private static final String PREF_CHANGES_COUNT = "pref_changes_count";

    //consumable
    private static final String SKU_CHANGES = "org.onepf.life3.changes";
    private int changesCount = -1;

    //non-consumable
    private static final String SKU_FIGURES = "org.onepf.life3.figures";

    //subscription
    private static final String SKU_ORANGE_CELLS = "org.onepf.life3.orange_cells";

    // UI elements
//    private LifeView lifeView;
    private LifeViewRenderer lifeViewRenderer;
    private ProgressDialog progressDialog;


    private OpenIabHelper openIabHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        //life view
        final LifeView lifeView = (LifeView) findViewById(R.id.life_view);
        lifeView.setUp(this, getChangeCount());
        lifeViewRenderer = new LifeViewRenderer(lifeView);
        lifeViewRenderer.updateCellColor(hasOrangeCells());
        //start/edit game button
        findViewById(R.id.start_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                lifeView.setEditMode(!lifeView.getEditMode());
                Button button = (Button) findViewById(R.id.start_button);
                button.setText(lifeView.getEditMode() ? R.string.start_button
                        : R.string.edit_button);
            }
        });
        //public keys
        String GOOGLE_PUBLIC_KEY = "YOUR_GOOGLE_PUBLIC_KEY";
        String YANDEX_PUBLIC_KEY = "YOUR_YANDEX_PUBLIC_KEY";
        //IAB helper
        Map<String, String> storeKeys = new HashMap<String, String>();
        storeKeys.put(OpenIabHelper.NAME_GOOGLE, GOOGLE_PUBLIC_KEY);
        storeKeys.put(OpenIabHelper.NAME_YANDEX, YANDEX_PUBLIC_KEY);

        openIabHelper = new OpenIabHelper(this, storeKeys);
        openIabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                                     public void onIabSetupFinished(IabResult result) {
                                         Log.d(TAG, "Setup finished.");
                                         showProgressDialog(false);
                                         if (result.isSuccess()) {
                                             Log.d(TAG, "Setup successful. Queying inventory.");
                                             openIabHelper.queryInventoryAsync(gotInventoryListener);
                                         } else {
                                             Toast.makeText(GameActivity.this, "Problem setting up in-app billing: " + result, Toast.LENGTH_SHORT).show();
                                         }
                                     }
                                 }
        );
        showProgressDialog(true);
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    private IabHelper.QueryInventoryFinishedListener gotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (result.isFailure()) {
                Toast.makeText(GameActivity.this, "Failed to query inventory: " + result, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(GameActivity.this, "Query inventory was successful." + result, Toast.LENGTH_SHORT).show();
            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            // check for orange cells subscription
            Purchase orangeCellsPurchase = inventory.getPurchase(SKU_ORANGE_CELLS);

            if (orangeCellsPurchase != null) {
                boolean orangeCellsWerePurchased = verifyDeveloperPayload(orangeCellsPurchase);
                SharedPreferences.Editor edit = getGameSharedPreferences().edit();
                edit.putBoolean(PREF_HAS_ORANGE_CELLS, orangeCellsWerePurchased);
                edit.commit();
                lifeViewRenderer.updateCellColor(hasOrangeCells());
            }

            //non-consumable figures
            Purchase figuresPurchase = inventory.getPurchase(SKU_FIGURES);
            if (figuresPurchase != null && verifyDeveloperPayload(figuresPurchase)) {
                SharedPreferences.Editor edit = getGameSharedPreferences().edit();
                edit.putBoolean(PREF_HAS_FIGURES, true);
                edit.commit();
            }

            //consumable changes
            Purchase changesPurchase = inventory.getPurchase(SKU_CHANGES);
            if (changesPurchase != null && verifyDeveloperPayload(changesPurchase)) {
                openIabHelper.consumeAsync(changesPurchase, consumeFinishedListener);
            }

            invalidateOptionsMenu();
        }

    };


    // Called when consumption is complete
    private IabHelper.OnConsumeFinishedListener consumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);
            if (result.isSuccess()) {
                if (purchase.getSku().equals(SKU_CHANGES)) {
                    increaseChangesCount(DEFAULT_CHANGES_COUNT);
                }
            } else {
                Toast.makeText(GameActivity.this, "unsuccessful consume", Toast.LENGTH_SHORT).show();
            }
        }
    };

    /**
     * Verifies the developer payload of a purchase.
     */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        SharedPreferences settings = getGameSharedPreferences();
        boolean hasFigures = settings.getBoolean(PREF_HAS_FIGURES, false);
        menu.setGroupVisible(R.id.menu_group_figures, hasFigures);
        menu.findItem(R.id.menu_buy_figures).setVisible(!hasFigures);
        menu.findItem(R.id.menu_buy_orange_cells).setVisible(!hasOrangeCells());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_buy_changes:
                buyChanges();
                break;
            case R.id.menu_buy_figures:
                buyFigures();
                break;
            case R.id.menu_buy_orange_cells:
                buyOrangeCells();
                break;
            case R.id.menu_empty_field:
                lifeViewRenderer.drawFigure(null);
                break;
            case R.id.menu_glider:
                lifeViewRenderer.drawFigure(Figures.glider);
                break;
            case R.id.menu_big_glider:
                lifeViewRenderer.drawFigure(Figures.bigGlider);
                break;
            case R.id.menu_periodic:
                lifeViewRenderer.drawFigure(Figures.periodic);
                break;
            case R.id.menu_robot:
                lifeViewRenderer.drawFigure(Figures.robot);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void buyChanges() {
        String payload = "";

        openIabHelper.launchPurchaseFlow(this, SKU_CHANGES, REQUEST_CODE,
                purchaseFinishedListener, payload);
    }

    private void buyOrangeCells() {
        openIabHelper.launchSubscriptionPurchaseFlow(this, SKU_ORANGE_CELLS, REQUEST_CODE, purchaseFinishedListener);
    }

    private void buyFigures() {
        String payload = "";
        openIabHelper.launchPurchaseFlow(this, SKU_FIGURES, REQUEST_CODE,
                purchaseFinishedListener, payload);
    }


    // Callback for when a purchase is finished
    private IabHelper.OnIabPurchaseFinishedListener purchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
            if (result.isFailure()) {
                Toast.makeText(GameActivity.this, "Error purchasing: " + result, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                Toast.makeText(GameActivity.this, "Error purchasing. Authenticity verification failed.", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Purchase successful.");

            if (purchase.getSku().equals(SKU_CHANGES)) {
                openIabHelper.consumeAsync(purchase, consumeFinishedListener);
            } else if (purchase.getSku().equals(SKU_FIGURES)) {
                SharedPreferences gameSharedPreferences = getGameSharedPreferences();
                SharedPreferences.Editor edit = gameSharedPreferences.edit();
                edit.putBoolean(PREF_HAS_FIGURES, true);
                edit.commit();
                invalidateOptionsMenu();
            } else if (purchase.getSku().equals(SKU_ORANGE_CELLS)) {
                Toast.makeText(GameActivity.this, R.string.subscribe_thank, Toast.LENGTH_SHORT).show();
                SharedPreferences.Editor edit = getGameSharedPreferences().edit();
                edit.putBoolean(PREF_HAS_ORANGE_CELLS, true);
                edit.commit();
                lifeViewRenderer.updateCellColor(hasOrangeCells());
                invalidateOptionsMenu();
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        lifeViewRenderer.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        lifeViewRenderer.pause();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public SharedPreferences getGameSharedPreferences() {
        return getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        // Pass on the activity result to the helper for handling
        if (!openIabHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }

    @Override
    public void setChangeCount(int count) {
        SharedPreferences.Editor edit = getGameSharedPreferences().edit();
        edit.putInt(PREF_CHANGES_COUNT, count);
        edit.commit();
        changesCount = count;
    }

    private void increaseChangesCount(int newChangesCount) {
        changesCount = changesCount == -1 ? newChangesCount : changesCount + newChangesCount;
        setChangeCount(changesCount);
        lifeViewRenderer.setChangeCount(changesCount);
    }

    private int getChangeCount() {
        if (changesCount == -1) {
            changesCount = getGameSharedPreferences().getInt(PREF_CHANGES_COUNT, DEFAULT_CHANGES_COUNT);
        }
        return changesCount;
    }

    @Override
    public void onNoChangesFound() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                this);
        builder.setTitle(R.string.changes_ended)
                .setMessage(R.string.changes_ended_message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog, int which) {
                                if (which == DialogInterface.BUTTON_POSITIVE) {
                                    buyChanges();
                                }
                            }
                        }).setNegativeButton(android.R.string.no, null).show();
    }

    @Override
    public void notEnoughSpaceForFigure() {
        Toast.makeText(this, R.string.not_enough_space, Toast.LENGTH_SHORT).show();
    }

    private boolean hasOrangeCells() {
        SharedPreferences settings = getGameSharedPreferences();
        return settings.getBoolean(PREF_HAS_ORANGE_CELLS, false);
    }

    private void showProgressDialog(boolean show) {
        if ((progressDialog != null && progressDialog.isShowing())) {
            if (!show) {
                progressDialog.dismiss();
            }
        } else {
            if (show) {
                progressDialog = ProgressDialog.show(this, null, getString(R.string.waiting_dialog_message));
            }
        }
    }
}