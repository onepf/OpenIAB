using UnityEngine;

/**
 * Represents an in-app product's listing details.
 */
namespace OnePF {
    public class SkuDetails {
        public string ItemType { get; private set; }
        public string Sku { get; private set; }
        public string Type { get; private set; }
        public string Price { get; private set; }
        public string Title { get; private set; }
        public string Description { get; private set; }
        public string Json { get; private set; }

        public SkuDetails(string jsonString) {
			var json = new JSON(jsonString);
			ItemType = json.ToString("itemType");
			Sku = json.ToString("sku");
			Type = json.ToString("type");
			Price = json.ToString("price");
			Title = json.ToString("title");
			Description = json.ToString("description");
			Json = json.ToString("json");

        }

#if UNITY_IOS
        public SkuDetails(JSON json) {
            ItemType = json.ToString("itemType");
            Sku = json.ToString("sku");
            Type = json.ToString("type");
            Price = json.ToString("price");
            Title = json.ToString("title");
            Description = json.ToString("description");
            Json = json.ToString("json");

            Sku = OpenIAB_iOS.StoreSku2Sku(Sku);
        }
#endif

#if UNITY_WP8
        public SkuDetails(OnePF.WP8.ProductListing listing)
        {
            Sku = OpenIAB_WP8.GetSku(listing.ProductId);
            Title = listing.Name;
            Description = listing.Description;
            Price = listing.FormattedPrice;
        }
#endif

        public override string ToString() {
            return string.Format("SkuDetails: type = {0}, SKU = {1}, title = {2}, price = {3}, description = {4}", ItemType, Sku, Title, Price, Description);
        }
    }
}