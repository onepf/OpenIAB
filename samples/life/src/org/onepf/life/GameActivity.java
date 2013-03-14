package org.onepf.life;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.MenuItem;
import com.amazon.inapp.purchasing.PurchasingManager;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import org.onepf.life.amazon.PurchasingObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameActivity extends Activity {
    // UI elements
	LifeView lifeView;
    MenuItem ab_buy_figures;
    MenuItem ab_buy_orange_cells;
    List<MenuItem> ab_menu_figures;

    static final String TAG = "Life";

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
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_game);
        lifeView = (LifeView) findViewById(R.id.life_view);
        
        final Button button = (Button) findViewById(R.id.start_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                lifeView.setEditMode(!lifeView.getEditMode());
        		Button button = (Button)findViewById(R.id.start_button);
        		button.setText(lifeView.getEditMode() ? R.string.start_button : R.string.edit_button);
            }
        });

        requestIds = new HashMap<String, String>();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar, menu);
        ab_buy_figures = menu.findItem(R.id.menu_buy_figures);
        ab_buy_orange_cells = menu.findItem(R.id.menu_buy_orange_cells);

        ab_menu_figures = new ArrayList<>();
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
        switch (item.getItemId()) {
            case R.id.menu_buy_changes:
                lifeView.onBuyUpgradeEvent();
                break;
            case R.id.menu_buy_figures:
                requestId = PurchasingManager.initiatePurchaseRequest(getResources().getString(R.string.figures_sku));
                storeRequestId(requestId, FIGURES);
                break;
            case R.id.menu_buy_orange_cells:
                requestId = PurchasingManager.initiatePurchaseRequest(getResources().getString(R.string.subscription_sku));
                storeRequestId(requestId, ORANGE_CELLS);
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
        PurchasingObserver purchasingObserver = new PurchasingObserver(this);
        PurchasingManager.registerObserver(purchasingObserver);
    }

	@Override
	public void onResume() {
		super.onResume();
        PurchasingManager.initiateGetUserIdRequest();
		lifeView.resume();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		lifeView.pause();
	}

    /**
     * Gets current logged in user
     * @return current user
     */
    public String getCurrentUser(){
        return currentUser;
    }

    /**
     * Sets current logged in user
     * @param currentUser current user to set
     */
    public void setCurrentUser(final String currentUser){
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
        if (ab_buy_orange_cells != null) {
            ab_buy_orange_cells.setVisible(!hasOrangeCells);
        }
        lifeView.setActiveCellBitmap(BitmapFactory.decodeResource(getResources(),
                hasOrangeCells ? R.drawable.cell_active_orange : R.drawable.cell_active_green));
    }
}
