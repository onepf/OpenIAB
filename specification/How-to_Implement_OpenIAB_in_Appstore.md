Introduction. Key Features of OpenIAB protocol
-------------

**Reliability.** Acquired Items are stored on Appstore side and can be retrieved by application at any time using mHelper.queryInventory(). If application is stopped by the system or crashed during item purchasing and cannot retrieve result you will find all purchased items next time when request Inventory from Appstore

**RSA Verification.** In addition to server side API for receipt verification, public and private RSA keys are used in OpenIAB protocol to sign receipt data. It allows to verify purchase is healthy right in application code without any server-to-server interaction

To support OpenIAB in your Appstore, you need to to following steps:
* Step 1. Native Client. Add bindable services to your native client to integrate with third-party applications.
* Step 2. Support on Backend. Your backend must be able to provide necessary information to services defined on step 1.
* Step 3. Server API for purchases and subscription verification. To provide developers ability verify purchases with their security server.

Step 1. Native Client. (If you have no native client, OpenIAB cannot be integrated)
-------------

To support OpenIAB in your native application two bindable services must be implemented with  following AIDL interfaces:
 
**IOpenAppstore.aidl** - interface to discover your store application by OpenIAB and connect developer’s application with your store.


**IOpenInAppBillingService.aidl** - interface for in-app billing requests and managing in-app billing transactions.


