How To add OpenIAB into an app
=====
1. Download the latest version of OpenIAB.jar from http://www.onepf.org/openiab and attach it to the project.
Or clone the library `git clone https://github.com/onepf/OpenIAB.git` and add /library as a Library Project.

2. Map Google Play SKUs to Yandex/Amazon/etc SKUs like this:
https://github.com/onepf/OpenIAB/blob/master/samples/trivialdrive/src/org/onepf/trivialdrive/MainActivity.java#L109

3. Instantiate `new OpenIabHelper`  and call `helper.startSetup()`.
When setup is done call  `helper.queryInventory()`
    ```java
      helper = new OpenIabHelper(this, storeKeys);
      helper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
          public void onIabSetupFinished(IabResult result) {
              if (!result.isSuccess()) {
                  complain("Problem setting up in-app billing: " + result);
                  return;
              }
              Log.d(TAG, "Setup successful. Querying inventory.");
                  helper.queryInventoryAsync(mGotInventoryListener);
              }
      });
    ```
https://github.com/onepf/OpenIAB/blob/master/samples/trivialdrive/src/org/onepf/trivialdrive/MainActivity.java#L196

4. Handle the results of `helper.queryInventory()` in an inventory listener and update UI to show what was purchased
https://github.com/onepf/OpenIAB/blob/master/samples/trivialdrive/src/org/onepf/trivialdrive/MainActivity.java#L223

5. To process purchases you need to override `onActivityResult()` of your Activity
    ```java
       @Override
       protected void onActivityResult(int requestCode, int resultCode, Intent data) {
           // Pass on the activity result to the helper for handling
           mHelper.handleActivityResult(requestCode, resultCode, data));
       }
    ```
When the user requests purchase of an item, call  `helper.launchPurchaseFlow()`
https://github.com/onepf/OpenIAB/blob/master/samples/trivialdrive/src/org/onepf/trivialdrive/MainActivity.java#L294
and handle the results with the listener
https://github.com/onepf/OpenIAB/blob/master/samples/trivialdrive/src/org/onepf/trivialdrive/MainActivity.java#L396

6. If the user has purchased a consumable item, call  ``` helper.consume() ```
to exclude it from the inventory. If the item is not consumed, a store supposes it as non-consumable item and doesn't allow to purchase it one more time. Also it will be returned by ``` helper.queryInventory() ``` next time
https://github.com/onepf/OpenIAB/blob/master/samples/trivialdrive/src/org/onepf/trivialdrive/MainActivity.java#L415

7. Specify keys for different stores like this:
https://github.com/onepf/OpenIAB/blob/master/samples/trivialdrive/src/org/onepf/trivialdrive/MainActivity.java#L188

8. Add the required permissions to the AndroidManifest.xml

    ```xml
    <!--all-->
    <uses-permission android:name="android.permission.INTERNET"/>
    <!--Google Play-->
    <uses-permission android:name="com.android.vending.BILLING" />
    <!--Open Store-->
    <uses-permission android:name="org.onepf.openiab.permission.BILLING" />
    <!--Amazon-->
    <!--Amazon requires no permissions -->
    <!--Samsung Apps-->
    <uses-permission android:name="com.sec.android.iap.permission.BILLING" />
    <!--Fortumo-->
    <uses-permission android:name="android.permission.RECEIVE_SMS"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.SEND_SMS" />
    <!--SlideME-->
    <uses-permission android:name="com.slideme.sam.manager.inapp.permission.BILLING" />
    ```

    Be careful using sms permissions. If you want to support devices without sms functionality, don't forget to add

      ```xml
      <uses-feature android:name="android.hardware.telephony" android:required="false"/>
      ```

9. Edit your proguard config file

    ```
    # GOOGLE
    -keep class com.android.vending.billing.**

    # AMAZON
    -dontwarn com.amazon.**
    -keep class com.amazon.** {*;}
    -keepattributes *Annotation*
    -dontoptimize

    # SAMSUNG
    -keep class com.sec.android.iap.**
    
    # NOKIA
    -keep class com.nokia.payment.iap.aidl.**

    #FORTUMO
    -keep class mp.** { *; }
    ```

10. Troubleshooting: additional logging is very helpful if you trying to understand what's wrong with configuration or raise issue:
    ```java
    helper.enableDebugLogging(true);
    ```



