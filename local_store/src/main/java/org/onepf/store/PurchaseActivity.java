package org.onepf.store;

import org.onepf.store.R;
import org.onepf.store.data.Database;
import org.onepf.store.data.Purchase;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class PurchaseActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.purchase);
    }

    public void onCancelClick(View view) {
        Intent intent = getIntent();
        intent.putExtra(BillingBinder.RESPONSE_CODE, BillingBinder.RESULT_USER_CANCELED);
        setResult(RESULT_OK, intent);
        finish();
    }

    public void onOkClick(View view) {
        Intent intent = getIntent();
        Database db = ((StoreApplication) getApplication()).getDatabase();

        final String packageName = intent.getStringExtra("packageName");
        final String sku = intent.getStringExtra("sku");
        final String developerPayload = intent.getStringExtra("developerPayload");

        Purchase purchase = db.createPurchase(packageName, sku, developerPayload);
        if (purchase == null) {
            intent.putExtra(BillingBinder.RESPONSE_CODE, BillingBinder.RESULT_ERROR);
        } else {
            db.storePurchase(purchase);
            intent.putExtra(BillingBinder.RESPONSE_CODE, BillingBinder.RESULT_OK);
            intent.putExtra(BillingBinder.INAPP_PURCHASE_DATA, purchase.toJson());
            // TODO: create signature properly!
            intent.putExtra(BillingBinder.INAPP_DATA_SIGNATURE, "");
        }

        setResult(RESULT_OK, intent);
        finish();
    }
}