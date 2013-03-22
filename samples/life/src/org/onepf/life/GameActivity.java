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

package org.onepf.life;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import com.amazon.inapp.purchasing.PurchasingManager;
import org.onepf.life.amazon.PurchasingObserver;
import org.onepf.life.google.GooglePlayHelper;
import org.onepf.life.util.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameActivity extends Activity {
	public enum Market {
		GOOGLE_PLAY, AMAZON_APP_STORE, UNDEFINED
	}

	Market market = Market.UNDEFINED;

	// Google play parameters
	String googleBase64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhh9ee2Ka+dO2UCkGSndfH6/5jZ/kgILRguYcp8TpkAus6SEU8r8RSjYf4umAVD0beC3e7KOpxHxjnnE0z8A+MegZ11DE7/jQw4XQ0BaGzDTezCJrNUR8PqKf/QemRIT7UaNC0DrYE07v9WFjHFSXOqChZaJpih5lC/1yxwh+54IS4wapKcKnOFjPqbxw8dMTA7b0Ti0KzpBcexIBeDV5FT6FimawfbUr/ejae2qlu1fZdlwmj+yJEFk8h9zLiH7lhzB6PIX72lLAYk+thS6K8i26XbtR+t9/wahlwv05W6qtLEvWBJ5yeNXUghAw+Hk/x8mwIlrsjWMQtt1W+pBxYQIDAQAB";
	GooglePlayHelper googlePlayHelper;

	// Amazon app store parameters
	// ... add something here

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

		// TODO: define market
		market = Market.GOOGLE_PLAY;
		Log.d(TAG, market.toString());

		if (market == Market.GOOGLE_PLAY) {
			googlePlayHelper = new GooglePlayHelper(this,
					googleBase64EncodedPublicKey);
		}
		if (market == Market.AMAZON_APP_STORE) {
			// TODO: amazon on start
		}

		requestIds = new HashMap<String, String>();
		context = this;
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

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		String requestId;
		Log.d(TAG, market.toString());
		switch (item.getItemId()) {
		case R.id.menu_buy_changes:
			if (market == Market.AMAZON_APP_STORE) {
				lifeView.onBuyUpgradeEvent();
			} else {
				if (market == Market.GOOGLE_PLAY) {
					if (googlePlayHelper != null) {
						googlePlayHelper.onBuyChanges();
					}
				} else {
					Log.d(TAG, "Buy 50 changes in " + market.toString());
				}
			}
			break;
		case R.id.menu_buy_figures:
			if (market == Market.AMAZON_APP_STORE) {
				requestId = PurchasingManager
						.initiatePurchaseRequest(getResources().getString(
								R.string.figures_sku));
				storeRequestId(requestId, FIGURES);
			} else {
				if (market == Market.GOOGLE_PLAY) {
					if (googlePlayHelper != null) {
						googlePlayHelper.onBuyFigures();
					}
				} else {
					Log.d(TAG, "Buy figures in " + market.toString());
				}
			}
			break;
		case R.id.menu_buy_orange_cells:
			if (market == Market.AMAZON_APP_STORE) {
				requestId = PurchasingManager
						.initiatePurchaseRequest(getResources().getString(
								R.string.subscription_sku));
				storeRequestId(requestId, ORANGE_CELLS);
			} else {
				if (market == Market.GOOGLE_PLAY) {
					if (googlePlayHelper != null) {
						googlePlayHelper.onBuyOrangeCells();
					}
				} else {
					Log.d(TAG, "Buy orange cells in " + market.toString());
				}
			}
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
	public void onStart() {
		super.onStart();
		if (market == Market.AMAZON_APP_STORE) {
			PurchasingObserver purchasingObserver = new PurchasingObserver(this);
			PurchasingManager.registerObserver(purchasingObserver);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (market == Market.AMAZON_APP_STORE) {
			PurchasingManager.initiateGetUserIdRequest();
		}
		lifeView.resume();
	}

	@Override
	public void onPause() {
		super.onPause();
		lifeView.pause();
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
	 * @param currentUser
	 *            current user to set
	 */
	public void setCurrentUser(final String currentUser) {
		this.currentUser = currentUser;
	}

	/**
	 * Helper method to associate request ids to shared preference keys
	 * 
	 * @param requestId
	 *            Request ID returned from a Purchasing Manager Request
	 * @param key
	 *            Key used in shared preferrences file
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
		for (MenuItem figure : ab_menu_figures) {
			figure.setVisible(hasFigures);
		}

		boolean hasOrangeCells = settings.getBoolean(ORANGE_CELLS, false);
		Log.d(TAG, hasOrangeCells + " ");
		if (ab_buy_orange_cells != null) {
			ab_buy_orange_cells.setVisible(!hasOrangeCells);
		}
		lifeView.setActiveCellBitmap(BitmapFactory.decodeResource(
				getResources(), hasOrangeCells ? R.drawable.cell_active_orange
						: R.drawable.cell_active_green));
	}

	public void alert(String message) {
		AlertDialog.Builder bld = new AlertDialog.Builder(context);
		bld.setMessage(message);
		bld.setNeutralButton("OK", null);
		bld.create().show();
	}
}
