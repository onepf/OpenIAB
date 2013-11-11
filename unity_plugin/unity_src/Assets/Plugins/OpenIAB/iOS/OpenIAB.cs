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
		private static extern bool canMakePayments();
		
		[DllImport ("__Internal")]
		private static extern void assignIdentifiersAndCallbackGameObject(string[] identifiers,int identifiersCount, string gameObjectName);
		
		[DllImport ("__Internal")]
		private static extern void loadProducts();	
		
		[DllImport ("__Internal")]
		private static extern bool buyProductWithIdentifier(string identifier);
		
		[DllImport ("__Internal")]
		private static extern void restoreProducts();	
	
		[DllImport ("__Internal")]
		public static extern StoreKitProduct detailsForProductWithIdentifier(string identifier);

		[DllImport ("__Internal")]
		private static extern bool isProductPurchased(string identifier);
        #endregion

		static Dictionary<string, string> _sku2storeSkuMappings = new Dictionary<string, string>();
		static Dictionary<string, string> _storeSku2skuMappings = new Dictionary<string, string>();
		static Inventory _inventory;
		static HashSet<string> _purchaseSet = new HashSet<string>();

		private bool IsDevice() {
			if (Application.platform != RuntimePlatform.IPhonePlayer) {
	            //OpenIAB.EventManager.SendMessage("OnBillingNotSupported", "editor mode");
	            return false;
	        }
			return true;
		}

		public static void CreateInventory(StoreKitProduct[] products) {
			_inventory = new Inventory(products);
		}

		public static bool IsProductPurchased(string sku) {
			return isProductPurchased(sku);
		}

		public static void AddProductToPurchaseHistory(string sku) {
			_purchaseSet.Add(sku);
		}

		public void init(Dictionary<string, string> storeKeys=null) {
			if (!IsDevice()) return;

			// Pass identifiers to the StoreKit
			string[] identifiers = new string[_sku2storeSkuMappings.Count];
			for (int i = 0; i < _sku2storeSkuMappings.Count; ++i) {
				identifiers[i] = _sku2storeSkuMappings.ElementAt(i).Value;
			}
			assignIdentifiersAndCallbackGameObject(identifiers, identifiers.Length, typeof(OpenIABEventManager).ToString());

			if (canMakePayments()) {
				loadProducts();
			} else {
				OpenIAB.EventManager.SendMessage("OnBillingNotSupported", "User cannot make payments.");	
			}
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
			if (_inventory == null) {
				OpenIAB.EventManager.SendMessage("OnQueryInventoryFailed", "Inventory is null");
			} else {
				OpenIABEventManager.OnQueryInventorySucceeded(_inventory);
			}
		}
		
		public void purchaseProduct(string sku, string developerPayload="") {
            string storeSku = _sku2storeSkuMappings[sku];
			if (!IsDevice()) {
				// Fake purchase in editor mode
                OpenIAB.EventManager.SendMessage("OnPurchaseSucceeded", storeSku);
                return;
            }
			
			if (!buyProductWithIdentifier(storeSku)) {
				OpenIAB.EventManager.SendMessage("OnPurchaseFailed", "'Failed to start purchase operation' failed");
			} else {
				_purchaseSet.Add(sku);
			}
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

			// TODO: ZALIPON =\
			if (_purchaseSet.Contains(purchase.Sku)) {
				OpenIAB.EventManager.SendMessage("OnConsumePurchaseSucceeded", purchase.Serialize());	
				_purchaseSet.Remove(purchase.Sku);
			}
		}
		
		public void restoreTransactions() {
			restoreProducts();
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
