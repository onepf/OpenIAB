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
 * Date: 16.04.13
 * Time: 16:09
 */

public class GooglePlayBillingService implements AppstoreInAppBillingService {
    Context mContext;
    private IabHelper mIabHelper;

    public GooglePlayBillingService(Context context, String publicKey) {
        mContext = context;
        mIabHelper = new IabHelper(context, publicKey);
    }

    @Override
    public void startSetup(IabHelper.OnIabSetupFinishedListener listener) {
        mIabHelper.startSetup(listener);
    }

    @Override
    public void launchPurchaseFlow(Activity act, String sku, String itemType, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener, String extraData, String initialSku) {
        mIabHelper.launchPurchaseFlow(act, sku, itemType, requestCode, listener, extraData);
    }

    @Override
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return mIabHelper.handleActivityResult(requestCode, resultCode, data);
    }

    @Override
    public Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus, List<String> moreSubsSkus) throws IabException {
        return mIabHelper.queryInventory(querySkuDetails, moreItemSkus, moreSubsSkus);
    }

    @Override
    public void consume(Purchase itemInfo) throws IabException {
        mIabHelper.consume(itemInfo);
    }


    public void dispose() {
        mIabHelper.dispose();
    }
}