Support instructions for the stores
=====

Google Play and Open Stores
-------------
1. Add the corresponding billing permissions

    ```xml
    <uses-permission android:name="com.android.vending.BILLING" />
    <uses-permission android:name="org.onepf.openiab.permission.BILLING" />
    ```

2. Provide your public keys

    ```java
    Map<String, String> storeKeys = new HashMap<String, String>();
    storeKeys.put(OpenIabHelper.NAME_GOOGLE, googleBase64EncodedPublicKey);
    storeKeys.put(OPEN_STORE_NAME, openStoreBase64EncodedPublicKey);
    OpenIabHelper.Options options = new OpenIabHelper.Options();
    options.storeKeys = storeKeys;
    mHelper = new OpenIabHelper(this, options);
    //or
    mHelper = new OpenIabHelper(this, storeKeys);
    ```
    otherwise verify purchases on your server side.

3. Map the SKUs if they are different for the required stores

    ```java
    OpenIabHelper.mapSku(SKU_PREMIUM, OpenIabHelper.STORE_NAME, "org.onepf.trivialdrive.storename.premium");
    OpenIabHelper.mapSku(SKU_GAS, OpenIabHelper.STORE_NAME, "org.onepf.trivialdrive.storename.gas");
    OpenIabHelper.mapSku(SKU_INFINITE_GAS, OpenIabHelper.STORE_NAME, "org.onepf.trivialdrive.storename.infinite_gas");
    ```

4. In the proguard configuration file

    ```proguard
     # GOOGLE
     -keep class com.android.vending.billing.**
     ```

5. To test .apk with Google Play please ensure
    - your .apk submitted to Google Play Developer Console
    - your .apk is signed by production key
    - versionCode in AndroidManifest.xml of your .apk equal to versionCode of .apk submitted to Developer Console


Receipt Verification on Server
---------------------

1. Create OpenIabHelper with "Skip signature verification" option and no publicKeys. If you specify no publicKeys and forget VERIFY_SKIP option, an IllegalArgumentException will be thrown

    ```java
    Options opts = new OpenIabHelper.Options();
    opts.verifyMode = Options.VERIFY_SKIP;
    mHelper = new OpenIabHelper(context, opts);
    ```

2. Get receipt's data and signature from Purchase object and send it to your server

    ```java
    new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            // … different result checks ...
            String receiptData = purchase.getOriginalJson();
            String receiptSignature = purchase.getSignature();
            String storeName = purchase.getAppstoreName();
            String urlToContent  = yourRequestReceiptVerificationOnServer(receiptData, receiptSignature, storeName);
            // … further code ...
        }
    }
    ```

Amazon
-------------
1. In the AndroidManifest.xml declare the receiver

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

2. Map the SKUs if required.
Remember, the SKUs must be unique across your Amazon developer account.

    ```java
    OpenIabHelper.mapSku(SKU_PREMIUM, OpenIabHelper.NAME_AMAZON, "org.onepf.trivialdrive.amazon.premium");
    OpenIabHelper.mapSku(SKU_GAS, OpenIabHelper.NAME_AMAZON, "org.onepf.trivialdrive.amazon.gas");
    OpenIabHelper.mapSku(SKU_INFINITE_GAS, OpenIabHelper.NAME_AMAZON, "org.onepf.trivialdrive.amazon.infinite_gas");
    ```

3. In the proguard config file add

    ```proguard
     # AMAZON
    -dontwarn com.amazon.**
    -keep class com.amazon.** {*;}
    -keepattributes *Annotation*
    -dontoptimize
    ```

4. If OpenIAB added as library project, Amazon SDK in-app-purchasing-1.0.3.jar should exist in build-path (/libs)

5. To test .apk with Amazon SDK Tester some steps are needed:

    - Download and install Amazon SDK Tester from Amazon website
    - Download JSON with in-app products from Amazon Developer Console and put JSON with in-app products to /mnt/sdcard
    - Install your .apk with special option to help OpenIAB choose Amazon protocol
    ```bash
    # install for Amazon SDK Tester:
    adb install -i com.amazon.venezia /path/to/YourApp.apk
    ```


Samsung Apps
-------------
1. In the AndroidManifest.xml add the corresponding billing permission

    ```xml
     <uses-permission android:name="com.sec.android.iap.permission.BILLING" />
    ```

