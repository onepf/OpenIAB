package org.onepf.sample.trivialdrive;

import android.app.Application;

/**
 * Created by akarimova on 29.10.14.
 */
public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        InAppConfig.init();
    }
}
