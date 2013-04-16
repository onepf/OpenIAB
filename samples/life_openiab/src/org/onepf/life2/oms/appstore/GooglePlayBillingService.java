package org.onepf.life2.oms.appstore;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import org.onepf.life2.google.util.IabHelper;
import org.onepf.life2.google.util.Inventory;
import org.onepf.life2.google.util.Purchase;
import org.onepf.life2.oms.AppstoreInAppBillingService;

import java.util.List;

/**
 * User: Boris Minaev
 * Date: 16.04.13
 * Time: 16:09
 */
public class GooglePlayBillingService implements AppstoreInAppBillingService {
    Context mContext;


    public GooglePlayBillingService(Context context) {
        mContext = context;

    }

    @Override
    public void startSetup(IabHelper.OnIabSetupFinishedListener listener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void launchPurchaseFlow(Activity act, String sku, String itemType, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus, List<String> moreSubsSkus) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void consume(Purchase itemInfo) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
