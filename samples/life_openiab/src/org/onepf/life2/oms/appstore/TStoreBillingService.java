package org.onepf.life2.oms.appstore;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import com.skplanet.dodo.IapPlugin;
import org.onepf.life2.oms.AppstoreInAppBillingService;
import org.onepf.life2.oms.appstore.googleUtils.*;

import java.util.List;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 16.05.13
 */
public class TStoreBillingService implements AppstoreInAppBillingService {
    private final Context mContext;
    private IapPlugin mPlugin;

    public TStoreBillingService(Context context) {
        mContext = context;
    }

    @Override
    public void startSetup(IabHelper.OnIabSetupFinishedListener listener) {
        mPlugin = IapPlugin.getPlugin(mContext);
        listener.onIabSetupFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK, "Setup successful."));
    }

    @Override
    public void launchPurchaseFlow(Activity act, String sku, String itemType, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
    }

    @Override
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return false;
    }

    @Override
    public Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus, List<String> moreSubsSkus) throws IabException {
        return null;
    }

    @Override
    public void consume(Purchase itemInfo) throws IabException {
    }

    @Override
    public void dispose() {
    }
}
