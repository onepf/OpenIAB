package org.onepf.life2.oms.appstore;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import org.onepf.life2.oms.AppstoreInAppBillingService;
import org.onepf.life2.oms.appstore.googleUtils.IabException;
import org.onepf.life2.oms.appstore.googleUtils.IabHelper;
import org.onepf.life2.oms.appstore.googleUtils.Inventory;
import org.onepf.life2.oms.appstore.googleUtils.Purchase;

import java.util.List;

/**
 * User: Boris Minaev
 * Date: 22.04.13
 * Time: 12:29
 */
public class SamsungAppsBillingService implements AppstoreInAppBillingService {
    private Context mContext;
    private String mItemGroupId;

    public SamsungAppsBillingService(Context context, String itemGroupId) {
        mContext = context;
        mItemGroupId = itemGroupId;
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
    public Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus, List<String> moreSubsSkus) throws IabException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void consume(Purchase itemInfo) throws IabException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
