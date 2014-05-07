using UnityEngine;
using System.Collections;
using System.Collections.Generic;

namespace OnePF {
	public interface IOpenIAB {

        void init(Options options);
		void mapSku(string sku, string storeName, string storeSku);
		void unbindService();
		bool areSubscriptionsSupported();
        void queryInventory();
        void queryInventory(string[] inAppSkus);
		void purchaseProduct(string sku, string developerPayload="");
		void purchaseSubscription(string sku, string developerPayload="");
		void consumeProduct(Purchase purchase);
		void restoreTransactions();

        bool isDebugLog();
        void enableDebugLogging(bool enabled);
        void enableDebugLogging(bool enabled, string tag);
	
    }
}