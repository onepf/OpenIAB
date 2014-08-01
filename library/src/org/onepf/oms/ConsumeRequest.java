package org.onepf.oms;

import android.os.Handler;
import android.os.Looper;

import org.jetbrains.annotations.NotNull;
import org.onepf.oms.appstore.googleUtils.IabException;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Purchase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by krozov on 31.07.14.
 */
public class ConsumeRequest {
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    private final Purchase[] purchases;

    public ConsumeRequest(Purchase... purchases) {
        this.purchases = purchases;
    }

    public ConsumeRequest(List<Purchase> purchases) {
        this.purchases = purchases.toArray(new Purchase[purchases.size()]);
    }

    public Purchase[] getPurchases() {
        return purchases;
    }

    public void execute(OpenIabHelper openIabHelper, final Listener listener) {
        final Map<Purchase, IabResult> results = new HashMap<Purchase, IabResult>();
        final String appstoreName = openIabHelper.mAppstore.getAppstoreName();
        final SkuManager skuManager = SkuManager.getInstance();

        for (Purchase purchase : purchases) {
            try {
                Purchase purchaseStoreSku =
                        purchase.copy(skuManager.getStoreSku(appstoreName, purchase.getSku())); // TODO: use Purchase.getStoreSku()
                openIabHelper.appstoreBillingService.consume(purchaseStoreSku);
                results.put(purchase, new IabResult(IabHelper.BILLING_RESPONSE_RESULT_OK,
                        "Successful consume of sku " + purchase.getSku()));
            } catch (IabException ex) {
                results.put(purchase, ex.getResult());
            }
        }

        if (listener != null) {
            HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    listener.onConsumeMultiFinished(results);
                }
            });
        }
    }

    interface Listener {
        void onConsumeMultiFinished(@NotNull Map<Purchase, IabResult> result);
    }
}
