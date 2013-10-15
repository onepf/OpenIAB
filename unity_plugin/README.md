OpenIAB - Unity Plugin
=====

This library is Java part of our Unity plugin. 

Integration
=====

One simple step. Download Unity [package](http://127.0.0.1) and import it in your project. There is OpenIAB.jar file in the package, which contains compile output of the plugin and of the [OpenIAB](/onepf/OpenIAB) library.
There is also AndroidManifest.xml in the /Assets/Plugins/Android. Developer can add project specific settings to it.

Now you can run demo scene with some test buttons.

API
=====

All work is done through the two classes: _OpenIAB_, _OpenIABEventManager_.
First you need to place **OpenIABEventManager** prefab on the scene and subscribe to the following static events in _OpenIABEventManager_:

```
// Fired after init is called when billing is supported on the device
public static event Action billingSupportedEvent;

// Fired after init is called when billing is not supported on the device
public static event Action<string> billingNotSupportedEvent;

// Fired when the inventory and purchase history query has returned
public static event Action<Inventory> queryInventorySucceededEvent;

// Fired when the inventory and purchase history query fails
public static event Action<string> queryInventoryFailedEvent;

// Fired when a purchase of a product or a subscription succeeds
public static event Action<Purchase> purchaseSucceededEvent;

// Fired when a purchase fails
public static event Action<string> purchaseFailedEvent;

// Fired when a call to consume a product succeeds
public static event Action<Purchase> consumePurchaseSucceededEvent;

// Fired when a call to consume a product fails
public static event Action<string> consumePurchaseFailedEvent;
```

OpenIAB initializes itself in static constructor, but you need to call _init_ method passing it preferred stores list with public keys, in order to start billing service.

Then you can map sku's by calling _mapSku_ and use rest of the API.

This is full list of the provided methods:

```
// Starts up the billing service. This will also check to see if in app billing is supported and fire the appropriate event
public static void init(Dictionary<string, string> storeKeys);

// Maps sku for the supported stores
public static void mapSku(string sku, string storeName, string storeSku);

// Unbinds and shuts down the billing service
public static void unbindService();

// Returns whether subscriptions are supported on the current device
public static bool areSubscriptionsSupported();

// Sends a request to get all completed purchases and product information
public static void queryInventory();

// Purchases the product with the given sku and developerPayload
public static void purchaseProduct(string sku, string developerPayload="");

// Purchases the subscription with the given sku and developerPayload
public static void purchaseSubscription(string sku, string developerPayload="");

// Sends out a request to consume the product
public static void consumeProduct(Purchase purchase);
```