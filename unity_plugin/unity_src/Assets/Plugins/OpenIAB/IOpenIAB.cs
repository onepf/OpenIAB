using UnityEngine;
using System.Collections;
using System.Collections.Generic;

namespace OnePF {
	public interface IOpenIAB {
	
		void init(Dictionary<string, string> storeKeys=null);
		void mapSku(string sku, string storeName, string storeSku);
		void unbindService();
		bool areSubscriptionsSupported();
		void queryInventory();
		void purchaseProduct(string sku, string developerPayload="");
		void purchaseSubscription(string sku, string developerPayload="");
		void consumeProduct(Purchase purchase);
		void restoreTransactions();
			
	}
}