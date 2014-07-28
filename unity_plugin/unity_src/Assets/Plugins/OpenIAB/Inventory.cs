/*******************************************************************************
 * Copyright 2012-2014 One Platform Foundation
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 ******************************************************************************/

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using UnityEngine;

namespace OnePF
{
    /**
     * Container for finished purchases and store listings
     */ 
    public class Inventory
    {
        private Dictionary<String, SkuDetails> _skuMap = new Dictionary<String, SkuDetails>();
        private Dictionary<String, Purchase> _purchaseMap = new Dictionary<String, Purchase>();

        public Inventory(string json)
        {
            var j = new JSON(json);
            foreach (var entry in (List<object>) j.fields["purchaseMap"])
            {
                List<object> pair = (List<object>) entry;
#if UNITY_IOS
				string key = OpenIAB_iOS.StoreSku2Sku(pair[0].ToString());
				// TODO: use same cotr on all platforms. Test why it works on Android json
                Purchase value = new Purchase((JSON) pair[1]);
#else
                string key = pair[0].ToString();
                Purchase value = new Purchase(pair[1].ToString());
#endif
                _purchaseMap.Add(key, value);
            }
            foreach (var entry in (List<object>) j.fields["skuMap"])
            {
                List<object> pair = (List<object>) entry;
#if UNITY_IOS
				string key = OpenIAB_iOS.StoreSku2Sku(pair[0].ToString());
                SkuDetails value = new SkuDetails((JSON) pair[1]);
#else
                string key = pair[0].ToString();
                SkuDetails value = new SkuDetails(pair[1].ToString());
#endif
                _skuMap.Add(key, value);
            }
        }

#if UNITY_WP8
        public Inventory()
        {
        }
#endif

        public override string ToString()
        {
            StringBuilder str = new StringBuilder();
            str.Append("{purchaseMap:{");
            foreach (var pair in _purchaseMap)
            {
                str.Append("\"" + pair.Key + "\":{" + pair.Value.ToString() + "},");
            }
            str.Append("},");
            str.Append("skuMap:{");
            foreach (var pair in _skuMap)
            {
                str.Append("\"" + pair.Key + "\":{" + pair.Value.ToString() + "},");
            }
            str.Append("}}");
            return str.ToString();
        }

        /**
         * Returns the listing details for an in-app product.
         */
        public SkuDetails GetSkuDetails(string sku)
        {
            if (!_skuMap.ContainsKey(sku))
            {
                return null;
            }
            return _skuMap[sku];
        }

        /**
         * Returns purchase information for a given product, or null if there is no purchase.
         */
        public Purchase GetPurchase(string sku)
        {
            if (!_purchaseMap.ContainsKey(sku))
            {
                return null;
            }
            return _purchaseMap[sku];
        }

        /**
         * Returns whether or not there exists a purchase of the given product.
         */
        public bool HasPurchase(string sku)
        {
            return _purchaseMap.ContainsKey(sku);
        }

        /**
         * Return whether or not details about the given product are available.
         */
        public bool HasDetails(string sku)
        {
            return _skuMap.ContainsKey(sku);
        }

        /**
         * Erase a purchase (locally) from the inventory, given its product ID. This just
         * modifies the Inventory object locally and has no effect on the server! This is
         * useful when you have an existing Inventory object which you know to be up to date,
         * and you have just consumed an item successfully, which means that erasing its
         * purchase data from the Inventory you already have is quicker than querying for
         * a new Inventory.
         */
        public void ErasePurchase(string sku)
        {
            if (_purchaseMap.ContainsKey(sku)) _purchaseMap.Remove(sku);
        }

        /**
         * Returns a list of all owned product IDs.
         */
        public List<string> GetAllOwnedSkus()
        {
            return _purchaseMap.Keys.ToList<string>();
        }

        /**
         * Returns a list of all owned product IDs of a given type
         */
        public List<string> GetAllOwnedSkus(string itemType)
        {
            List<string> result = new List<string>();
            foreach (Purchase p in _purchaseMap.Values)
            {
                if (p.ItemType == itemType) result.Add(p.Sku);
            }
            return result;
        }

        /**
         * Returns a list of all purchases.
         */
        public List<Purchase> GetAllPurchases()
        {
            return _purchaseMap.Values.ToList<Purchase>();
        }

        /** 
         * Returns a list of all available {@code SkuDetails} products. 
         */
        public List<SkuDetails> GetAllAvailableSkus()
        {
            return _skuMap.Values.ToList();
        }

        public void AddSkuDetails(SkuDetails d)
        {
            _skuMap.Add(d.Sku, d);
        }

        public void AddPurchase(Purchase p)
        {
            _purchaseMap.Add(p.Sku, p);
        }
    }
}