These interfaces can be found at Library project(org.onepf.oms package) in OpenIAB repository on [GitHub](https://github.com/onepf/OpenIAB)


Implementation of IOpenAppstore.aidl interface
-------------

Store application must provide a bindable service that handles `org.onepf.oms.openappstore.BIND` intent and implement API described in [IOpenAppstore.aidl](../library/src/org/onepf/oms/IOpenAppstore.aidl) file.

Following methods must be implemented to work with the OpenIAB correctly:

#### String getAppstoreName();
Every OpenStore implementation must provide their unique name. It is required for OpenId to map developer’s In-App items (SKU) to specific market.  

**Uniqueness.** It is strictly required to have unique name here to OpenIAB works properly, so it is recomended to use name like `com.companyname.storename`.

#### boolean isPackageInstaller(String packageName);
Must return `true` if OpenStore is installer of package described by packageName. By installer means that package was installed or updated by Store application. 

#### boolean isBillingAvailable(String packageName);
Must return `true` if application with packageName is listed on OpenStore backend and In-App items for app are published  and ready for use. 

#### int getPackageVersion(String packageName);
Must return version of application with packageName is listed on OpenStore backend for current device.

####Intent getBillingServiceIntent();
Should return intent to be used for binding IOpenInAppBillingService. 

**Null Intent.** This method returns `null` means that Store doesn’t support In-App billing. In that case any call of `isBillingAvailable()` must return `false`;  

#### Intent getProductPageIntent(String packageName); 
Should return intent to show application page in Store Application. This method is optional, return `null` if you don’t need to implement this feature.

#### Intent getRateItPageIntent(String packageName);
should return intent to show rate application UI in Store Application. This method is optional, return `null` if you don’t need to implement this feature.

#### Intent getSameDeveloperPageIntent(String packageName)
should return intent to show developer’s page UI in Store Application. This method is optional, return `null` if you don’t need to implement this feature.


Implementation of IOpenInAppBillingService.aidl interface.
-------------

Store application must provide a bindable service that binded by intent returned by `getBillingServiceIntent()` and implement API described in [IOpenAppBillingService.aidl](../library/src/org/onepf/oms/IOpenInAppBillingService.aidl) file.



Following methods must be implemented to work with the OpenIAB correctly:


#### int isBillingSupported(int apiVersion, String packageName, String type);
Checks support for the requested API version, package and in-app type (could be "inapp" for one-time purchases or "subs" for subscriptions). 

|name|value|description|
|----|-----|-----------|
|apiVersion|int|billing API version that the app is using.|
|packageName|string|package name of the calling app|
|type|string|the type of the in-app items being requested ("inapp" or "subs").|

Must return RESULT_OK(0) if billing is supported, or corresponding result code on failure (described in AIDL file)


#### Bundle getSkuDetails(int apiVersion, String packageName, String type, in Bundle skusBundle);
Provide details of a list of SKUs available on OpenStore backend for current package.

|name|value|description|
|----|-----|-----------|
|apiVersion|int|billing API version that the app is using.|
|packageName|string|package name of the calling app|
|type|string|the type of the in-app items being requested ("inapp" or "subs").|
|skusBunde|string|bundle containing a StringArrayList of SKUs with key "ITEM_ID_LIST"|


Must return Bundle containing the following key-value pairs
* "RESPONSE_CODE" with int value, RESULT_OK(0) if success, other response codes on failure
* "DETAILS_LIST" with a StringArrayList containing purchase information in JSON format with following fields: productId, type, price, title, description. See JSON sample of sku details below.

###### JSON sample of sku details:
```
{ 
	"productId" : "exampleSku", 
	"type" : "inapp", 
	"price" : "$5.00",
	"title : "Example Title", 
	"description" : "This is an example description" 
}
```



#### Bundle getBuyIntent(int apiVersion, String packageName, String sku, String type, String developerPayload);
Returns a pending intent to launch the purchase flow for an in-app item by providing a SKU, the type, a unique purchase token and an optional developer payload.          

|name|value|description|
|----|-----|-----------|
|apiVersion|int|billing API version that the app is using.|
|packageName|string|package name of the calling app|
|sku|string|the SKU of the in-app item as published in the developer console|
|type|string|the type of the in-app items being requested ("inapp" or "subs").|
|developerPayload|string|optional argument to be sent back with the purchase information.|



Must return Bundle containing the following key-value pairs
* "RESPONSE_CODE" with int value, RESULT_OK(0) if success, other response codes on failure as listed above.
* "BUY_INTENT" - PendingIntent to start the purchase flow. 

**Purchase Flow.** UI flow must be implemented in Native Client to handle in-app purchase. The Pending intent should be launched by developer with `startIntentSenderForResult`. When purchase flow has completed, the `onActivityResult()` will give a resultCode of OK or CANCELED. If the purchase is successful, the result data will contain the following key-value pairs
* "RESPONSE_CODE" with int value, RESULT_OK(0) if success, other response codes on failure as listed above.
* "INAPP_PURCHASE_DATA" - Purchase receipt. String in JSON format with following fields: orderId, packageName, productId, purchaseTime, purchaseToken, developerPayload. See JSON sample of purchase receipt below.
* "INAPP_DATA_SIGNATURE" - String containing the signature of the purchase data that was signed with app-specific keys.

###### JSON sample of purchase receipt:
```
{
	"orderId":"12999763169054705758.1371079406387615",
	"packageName":"com.example.app", 
	"productId":"exampleSku", 
	"purchaseTime":1345678900000, 
	"purchaseToken" : "122333444455555", 
	"developerPayload":"example developer payload" 
}
```    

#### Bundle getPurchases(int apiVersion, String packageName, String type, String continuationToken);
Returns the current SKUs owned by the user of the type and package name specified along with purchase information and a signature of the data to be validated.

|name|value|description|
|----|-----|-----------|
|apiVersion|int|billing API version that the app is using.|
|packageName|string|package name of the calling app|
|type|string|the type of the in-app items being requested ("inapp" or "subs").|
|continuationToken|string|to be set as null for the first call, if the number of owned skus are too many, a continuationToken is returned in the response bundle. This method can be called again with the continuation token to get the next set of owned skus.|

     
Must return Bundle containing the following key-value pairs
* "RESPONSE_CODE" - int value, RESULT_OK(0) if success, other response codes on failure.
* "INAPP_PURCHASE_ITEM_LIST" - StringArrayList containing the list of SKUs
* "INAPP_PURCHASE_DATA_LIST" - StringArrayList containing the purchase information
* "INAPP_DATA_SIGNATURE_LIST"- StringArrayList containing the signatures of the purchase information
* "INAPP_CONTINUATION_TOKEN" - String containing a continuation token for the next set of in-app purchases. Only set if the user has more owned skus than the current list.

#### int consumePurchase(int apiVersion, String packageName, String purchaseToken);
Consume the last purchase of the given SKU. This will result in this item being removed
from all subsequent responses to getPurchases() and allow re-purchase of this item.

|name|value|description|
|----|-----|-----------|
|apiVersion|int|billing API version that the app is using.|
|packageName|string|package name of the calling app|
|purchaseToken|string|token in the purchase information JSON that identifies the purchase to be consumed|

Must return 0 if consumption succeeded. Appropriate error values for failures.


#### Notes:
**Offline mode.** It’s better to cache all inventory of user for every app in Native Client cache, so applications with in-app purchases can start and provide all purchased features when device has no internet connection


Step 2. Support on BackEnd
-------------
When developer register new application private/public RSA keys need to be generated. Every purchase need to be signed using private key. Public key should be provided to a developer of the application to verify purchase.

Appstore backend should provide enough information to Native Client to implement services defined on Step 1.  

Step 3. Server API for purchases and subscriptions verification
-------------
To provide developers ability verify purchases with their security server, OpenStore should provide following REST API methods:

#### In-App Purchase status method
Provide developer the purchase and consumption status of an inapp item

###### Request

```
GET https://<Your API server address>/{packageName}/inapp/{productId}/purchases/{token}
```

###### Parameters
|name|value|description|
|----|-----|-----------|
|packageName|string|The package name of the application the inapp product was sold in (for example, 'com.some.thing').|
|productId|string|The inapp product SKU (for example, 'com.some.thing.inapp1').|
|token|string|The token provided to the user's device when the inapp product was purchased.|

###### Response 
If successful, this method should return response as JSON string in the following format 
 
```
{
	"kind": "androidpublisher#inappPurchase",
	"purchaseTime": {long},
  	"purchaseState": {integer},
  	"consumptionState": {integer},
  	"developerPayload": {string}
}
```

|name|value|description|
|----|-----|-----------|
|kind|string|This kind represents a inappPurchase object in the Appstore|
|purchaseTime|long|The time the product was purchased, in millis since the epoch (Jan 1, 1970).|
|purchaseState|int|Purchase state. Possible values: 0 - Purchases, 1 - Canceled|
|consuptionState|int|Consumption state. Possible values: 0 - Consumed, 1 - to be consumed|
|developerPayload|string|A developer-specified string that contains supplemental information about an order.|

#### Subscription status method
Provide developer a user's subscription purchase status and returns its expiry time.

###### Request

```
GET https://<Your API server address>/{packageName}/subscriptions/{subscriptionId}/purchases/{token}
```

###### Parameters
|name|value|description|
|----|-----|-----------|
|packageName|string|The package name of the application wich this subscription was purchased (for example, 'com.some.thing').|
|subscriptionId|string|The purchased subscription ID (for example, 'monthly001').|
|token|string|The token provided to the user's device when the subscription was purchased.|

###### Response 
 If successful, this method should return response as JSON string in the following format 
 
```
{
	"kind": "androidpublisher#inappPurchase",
	"initiationTimestampMsec": {long},
  	"validUntilTimestampMsec": {long},
  	"autoRenewing": {boolean},
}
```

|name|value|description|
|----|-----|-----------|
|kind|string|This kind represents a subscriptionPurchase object in the Appstore|
|initiationTimestampMsec|long|Time at which the subscription was granted, in milliseconds since Epoch.|
|validUntilTimestampMsec|long|Time at which the subscription will expire, in milliseconds since Epoch.|
|autoRenewing|boolean|Whether the subscription will automatically be renewed when it reaches its current expiry time.|

#### Cancel Subscription method
Provide developer a user's subscription purchase status and returns its expiry time.

###### Request

```
POST https://<Your API server address>/{packageName}/subscriptions/{subscriptionId}/purchases/{token}/cancel
```

###### Parameters
|name|value|description|
|----|-----|-----------|
|packageName|string|The package name of the application wich this subscription was purchased (for example, 'com.some.thing').|
|subscriptionId|string|The purchased subscription ID (for example, 'monthly001').|
|token|string|The token provided to the user's device when the subscription was purchased.|

###### Response 
If successful, this method should returns an empty response body.

#### Authorization
Methods are desribed above must require authorization. Appstore should provide
developer token to use as authorization token.

Two possible ways of using token must be supported:

* As query parameter `https://<Your API server address>/...?access_token=...`
* As `Authorization` header of the request
  