2. Map the SKUs if required.
   Remember, Samsung Apps describes an item it terms of Item Group ID and Item ID.

   ```java
   //format "group_id/item_id"
   OpenIabHelper.mapSku(SKU_PREMIUM, OpenIabHelper.NAME_SAMSUNG, "100000100696/000001003746");
   OpenIabHelper.mapSku(SKU_GAS, OpenIabHelper.NAME_SAMSUNG, "100000100696/000001003744");
   OpenIabHelper.mapSku(SKU_INFINITE_GAS, OpenIabHelper.NAME_SAMSUNG, "100000100696/000001003747");
   ```

3. Instantiate ``` new OpenIabHelper ``` using an Activity instance.
   Activity context is required to call  ``` startActivityForResult() ``` for SamsungAccount Activity.

4. In the proguard config file add

    ```proguard
    # SAMSUNG
    -keep class com.sec.android.iap.**
    ```
5. To test your .apk with SamsungApps following steps are needed:
    - Ensure SamsungApps is installed on your device
    - Ensure Samsung IAP Service is installed on your device
    - Install your .apk with special option to help OpenIAB choose SamsunApps
    ```bash
    # install for SamsungApps:
    adb install -i com.sec.android.app.samsungapps /path/to/YourApp.apk
    ```

Nokia IAP
---------
1. In the AndroidManifest.xml add the corresponding billing permission

    ```xml
    <uses-permission android:name="com.nokia.payment.BILLING"/>
    ```

2. Map the SKUs if required.

    ```java
    OpenIabHelper.mapSku(SKU_PREMIUM, OpenIabHelper.NAME_NOKIA, "1023608");
    OpenIabHelper.mapSku(SKU_GAS, OpenIabHelper.NAME_NOKIA, "1023609");
    OpenIabHelper.mapSku(SKU_INFINITE_GAS, OpenIabHelper.NAME_NOKIA, "1023610");
    ```

3. In the proguard configuration file

    ```proguard
    # NOKIA
    -keep class com.nokia.payment.iap.aidl.**
    ```
4. To test your .apk with Nokia Store Install your .apk with special option to help OpenIAB choose Nokia protocol

    ```bash
    # install for Nokia Store:
    adb install -i com.nokia.payment.iapenabler /path/to/YourApp.apk
    ```

SlideME
-------------
1. In the AndroidManifest.xml add the corresponding billing permission

    ```xml
     <uses-permission android:name="com.slideme.sam.manager.inapp.permission.BILLING" />
    ```
    
2. To test your application with SlideME store install your .apk with special option

    ```bash
    # install for SlideME:
    adb install -i com.slideme.sam.manager /path/to/YourApp.apk
    ```

Fortumo: carrier billing and NOOK
=================================

Before start to work with OpenIAB library
-----------------------------------------
Create a <a href="http://fortumo.com/?utm_source=openiab&utm_medium=openiab&utm_campaign=openiab">Fortumo account</a> and add a required number of <a href="http://developers.fortumo.com/in-app-purchasing-on-nook/">NOOK</a> and <a href="http://developers.fortumo.com/in-app-purchasing-on-android/">Android</a> services.
One service corresponds to one price, e.g. for 3 in-apps with different prices you should create 3 different services.

OpenIAB setup
-------------
1. Make sure that <a href="https://github.com/onepf/OpenIAB/blob/master/library/libs/FortumoInApp-android-9.1.2-o.jar">FortumoInApp-android-9.1.2-o.jar</a> is attached to the project.

2. In the AndroidManifest.xml add the following permissions

    ```xml
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.RECEIVE_SMS" />
    <uses-permission android:name="android.permission.SEND_SMS" />
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    ```
   and declare the Fortumo SDK objects

     ```xml
     <!-- Declare these objects, this is part of Fortumo SDK,
         and should not be called directly -->
      <receiver android:name="mp.MpSMSReceiver">
            <intent-filter>
                <action android:name="android.provider.Telephony.SMS_RECEIVED"/>
            </intent-filter>
        </receiver>
        <service android:name="mp.MpService"/>
        <service android:name="mp.StatusUpdateService"/>
        <activity android:name="mp.MpActivity"
                  android:theme="@android:style/Theme.Translucent.NoTitleBar"
                  android:configChanges="orientation|keyboardHidden|screenSize"/>
       ```
    if you want to support devices without sms functionality, add

      ```xml
      <uses-feature android:name="android.hardware.telephony" android:required="false"/>
      ```
