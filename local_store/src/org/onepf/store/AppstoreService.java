package org.onepf.store;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AppstoreService extends Service {

    AppstoreBinder _binder;

    @Override
    public void onCreate() {
        super.onCreate();
        _binder = new AppstoreBinder(this, ((StoreApplication)getApplication()).getDatabase());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return _binder;
    }
}
