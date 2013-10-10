package org.onepf.oms;

import android.content.Intent;
import android.os.IBinder;

public class AppstoreService extends ServiceBase {

    AppstoreBinder _binder;

    @Override
    public void onCreate() {
        super.onCreate();
        _binder = new AppstoreBinder(this, getBillingApplication().getDatabase());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return _binder;
    }
}