3. In the code setup an Options object

    ```java
    OpenIabHelper.Options options = new OpenIabHelper.Options();
    //set supportFortumo flag to true
    options.supportFortumo = true;
    //or
    List<Appstore> storeList = new ArrayList<Appstore>();
    storeList.add(new FortumoStore(this));
    //by the way, you can add other stores object to the list
    options.availableStores = storeList;
    mHelper = new OpenIabHelper(this, options);
    ```
4. Add <a href="https://github.com/onepf/AppDF/blob/xsd-for-inapps/specification/inapp-description.xsd">inapps_products.xml</a> (in-app products description in terms similar to Google Play) and
<a href="https://github.com/onepf/AppDF/blob/xsd-for-inapps/specification/fortumo-products-description.xsd">fortumo_inapps_details.xml</a> (data about your Fortumo services,
need to be copy-pasted from <a href="http://developers.fortumo.com/getting-started/dashboard-and-reporting/">Dashboard.</a>) files to the assets folder.
You can find a sample <a href="https://github.com/onepf/OpenIAB/tree/master/samples/trivialdrive/assets">here.</a>
Example of inapp-products.xml

     ```xml
     <inapp-products>
         <!--Zero or more repetitions:-->
         <items>
             <!--Zero or more repetitions:-->
             <item id="sku_gas"
                   publish-state="published"> <!-- id: the same format as Google SKU, required; published: "published" or "unpublushed", required by the xsd, but is not actually used it the current implementation-->
                 <summary><!--encapsulates all elements related to description, required-->
                     <summary-base><!--default strings elements, required-->
                         <title>1/4 of gas tank</title> <!-- default title, required-->
                         <description>Some gas to go further</description> <!-- default description, required-->
                     </summary-base>
                     <!--Zero or more repetitions:-->
                     <summary-localization locale="ru_RU"> <!-- locale: [a-z]_[A-Z], required-->
                         <title>Четверть бака бензина</title> <!-- required -->
                         <description>Немного топлива, чтобы проехать еще</description> <!-- required -->
                     </summary-localization>
                     <summary-localization locale="en_US">
                         <title>1/4 of gas tank</title>
                         <description>Some gas to go further</description>
                     </summary-localization>
                 </summary>
                 <price autofill="true"> <!-- autofill: true or false, required by the xsd, but is not used in the current implementation-->
                     <price-base>1.00</price-base> <!-- default price, required-->
                     <!--Zero or more repetitions:-->
                     <price-local country="RU"> <!-- country: [A-Z][A_Z], required-->
                         30.00</price-local>
                     <price-local country="EN">1.00</price-local>
                 </price>
             </item>
            ...
         </items>
     </inapp-products>
     ```
     Example of fortumo_inapps_details.xml.

     ```xml
     <fortumo-products>
         <!--
         <product                    // mapping for particular fortumo service
             id="sku_gas"            // SKU with rules same to Appstore SKU
             service-id="61730610ade8f2f754bb3bd4b0c1fd0e"           // Fortumo serviceId need to be copy-pasted from Dashboard
             service-inapp-secret="cbc0da3763e59eee2d5a523fe5761346" // Fortumo inapp-secret need to be copy-pasted from Dashboard
             nook-service-id="61730610ade8f2f754bb3bd4b0c1fd0e"           // Fortumo NOOK serviceId need to be copy-pasted from Dashboard
             nook-service-inapp-secret="cbc0da3763e59eee2d5a523fe5761346" // Fortumo NOOK inapp-secret need to be copy-pasted from Dashboard
             consumable="true"/>     // consumable or not - currently is necessary parameter as wellas for Amazon
         -->
         <product
                 id="sku_gas"
                 service-id="61730610ade8f2f754bb3bd4b0c1fd0e"
                 service-inapp-secret="cbc0da3763e59eee2d5a523fe5761346"
                 nook-service-id="f2ca9394861085d34158e09cedd87738"
                 nook-service-inapp-secret="78629d599dac532856cc2e90dd69e772"
                 consumable="true"/>
         <product
                 id="sku_premium"
                 service-id="61730610ade8f2f754bb3bd4b0c1fd0e"
                 service-inapp-secret="cbc0da3763e59eee2d5a523fe5761346"
                 nook-service-id="f2ca9394861085d34158e09cedd87738"
                 nook-service-inapp-secret="78629d599dac532856cc2e90dd69e772"
                 consumable="false"/>
     </fortumo-products>
     ```
     Both files can be created using <a href="http://www.onepf.org/editor/">AppDF Editor.</a>

