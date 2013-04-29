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
```
package org.onepf.oms;

interface IOpenAppstore {
    boolean isAppAvailable(String packageName);
    boolean isInstaller(String packageName);
    boolean isIabServiceSupported(String packageName);
    IOpenInAppBillingService getInAppBillingService();
    String getAppstoreName();
}
```

### Sample Code 
```
String myPackageName = context.getPackageName();
PackageManager packageManager = getPackageManager();
Intent intentAppstoreServices = new Intent("org.onepf.oms.openappstore.BIND");

List<ResolveInfo> infoList = packageManager.queryIntentServices(intentAppstores, 0);
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
            //ToDo add logic that asynchroniously collects information from all the appstores and selects right one
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //Nothing to do here
        }
    }, Context.BIND_AUTO_CREATE);
}
```        


OpenIAB API
-------------

[Description of open IAB]
OpenIAB API is designed to be as close to Google IAB API as possible to make it easier for app developers to port their apps and games.


### Interface
```
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
Specification version: 0.51
Last update: April 29, 2013  

License
-------------
This file is licensed under the Creative Commons Attribution 2.5 license:  
http://creativecommons.org/licenses/by/2.5/

Source code is licensed under Apache License, Version 2.0:  
http://www.apache.org/licenses/LICENSE-2.0.html


