package org.onepf.openiab;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;

public class UnityProxyActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (UnityPlugin.sendRequest) {
            UnityPlugin.sendRequest = false;

            Intent i = getIntent();
            String sku = i.getStringExtra("sku");
            String developerPayload = i.getStringExtra("developerPayload");
            boolean inapp = i.getBooleanExtra("inapp", true);

            try {
                if (inapp) {
                    UnityPlugin.instance().getHelper().launchPurchaseFlow(this, sku, UnityPlugin.RC_REQUEST, UnityPlugin.instance().getPurchaseFinishedListener(), developerPayload);
                } else {
                    UnityPlugin.instance().getHelper().launchSubscriptionPurchaseFlow(this, sku, UnityPlugin.RC_REQUEST, UnityPlugin.instance().getPurchaseFinishedListener(), developerPayload);
                }
            } catch (java.lang.IllegalStateException e) {
                UnityPlugin.instance().getPurchaseFinishedListener().onIabPurchaseFinished(new IabResult(IabHelper.BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE, "Cannot start purchase process. Billing unavailable."), null);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(UnityPlugin.TAG, "onActivityResult(" + requestCode + ", " + resultCode + ", " + data);

        // Pass on the activity result to the helper for handling
        if (!UnityPlugin.instance().getHelper().handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.d(UnityPlugin.TAG, "onActivityResult handled by IABUtil.");
        }

        finish();
    }
}
