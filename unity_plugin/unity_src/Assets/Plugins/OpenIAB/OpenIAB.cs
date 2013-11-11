using UnityEngine;
using System.Collections;
using System.Collections.Generic;
using System;

namespace OnePF {
    public class OpenIAB {	
        
		public static GameObject EventManager { get { return GameObject.Find(typeof(OpenIABEventManager).ToString()); } }
		
		static IOpenIAB _billing;
		
        static OpenIAB() {
#if UNITY_ANDROID
			_billing = new OpenIAB_Android();
            Debug.Log("********** Android OpenIAB plugin initialized **********");
#elif UNITY_IOS
			_billing = new OpenIAB_iOS();
            Debug.Log("********** iOS OpenIAB plugin initialized **********");
#else
			Debug.LogError("OpenIAB billing not supported on this platform. Sorry.");
#endif
        }

		// Should be only called before init
        public static void mapSku(string sku, string storeName, string storeSku) {
            _billing.mapSku(sku, storeName, storeSku);
        }
		
        // Starts up the billing service. This will also check to see if in app billing is supported and fire the appropriate event
        public static void init(Dictionary<string, string> storeKeys) {
			_billing.init(storeKeys);
        }

        // Unbinds and shuts down the billing service
        public static void unbindService() {
            _billing.unbindService();
        }

        public static bool areSubscriptionsSupported() {
            return _billing.areSubscriptionsSupported();
        }

        // Sends a request to get all completed purchases and product information
        public static void queryInventory() {
            _billing.queryInventory();
        }

        // Purchases the product with the given sku and developerPayload
        public static void purchaseProduct(string sku, string developerPayload="") {
            _billing.purchaseProduct(sku, developerPayload);
        }

        // Purchases the subscription with the given sku and developerPayload
        public static void purchaseSubscription(string sku, string developerPayload="") {
            _billing.purchaseSubscription(sku, developerPayload);
        }

        // Sends out a request to consume the product
        public static void consumeProduct(Purchase purchase) {
            _billing.consumeProduct(purchase);
        }
		
		public static void restoreTransactions() {
			_billing.restoreTransactions();
		}
    }
}
