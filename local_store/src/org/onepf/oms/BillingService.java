package org.onepf.oms;

import android.content.Intent;
import android.os.IBinder;

public class BillingService extends ServiceBase {

    BillingBinder _binder;

    @Override
    public void onCreate() {
        super.onCreate();
        _binder = new BillingBinder(this, getBillingApplication().getDatabase());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return _binder;
    }
}
