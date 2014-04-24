using System.Collections.Generic;
using System;

namespace OnePF.WP8
{
    public class Store
    {
        public static event Action<Dictionary<string, ProductListing>> LoadListingsSucceeded;
        public static event Action<string> LoadListingsFailed;
        public static event Action<string, string> PurchaseSucceeded;
        public static event Action<string> PurchaseFailed;
        public static event Action<string> ConsumeSucceeded;
        public static event Action<string> ConsumeFailed;

        public static IEnumerable<string> Inventory { get { return new List<string>(); } }

        public static void LoadListings(string[] productIds)
        {
            if (LoadListingsSucceeded != null)
                LoadListingsSucceeded(new Dictionary<string, ProductListing>());
        }

        public static void PurchaseProduct(string productId, string developerPayload) 
        { 
            if (PurchaseSucceeded != null)
                PurchaseSucceeded(productId, developerPayload);
        }

        public static void ConsumeProduct(string productId) 
        {
            if (ConsumeSucceeded != null)
                ConsumeSucceeded(productId);
        }
    }
}

