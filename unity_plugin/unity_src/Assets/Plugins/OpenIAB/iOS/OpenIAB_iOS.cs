using UnityEngine;
using System.Collections;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Linq;

namespace OnePF {
    public class OpenIAB_iOS
#if UNITY_IOS
	: IOpenIAB 
#endif
 {
        public static readonly string STORE = "appstore";

#if UNITY_IOS
        #region NativeMethods
		[DllImport ("__Internal")]
        private static extern bool AppStore_canMakePayments();
		
		[DllImport ("__Internal")]
        private static extern void AppStore_requestProducts(string[] skus, int skusNumber);
	
		[DllImport ("__Internal")]
        private static extern void AppStore_startPurchase(string sku);
		
		[DllImport ("__Internal")]
		private static extern void AppStore_restorePurchases();	

        [DllImport ("__Internal")]
        private static extern bool Inventory_hasPurchase(string sku);

        [DllImport ("__Internal")]
        private static extern void Inventory_query();

        [DllImport ("__Internal")]
        private static extern void Inventory_removePurchase(string sku);
        #endregion

		static Dictionary<string, string> _sku2storeSkuMappings = new Dictionary<string, string>();
		static Dictionary<string, string> _storeSku2skuMappings = new Dictionary<string, string>();

		private bool IsDevice() {
			if (Application.platform != RuntimePlatform.IPhonePlayer) {
	            return false;
	        }
			return true;
		}

        public void init(Options options) {
            if (!IsDevice()) return;
            init(options.storeKeys);
        }

		public void init(Dictionary<string, string> storeKeys=null) {
			if (!IsDevice()) return;

            if (!AppStore_canMakePayments()) {
                OpenIAB.EventManager.SendMessage("OnBillingNotSupported", "User cannot make payments.");
                return;
            }

            // Pass identifiers to the StoreKit
            string[] identifiers = new string[_sku2storeSkuMappings.Count];
            for (int i = 0; i < _sku2storeSkuMappings.Count; ++i) {
                identifiers[i] = _sku2storeSkuMappings.ElementAt(i).Value;
            }

			AppStore_requestProducts(identifiers, identifiers.Length);
		}
		
		public void mapSku(string sku, string storeName, string storeSku) {
			if (storeName == STORE) {
				_sku2storeSkuMappings[sku] = storeSku;
				_storeSku2skuMappings[storeSku] = sku;
			}
		}
		
		public void unbindService() {
		}
		
		public bool areSubscriptionsSupported() {
			return true;
		}

        public void queryInventory() {
            if (!IsDevice()) {
				return;
			}
            Inventory_query();
        }

		public void queryInventory(string[] skus) {
            queryInventory();
		}

		public void purchaseProduct(string sku, string developerPayload="") {
            string storeSku = _sku2storeSkuMappings[sku];
			if (!IsDevice()) {
				// Fake purchase in editor mode
                OpenIAB.EventManager.SendMessage("OnPurchaseSucceeded", storeSku);
                return;
            }
			
            AppStore_startPurchase(storeSku);
		}
		
		public void purchaseSubscription(string sku, string developerPayload="") {
			purchaseProduct(sku, developerPayload);
		}
		
		
		public void consumeProduct(Purchase purchase) {
            if (!IsDevice()) {
				// Fake consume in editor mode
                OpenIAB.EventManager.SendMessage("OnConsumePurchaseSucceeded", purchase.Serialize());
                return;
            }

			var storeSku = OpenIAB_iOS.Sku2StoreSku(purchase.Sku);
            if (Inventory_hasPurchase(storeSku)) {
                OpenIAB.EventManager.SendMessage("OnConsumePurchaseSucceeded", purchase.Serialize());
                Inventory_removePurchase(storeSku);
            } else {
				OpenIAB.EventManager.SendMessage("OnConsumePurchaseFailed", "Purchase not found");
			}
		}
		
		public void restoreTransactions() {
            AppStore_restorePurchases();
		}

        public bool isDebugLog() {
            return false;
        }

        public void enableDebugLogging(bool enabled) {
        }

        public void enableDebugLogging(bool enabled, string tag) {
        }

		public static string StoreSku2Sku(string storeSku) {
			return _storeSku2skuMappings[storeSku];
		}

		public static string Sku2StoreSku(string sku) {
			return _sku2storeSkuMappings[sku];
		}
#endif
    }
}
