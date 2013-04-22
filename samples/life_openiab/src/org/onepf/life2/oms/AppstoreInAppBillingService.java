package org.onepf.life2.oms;

import android.app.Activity;
import android.content.Intent;
import org.onepf.life2.oms.appstore.googleUtils.IabException;
import org.onepf.life2.oms.appstore.googleUtils.IabHelper;
import org.onepf.life2.oms.appstore.googleUtils.Inventory;
import org.onepf.life2.oms.appstore.googleUtils.Purchase;

import java.util.List;

/**
 * User: Boris Minaev
 * Date: 16.04.13
 * Time: 15:46
 */
public interface AppstoreInAppBillingService {
    void startSetup(final IabHelper.OnIabSetupFinishedListener listener);

    void launchPurchaseFlow(Activity act, String sku, String itemType, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener, String extraData);

    boolean handleActivityResult(int requestCode, int resultCode, Intent data);

    Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus, List<String> moreSubsSkus) throws IabException;

    void consume(Purchase itemInfo) throws IabException;
}
