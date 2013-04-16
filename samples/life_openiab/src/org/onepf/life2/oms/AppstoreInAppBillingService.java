package org.onepf.life2.oms;

import android.app.Activity;
import android.content.Intent;
import org.onepf.life2.google.util.IabHelper;
import org.onepf.life2.google.util.Inventory;
import org.onepf.life2.google.util.Purchase;

import java.util.List;

/**
 * User: Boris Minaev
 * Date: 16.04.13
 * Time: 15:46
 */
interface AppstoreInAppBillingService {
    void startSetup(final IabHelper.OnIabSetupFinishedListener listener);

    void launchPurchaseFlow(Activity act, String sku, String itemType, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener, String extraData);

    boolean handleActivityResult(int requestCode, int resultCode, Intent data);

    Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus, List<String> moreSubsSkus);

    void consume(Purchase itemInfo);
}