5. In the proguard config file add

    ```proguard
     # FORTUMO
     -keep class mp.** { *; }
     ```

Unity Plugin
=====
There is also Unity engine [plugin](unity_plugin) that will simplify integration for C#/JavaScript developers. No need to write any java code.

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
OpenIAB SDK is used in production by wide variety of application and games. OpenIAB packages are available for Android apps and games based on Unity3d or Marmalade SDK. OpenIAB protocol is implemented by several Appstores.

We have some samples that works in any Appstore in our [samples folder](https://github.com/onepf/OpenIAB/tree/master/samples). To find differences between TrivialDrive provided by Google and TrivialDrive with OpenIAB, please check our [sample](https://github.com/onepf/OpenIAB/tree/master/samples/trivialdrive). It demonstrates what changes need to be done to work with all Appstores and Carrier Billing.

If you an Appstore developer and want to know how to integrate OpenIAB protocol in your Appstore, please start with our [Step-By-Step How-To](https://github.com/onepf/OpenIAB/blob/master/specification/How-to_Implement_OpenIAB_in_Appstore.md)

Basic Principles
=====
* **As close to Google Play In-app Billing API as possible** - we optimize the OpenIAB library by the following parameter "lines on code you need to change in an app that already works in Google Play to make it working in all the appstores"
* **One APK works in all appstores** - OpenIAB chooses proper billing method automatically or follows your requirements
* **Open In-App Billing protocol** - OpenIAB is designed provide lightweight solution that supports hundreds of appstores. When appstore implement OpenIAB protocol on appstore side all applications with OpenIAB become fully compatible with new appstore without recompile.
* **No middle man**

No Middle Man
=====
OpenIAB is an open source library that handles OpenIAB protocol and wraps some already existing IAB SDKs as well.
It is important to understand that all payments are processed directly by appstore and there is no a middle man
staying between the app developers and the appstores.
OpenIAB is not a payment service. It is just an API how the apps communicate with appstores to request in-app billing.
There is a common open API all the stores can use instead of each new store implement their own API
and developers have to integrate all these different APIs in their apps.


How Can I Help?
=====

* If you know about issues we missed - please, let us know in <a href="https://github.com/onepf/OpenIAB/issues">Issues on GitHub</a>
* If you have contacts with Appstore you like, ask them to implement <a href="https://github.com/onepf/OpenIAB/blob/master/specification/How-to_Implement_OpenIAB_in_Appstore.md">OpenIAB</a> on their side
* If you are an Android app developer check <a href="https://github.com/onepf/OpenIAB/issues?state=open">the list of open tasks</a>, see if any of these tasks interests you and comment it. <a href="https://github.com/onepf/OpenIAB">Fork OpenIAB</a> on GitHub and submit your code</li>
* If you are an Appstore and already support In-App Billing we will be happy to meet with your API and find best way to make it compatible with OpenIAB. Please, raise an <a href="https://github.com/onepf/OpenIAB/issues?state=open">Issue</a> to let us know</li>
* If you are an appstore that does not yet support in-app billing, but plans to support it, then we will be glad to help you with OpenIAB API. Please check our <a href="https://github.com/onepf/OpenIAB/blob/master/specification/How-to_Implement_OpenIAB_in_Appstore.md">How-To</a> and contact us to get deeper explanation of questions you have by raising an <a href="https://github.com/onepf/OpenIAB/issues?state=open">Issue</a></li>


License
=====
Source code of the OpenIAB library and the samples is available under the terms of the Apache License, Version 2.0:
http://www.apache.org/licenses/LICENSE-2.0

The OpenIAB API specification and the related texts are available under the terms of the Creative Commons Attribution 2.5 license:
http://creativecommons.org/licenses/by/2.5/

