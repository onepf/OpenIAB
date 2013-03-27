package org.onepf.life;

import android.content.Intent;
import android.util.Log;
import com.amazon.inapp.purchasing.PurchasingManager;
import org.onepf.life.amazon.AmazonHelper;
import org.onepf.life.google.GooglePlayHelper;

/**
 * User: Boris Minaev
 * Date: 27.03.13
 * Time: 19:35
 */

public class BillingHelper {
    private GameActivity activity;
    private BasePurchaseHelper currentHelper;

    public BillingHelper(GameActivity activity) {
        this.activity = activity;
        new GooglePlayHelper(activity, this);
        new AmazonHelper(activity, this);
    }

    public void updateHelper(BasePurchaseHelper helper) {
        if (currentHelper == null || currentHelper.getPriority() < helper.getPriority()) {
            currentHelper = helper;
        }
    }

    public void onBuyChanges() {
        Log.e(GameActivity.TAG, "onBuyChanges");
        if (currentHelper != null) {
            currentHelper.onBuyChanges();
        }
    }

    public void onBuyOrangeCells() {
        Log.e(GameActivity.TAG, "onBuyOrangeCells");
        if (currentHelper != null) {
            currentHelper.onBuyOrangeCells();
        }
    }

    public void onBuyFigures() {
        Log.e(GameActivity.TAG, "onBuyFigures");
        if (currentHelper != null) {
            currentHelper.onBuyFigures();
        }
    }

    public void onResume() {
        if (currentHelper != null) {
            if (currentHelper.getMarket() == Market.AMAZON_APP_STORE) {
                PurchasingManager.initiateGetUserIdRequest();
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (currentHelper != null) {
            if (currentHelper.getMarket() == Market.GOOGLE_PLAY) {
                currentHelper
                        .onActivityResult(requestCode, resultCode, data);
            }
        }
    }
}
