/**
 * Represents an in-app billing purchase.
 * 
 */
namespace OnePF
{
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

        public Purchase(string jsonString) {
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

        // For debug purposes and editor mode
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