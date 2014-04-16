using UnityEngine;
using System.Collections;
using System.Collections.Generic;
using System;
using OnePF.WP8;

namespace OnePF
{
    public class OpenIAB_WP8
#if UNITY_WP8
 : IOpenIAB
#endif
    {
        public static readonly string STORE = "wp8_store";

#if UNITY_WP8

        static Dictionary<string, string> _sku2storeSkuMappings = new Dictionary<string, string>();
        static Dictionary<string, string> _storeSku2skuMappings = new Dictionary<string, string>();

        public static string GetSku(string storeSku)
        {
            return _storeSku2skuMappings.ContainsKey(storeSku) ? _storeSku2skuMappings[storeSku] : storeSku;
        }

        public static string GetStoreSku(string sku)
        {
            return _sku2storeSkuMappings.ContainsKey(sku) ? _sku2storeSkuMappings[sku] : sku;
        }

        static OpenIAB_WP8()
        {
            Store.PurchaseSucceeded += (storeSku, payload) => 
            {
                string sku = GetSku(storeSku);
                Purchase purchase = Purchase.CreateFromSku(sku, payload);
                OpenIAB.EventManager.SendMessage("OnPurchaseSucceeded", purchase); 
            };
            Store.PurchaseFailed += (error) => { OpenIAB.EventManager.SendMessage("OnPurchaseFailed", error); };
            
            Store.ConsumeSucceeded += (storeSku) => 
            { 
                string sku = GetSku(storeSku);
                Purchase purchase = Purchase.CreateFromSku(sku);
                OpenIAB.EventManager.SendMessage("OnConsumePurchaseSucceeded", purchase); 
            };
            Store.ConsumeFailed += (error) => { OpenIAB.EventManager.SendMessage("OnConsumePurchaseFailed", error); };
            
            Store.LoadListingsSucceeded += (listings) =>
            {
                Inventory inventory = GetInventory();
                foreach (KeyValuePair<string, ProductListing> pair in listings)
                {
                    SkuDetails skuDetails = new SkuDetails(pair.Value);
                    inventory.AddSkuDetails(skuDetails);
                }
                OpenIAB.EventManager.SendMessage("OnQueryInventorySucceeded", inventory);
            };
            Store.LoadListingsFailed += (error) =>
            {
                OpenIAB.EventManager.SendMessage("OnQueryInventoryFailed", error);
            };
        }

        private static Inventory GetInventory()
        {
            var inventory = new Inventory();
            var purchasesList = Store.Inventory;
            foreach (string storeSku in purchasesList)
            {
                Purchase purchase = Purchase.CreateFromSku(GetSku(storeSku));
                inventory.AddPurchase(purchase);
            }
            return inventory;
        }

        public void init(Options options)
        {
            OpenIAB.EventManager.SendMessage("OnBillingSupported");
        }

        public void mapSku(string sku, string storeName, string storeSku)
        {
            if (storeName == STORE)
            {
                _sku2storeSkuMappings[sku] = storeSku;
                _storeSku2skuMappings[storeSku] = sku;
            }
        }

        public void unbindService()
        {
        }

        public bool areSubscriptionsSupported()
        {
            return true;
        }

        public void queryInventory()
        {
            OpenIAB.EventManager.SendMessage("OnQueryInventorySucceeded", GetInventory());
        }

        public void queryInventory(string[] skus)
        {
            string[] storeSkus = new string[skus.Length];
            for (int i = 0; i < skus.Length; ++i)
                storeSkus[i] = GetStoreSku(skus[i]);
            Store.LoadListings(storeSkus);
        }

        public void purchaseProduct(string sku, string developerPayload = "")
        {
            string storeSku = GetStoreSku(sku);
            Store.PurchaseProduct(storeSku, developerPayload);
        }

        public void purchaseSubscription(string sku, string developerPayload = "")
        {
            purchaseProduct(sku, developerPayload);
        }

        public void consumeProduct(Purchase purchase)
        {
            string storeSku = GetStoreSku(purchase.Sku);
            Store.ConsumeProduct(storeSku);
        }

        /// <summary>
        /// Not needed on WP8
        /// </summary>
        public void restoreTransactions()
        {
        }

        public bool isDebugLog()
        {
            // TODO: implement in DLL
            return false;
        }

        public void enableDebugLogging(bool enabled)
        {
            // TODO: implement in DLL
        }

        public void enableDebugLogging(bool enabled, string tag)
        {
            // TODO: implement in DLL
        }
#endif
    }
}