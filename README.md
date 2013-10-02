OpenIAB - Open In-App Billing
=====

Uploading Android apps to all the existing Android appstores is a painful process and [AppDF](/onepf/AppDF) 
project was designed to make it easier. But what is even more difficult for the developers is 
supporting different in-purchase APIs of different appstores. There are five different In-App Purchase APIs 
already and this number is increasing. We are going to create an open source library that will wrap 
appstore in-app purchase APIs of all the stores and provide an easy way for the developers to develop 
their apps/games in a way that one APK will work in all the stores and automatically use right in-app 
purchase API under each store. Plus we are going to develop an open in-app billing API that stores 
could implement to support all the built APK files using this library.

How To add OpenIAB into your app
=====
1. Download library from GitHub
``` 
git clone https://github.com/onepf/OpenIAB.git
```

2. Link /library to project as Android Library

3. Instantiate ``` new OpenIabHelper ```  and call ``` mHelper.startSetup() ```. 
When setup is done call  ``` mHelper.queryInventory() ```
```java
  mHelper = new OpenIabHelper(this, storeKeys);
  mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
      public void onIabSetupFinished(IabResult result) {
          if (!result.isSuccess()) {
              complain("Problem setting up in-app billing: " + result);
              return;
          }
          Log.d(TAG, "Setup successful. Querying inventory.");
              mHelper.queryInventoryAsync(mGotInventoryListener);
          }
  });
```
https://github.com/onepf/OpenIAB/blob/master/samples/trivialdrive/src/org/onepf/trivialdrive/MainActivity.java#L186

4. Handle results of ``` mHelper.queryInventory() ``` in listener and update UI to show what is purchased
https://github.com/onepf/OpenIAB/blob/master/samples/trivialdrive/src/org/onepf/trivialdrive/MainActivity.java#L210

5. When in user requested purchase of item call  ``` mHelper.launchPurchaseFlow() ```
https://github.com/onepf/OpenIAB/blob/master/samples/trivialdrive/src/org/onepf/trivialdrive/MainActivity.java#L271
and handle results with listener
https://github.com/onepf/OpenIAB/blob/master/samples/trivialdrive/src/org/onepf/trivialdrive/MainActivity.java#L357

6. If user purchased consumable item call  ``` mHelper.consume() ```
to exclude it from inventory. If item not consumed Store suppose it non-consumable item and doesn't allow to purchase it one more time. Also it will be returned by ``` mHelper.queryInventory() ``` next time
https://github.com/onepf/OpenIAB/blob/master/samples/trivialdrive/src/org/onepf/trivialdrive/MainActivity.java#L237
and handle results with listener
https://github.com/onepf/OpenIAB/blob/master/samples/trivialdrive/src/org/onepf/trivialdrive/MainActivity.java#L399

7. Map Google Play SKU ids to Yandex/Amazon SKUs like this:
https://github.com/onepf/OpenIAB/blob/master/samples/trivialdrive/src/org/onepf/trivialdrive/MainActivity.java#L108

8. Specify keys for different stores like this:
https://github.com/onepf/OpenIAB/blob/master/samples/trivialdrive/src/org/onepf/trivialdrive/MainActivity.java#L173

9. Add permissions required for OpenIAB in your AndroidManifest.xml
```xml
<uses-permission android:name="org.onepf.openiab.permission.BILLING" />
```
And register reciever for Amazon
```xml
<receiver android:name="com.amazon.inapp.purchasing.ResponseReceiver">
    <intent-filter>
        <action
            android:name="com.amazon.inapp.purchasing.NOTIFY"
            android:permission="com.amazon.inapp.purchasing.Permission.NOTIFY"
        />
    </intent-filter>
</receiver>
```
https://github.com/oorlov/OpenIAB/blob/doc-update/samples/trivialdrive/AndroidManifest.xml#L33


How OpenIAB Works
=====
1. An Android app developer integrates OpenIAB library in his/her Android code 
2. An Android app developer implements in-app purchases using OpenIAB API (which is very close to Google Play IAB API, just few changes in source code will be needed) 
3. OpenIAB Lib detects which appstore installed the app 
4. OpenIAB Lib redirects in-app purchase calls to the corresponding appstore IAB API (OpenIAB Lib wrapps IAB APIs of severall apstores) 
5. All In-App Billing logic is handled by the corresponding appstore, OpenIAB has no code to process in-app purchases and has no UI, it just wrapps In-App Billing APIs of different stores in one library 

<img src="http://www.onepf.org/img/openiabdiagram1.png">

<img src="http://www.onepf.org/img/openiabdiagram2.png">

