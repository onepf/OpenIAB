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

namespace OnePF
{
    /**
     * Represents an in-app billing purchase.
     */
    public class Purchase
    {
        public string ItemType { get; private set; }  // ITEM_TYPE_INAPP or ITEM_TYPE_SUBS
        public string OrderId { get; private set; }
        public string PackageName { get; private set; }
        public string Sku { get; private set; }
        public long PurchaseTime { get; private set; }
        public int PurchaseState { get; private set; }
        public string DeveloperPayload { get; private set; }
        public string Token { get; private set; }
        public string OriginalJson { get; private set; }
        public string Signature { get; private set; }
        public string AppstoreName { get; private set; }

        private Purchase()
        {
        }

        /**
         * Create purchase from json string
         * @param jsonString data serialized to json
         */
        public Purchase(string jsonString)
        {
            var json = new JSON(jsonString);
            ItemType = json.ToString("itemType");
            OrderId = json.ToString("orderId");
            PackageName = json.ToString("packageName");
            Sku = json.ToString("sku");
            PurchaseTime = json.ToLong("purchaseTime");
            PurchaseState = json.ToInt("purchaseState");
            DeveloperPayload = json.ToString("developerPayload");
            Token = json.ToString("token");
            OriginalJson = json.ToString("originalJson");
            Signature = json.ToString("signature");
            AppstoreName = json.ToString("appstoreName");
        }

#if UNITY_IOS
        public Purchase(JSON json) {
            ItemType = json.ToString("itemType");
            OrderId = json.ToString("orderId");
            PackageName = json.ToString("packageName");
            Sku = json.ToString("sku");
            PurchaseTime = json.ToLong("purchaseTime");
            PurchaseState = json.ToInt("purchaseState");
            DeveloperPayload = json.ToString("developerPayload");
            Token = json.ToString("token");
            OriginalJson = json.ToString("originalJson");
            Signature = json.ToString("signature");
            AppstoreName = json.ToString("appstoreName");

			Sku = OpenIAB_iOS.StoreSku2Sku(Sku);
        }
#endif

        /**
         * For debug purposes and editor mode
         * @param sku product ID
         */ 
        public static Purchase CreateFromSku(string sku)
        {
            return CreateFromSku(sku, "");
        }

        public static Purchase CreateFromSku(string sku, string developerPayload)
        {
            var p = new Purchase();
            p.Sku = sku;
            p.DeveloperPayload = developerPayload;
#if UNITY_IOS
			AddIOSHack(p);
#endif
            return p;
        }

        /**
         * ToString
         * @return original json
         */ 
        public override string ToString()
        {
            return "SKU:" + Sku + ";" + OriginalJson;
        }

#if UNITY_IOS
		private static void AddIOSHack(Purchase p) {
			if(string.IsNullOrEmpty(p.AppstoreName)) {
				p.AppstoreName = "com.apple.appstore";
			}
			if(string.IsNullOrEmpty(p.ItemType)) {
				p.ItemType = "InApp";
			}
			if(string.IsNullOrEmpty(p.OrderId)) {
				p.OrderId = System.Guid.NewGuid().ToString();
			}
		}
#endif

        /**
         * Serilize to json
         * @return json string
         */ 
        public string Serialize()
        {
            var j = new JSON();
            j["itemType"] = ItemType;
            j["orderId"] = OrderId;
            j["packageName"] = PackageName;
            j["sku"] = Sku;
            j["purchaseTime"] = PurchaseTime;
            j["purchaseState"] = PurchaseState;
            j["developerPayload"] = DeveloperPayload;
            j["token"] = Token;
            j["originalJson"] = OriginalJson;
            j["signature"] = Signature;
            j["appstoreName"] = AppstoreName;
            return j.serialized;
        }
    }
}