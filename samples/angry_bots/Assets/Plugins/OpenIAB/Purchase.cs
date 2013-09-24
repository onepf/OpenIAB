/**
 * Represents an in-app billing purchase.
 * 
 */
namespace OpenIabPlugin {
    public class Purchase {
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

        private Purchase() {
        }

        public Purchase(string json) {
            var j = new JSON(json);
            ItemType = j.ToString("itemType");
            OrderId = j.ToString("orderId");
            PackageName = j.ToString("packageName");
            Sku = j.ToString("sku");
            PurchaseTime = j.ToLong("purchaseTime");
            PurchaseState = j.ToInt("purchaseState");
            DeveloperPayload = j.ToString("developerPayload");
            Token = j.ToString("token");
            OriginalJson = j.ToString("originalJson");
            Signature = j.ToString("signature");
            AppstoreName = j.ToString("appstoreName");
        }

        // For debug purposes and editor mode
        internal static Purchase CreateFromSku(string sku) {
            return CreateFromSku(sku, "");
        }
        internal static Purchase CreateFromSku(string sku, string developerPayload) {
            var p = new Purchase();
            p.Sku = sku;
            p.DeveloperPayload = developerPayload;
            return p;
        }

        public override string ToString() {
            return "PurchaseInfo(type:" + ItemType + "):" + OriginalJson;
        }

        public string Serialize() {
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