Current Status
=====
We have just started. We are creating a [sample game](/onepf/OpenIAB/tree/master/samples/trivialdrive) that supports in-app billing of all existing appstores that support in-app purchasing. In the same time, we are designing 
[Open In-App Billing API](http://www.github.com/onepf/OpenIAB/blob/master/specification/openms_spec_1_0.md) that appstores can use to easily integrate in-app billing functionality.

Basic Principles
=====
* **As close to Google Play In-app Billing API as possible** - we optimize the OpenIAB library by the following parameter "lines on code you need to change in an app that already works in Google Play to make it working in all the appstores"
* **No middle man**
* **Modular architecture** - adding new appstore should be just adding one more class imeplementing a fixed interface
* **One APK file to work in all appstores** - but OpenIAB should have an option to build separate APK files for appstores for developers who want to create APK without any overhead
* **No additional functionality** - OpenIAB does not make in-app billing easier to use, we just make it working in all the appstores with single code

No Middle Man
=====
OpenIAB is an open source library that wraps the already existing IAB APIs as well as an open API that 
appstores could implement. It is important to understand that all payments are processes directly by 
each store and there is no a middle man staying between the app developers and the appstores. 
OpenIAB will not do payments for the appstores. It is just an API how the apps communicate with 
appstores to request in-app billing. There is a common open API all the stores can use instead of 
each new store implement their own API and developers have to integrate all these different APIs in their apps.

AppStores
=====
The following Android application stores support in-app billing today:
 * [Google Play](https://play.google.com/apps/publish/)
 * [Amazon AppStore](https://developer.amazon.com/welcome.html)
 * [Samsung Apps](http://seller.samsungapps.com/)
 * [SK-Telecom T-Store](http://dev.tstore.co.kr/devpoc/main/main.omp)
 * [NOOK](https://nookdeveloper.barnesandnoble.com/) (via [Fortumo](http://smsfortumo.ru/))
 
If you know about other Android appstores that support in-app purchasing 
please [let us know](http://groups.google.com/group/opf_openiab).

We are working on integrating their IAB APIs in one OpenIAB library. Here is information about
Appstore IAB feature support:
<table>
  <tr>
    <th></th>
    <th><a href="https://play.google.com/apps/publish/">Google Play</a></th>
    <th><a href="https://developer.amazon.com/welcome.html">Amazon AppStore</a></th>
    <th><a href="http://seller.samsungapps.com/">Samsung Apps</a></th>
    <th><a href="http://dev.tstore.co.kr/devpoc/main/main.omp">SK-Telecom T-Store</a></th>
  </tr>
  <tr>
    <td>Link to IAB API description</td>
    <td><a href="http://developer.android.com/google/play/billing/index.html">Google IAB API</a></td>
    <td><a href="https://developer.amazon.com/sdk/in-app-purchasing.html">Amazon IAB API</a></td>
    <td><a href="http://developer.samsung.com/android/tools-sdks/In-App-Purchase-Library">Samsung IAB API</a></td>
    <td><a href="http://dev.tstore.co.kr/devpoc/guide/guideProd.omp#a1_5">T-Store IAB API</a></td>
  </tr>
  <tr>
    <td>Processing code</td>
    <td>Appstore</td>
    <td>Appstore</td>
    <td>Lib</td>
    <td>Lib</td>
  </tr>
  <tr>
    <td>Subscription</td>
    <td>Yes</td>
    <td>Yes</td>
    <td>No</td>
    <td>Yes</td>
  </tr>
  <tr>
    <td>Consumable goods</td>
    <td>Yes</td>
    <td>Yes</td>
    <td>Yes</td>
    <td>Yes</td>
  </tr>
  <tr>
    <td>Non-consumable goods</td>
    <td>Yes</td>
    <td>Yes</td>
    <td>No</td>
    <td>Yes</td>
  </tr>
</table>

How Can I Help?
=====
* If you are an Android app developer check <a href="https://github.com/onepf/OpenIAB/issues?labels=open+tasks&state=open">the list of open tasks</a>, check if any of these tasks is interesting for you, send a message to <a href="http://groups.google.com/group/opf_openiab">OpenIAB mailing list</a> how you want to help. <a href="https://github.com/onepf/OpenIAB">Fork OpenIAB</a> on GitHub. 
* If you are an appstore and already support In-App Billing then most probably we are already working on supporting your API in OpenIAB library, and your help is very welcome since you know your API better than anyone else! Just contact us by <a href="http://groups.google.com/group/opf_openiab">joining OpenIAB mailing list</a>. 
* If you are an appstore and do not support In-App Billing yet but plan to support it then we will be glad working with your on creating a common OpenIAB API and API. <a href="http://groups.google.com/group/opf_openiab">Join OpenIAB mailing list</a> to be involved in OpenIAB API development. 

License
=====
Source code of the OpenIAB library and the samples is available under the terms of the Apache License, Version 2.0:  
http://www.apache.org/licenses/LICENSE-2.0

The OpenIAB API specification and the related texts are available under the terms of the Creative Commons Attribution 2.5 license:  
http://creativecommons.org/licenses/by/2.5/

