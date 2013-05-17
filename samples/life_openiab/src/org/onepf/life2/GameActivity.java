/*******************************************************************************
 * Copyright 2013 One Platform Foundation
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

package org.onepf.life2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameActivity extends Activity {

    public BillingHelper billingHelper;

    // UI elements
    LifeView lifeView;
    MenuItem ab_buy_figures;
    MenuItem ab_buy_orange_cells;
    List<MenuItem> ab_menu_figures;
    Context context;

    public static final String TAG = "Life";

    // Keys for shared preferences
    public static final String CHANGES = "changes";
    public static final String FIGURES = "figures";
    public static final String ORANGE_CELLS = "orange_cells";

    // currently logged in user
    private String currentUser;

    // Mapping of our requestIds to unlockable content
    public Map<String, String> requestIds;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_game);
        lifeView = (LifeView) findViewById(R.id.life_view);

        final Button button = (Button) findViewById(R.id.start_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                lifeView.setEditMode(!lifeView.getEditMode());
                Button button = (Button) findViewById(R.id.start_button);
                button.setText(lifeView.getEditMode() ? R.string.start_button
                        : R.string.edit_button);
            }
        });


        requestIds = new HashMap<String, String>();
        context = this;

        billingHelper = new BillingHelper(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar, menu);
        ab_buy_figures = menu.findItem(R.id.menu_buy_figures);
        ab_buy_orange_cells = menu.findItem(R.id.menu_buy_orange_cells);

        ab_menu_figures = new ArrayList<MenuItem>();
        ab_menu_figures.add(menu.findItem(R.id.menu_empty_field));
        ab_menu_figures.add(menu.findItem(R.id.menu_glider));
        ab_menu_figures.add(menu.findItem(R.id.menu_big_glider));
        ab_menu_figures.add(menu.findItem(R.id.menu_periodic));
        ab_menu_figures.add(menu.findItem(R.id.menu_robot));

        update();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_buy_changes:
                billingHelper.onBuyChanges();
                break;
            case R.id.menu_buy_figures:
                billingHelper.onBuyFigures();
                break;
            case R.id.menu_buy_orange_cells:
                billingHelper.onBuyOrangeCells();
                break;
            case R.id.menu_empty_field:
                lifeView.drawFigure(null);
                break;
            case R.id.menu_glider:
                lifeView.drawFigure(Figures.glider);
                break;
            case R.id.menu_big_glider:
                lifeView.drawFigure(Figures.bigGlider);
                break;
            case R.id.menu_periodic:
                lifeView.drawFigure(Figures.periodic);
                break;
            case R.id.menu_robot:
                lifeView.drawFigure(Figures.robot);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        billingHelper.onResume();
        lifeView.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        lifeView.pause();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        billingHelper.onDestroy();
    }

    /**
     * Gets current logged in user
     *
     * @return current user
     */
    public String getCurrentUser() {
        return currentUser;
    }

    /**
     * Sets current logged in user
     *
     * @param currentUser current user to set
     */
    public void setCurrentUser(final String currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * Helper method to associate request ids to shared preference keys
     *
     * @param requestId Request ID returned from a Purchasing Manager Request
     * @param key       Key used in shared preferrences file
     */
    public void storeRequestId(String requestId, String key) {
        requestIds.put(requestId, key);
    }

    /**
     * Get the SharedPreferences file for the current user.
     *
     * @return SharedPreferences file for a user.
     */
    public SharedPreferences getSharedPreferencesForCurrentUser() {
        Log.d(TAG, "Return preferences with user: " + currentUser);
        return getSharedPreferences(currentUser, Context.MODE_PRIVATE);
    }

    public void update() {
        final SharedPreferences settings = getSharedPreferencesForCurrentUser();
        lifeView.setChangeCount(settings.getInt(CHANGES, 50));
        boolean hasFigures = settings.getBoolean(FIGURES, false);
        if (ab_buy_figures != null) {
            ab_buy_figures.setVisible(!hasFigures);
        }
        if (ab_menu_figures != null) {
            for (MenuItem figure : ab_menu_figures) {
                figure.setVisible(hasFigures);
            }
        }
        boolean hasOrangeCells = settings.getBoolean(ORANGE_CELLS, false);
        Log.d(TAG, "Has orange cells: " + hasOrangeCells);
        if (ab_buy_orange_cells != null) {
            ab_buy_orange_cells.setVisible(!hasOrangeCells);
        }
        lifeView.setActiveCellBitmap(BitmapFactory.decodeResource(
                getResources(), hasOrangeCells ? R.drawable.cell_active_orange
                : R.drawable.cell_active_green));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        billingHelper.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(context);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        bld.create().show();
    }


    public BillingHelper getBillingHelper() {
        return billingHelper;
    }
}
