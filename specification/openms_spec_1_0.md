Summary
-------------

Open Mobile Services is API that Android appstores implement in order to abstract appstore specific services like in-app billing, licensing, pish, etc from Android apps. So that Android apps can use such services from their code without taking care which appstore will handle it.

There could be several appstores installed on a device. So an app should first determine which of them installed this app.
The Android app gets all OMS appstore services by requesting all services that handle a particual intent. Then it checks which appstore to work with (by checking which appstore installed this app, etc). 

APIs
-------------
OpenAppstore API  - general top-level API to check which appstore to work with and request other service APIs.

OpenIAB API (Open In-App Billing API) - [short description here]

OpenAppstore API
-------------
An application store that implemenents OpenStore API must provide a bindable service that handles `org.onepf.oms.openappstore.BIND` intent and implements the following API.

### Interface
```java
package org.onepf.oms;

import android.content.Intent;

interface IOpenAppstore {
    boolean isAppAvailable(String packageName);
    boolean isInstaller(String packageName);
    boolean isIabServiceSupported(String packageName);
    Intent getInAppBillingServiceIntent();
    String getAppstoreName();
}
```

### Sample Code 
```java
interface ServiceFounder {
    void onServiceFound(Intent intent, boolean installer);
    void onServiceNotFound();
}

class AppstoresServiceSupport {
    class ServiceInfo {
        boolean installer;
        boolean supported;
        Intent intent;
        ServiceInfo(boolean installer, boolean supported, Intent intent) {
            this.installer = installer;
            this.supported = supported;
            this.intent = intent;
        }
    }
    List<ServiceInfo> serviceInfo;
    int count = 0;
    public AppstoresServiceSupport(int count) {
        this.count = count;
        serviceInfos = new ArrayList<ServiceInfo>();  
    }

    public void add(boolean installer, boolean supported, Intent intent) {
        serviceInfos.add(new ServiceInfo(installer, supported, intent));
    }

    public boolean isReady() {
        return serviceInfos.size() >= count;
    }

    public void getServiceIntent(ServiceFounder serviceFounder) {
        Intent intent = null;
        for (ServiceInfo info : serviceInfos) {
            if (info.installer) {
                intent = info.intent;    
            }
        }
        if (intent) {
            serviceFounder.onServiceFound(intent, true);
        } else {
            for (ServiceInfo info : serviceInfos) {
                if (info.supported) {
                    intent = info.intent;    
                }
            }
            if (intent) {
                serviceFounder.onServiceFound(intent, false);
            } else {
                serviceFounder.onServiceNotFound();
            }
        }
    }
  
}

interface ServiceFounder {
    void onServiceFound(Intent intent, boolean installer);
    void onServiceNotFound();
}

public void findOpenIabService(Context context, final ServiceFounder serviceFounder) { 
    String myPackageName = context.getPackageName();
    PackageManager packageManager = getPackageManager();
    Intent intentAppstoreServices = new Intent("org.onepf.oms.openappstore.BIND");

    List<ResolveInfo> infoList = packageManager.queryIntentServices(intentAppstores, 0);
    AppstoresServiceSupport intentAppstore = new AppstoresServiceSupport(infoList.size());
    for (ResolveInfo info : infoList) {
        String packageName = info.serviceInfo.packageName;
        String name = info.serviceInfo.name;
        Intent intentAppstore = new Intent();
        intentAppstore.setClassName(packageName, name);
        bindService(intentAppstore, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                openAppstoreService = IOpenAppstore.Stub.asInterface(service);
                boolean isInstaller = openAppstoreService.isInstaller(myPackageName);
                boolean isSupported = openAppstoreService.isIabServiceSupported(myPackageName);
                Intent iabIntent = openAppstoreService.getInAppBillingServiceIntent();
                appstoreServiceSupport.add(isInstaller, isSupported, iabIntent);
                if (appstoreServiceSupport.isReady()) {
                    appstoreServiceSupport.getServiceIntent(serviceFounder);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                //Nothing to do here
            }
        }, Context.BIND_AUTO_CREATE);
    }
}
```        


OpenIAB API
-------------

[Description of open IAB]
OpenIAB API is designed to be as close to Google IAB API as possible to make it easier for app developers to port their apps and games.


### Interface
```java
package org.onepf.oms;

interface IOpenInAppBillingService {
    int isBillingSupported(int apiVersion, String packageName, String type);
    Bundle getSkuDetails(int apiVersion, String packageName, String type, in Bundle skusBundle);
    Bundle getBuyIntent(int apiVersion, String packageName, String sku, String type, String developerPayload);
    Bundle getPurchases(int apiVersion, String packageName, String type, String continuationToken);
    int consumePurchase(int apiVersion, String packageName, String purchaseToken);
}
```

Status
-------------
Current status: draft  
Specification version: 0.52
Last update: April 29, 2013  

License
-------------
This file is licensed under the Creative Commons Attribution 2.5 license:  
http://creativecommons.org/licenses/by/2.5/

Source code is licensed under Apache License, Version 2.0:  
http://www.apache.org/licenses/LICENSE-2.0.html


