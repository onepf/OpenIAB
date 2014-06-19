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
		public string CurrencyCode { get; private set;}
		public string PriceValue { get; private set;}

		// Used for Android
        public SkuDetails(string jsonString) {
			var json = new JSON(jsonString);
			ItemType = json.ToString("itemType");
			Sku = json.ToString("sku");
			Type = json.ToString("type");
			Price = json.ToString("price");
			Title = json.ToString("title");
			Description = json.ToString("description");
			Json = json.ToString("json");
			CurrencyCode = json.ToString("currencyCode");
			PriceValue = json.ToString("priceValue");
			ParseFromJson();
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
			CurrencyCode = json.ToString("currencyCode");
			PriceValue = json.ToString("priceValue");

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

		private void ParseFromJson() {
			if (string.IsNullOrEmpty(Json)) return;
			var json = new JSON(Json);
			if (string.IsNullOrEmpty(PriceValue)) {
				float val = json.ToFloat("price_amount_micros");
				val /= 1000000;
				PriceValue = val.ToString();
			}
			if (string.IsNullOrEmpty(CurrencyCode))
				CurrencyCode = json.ToString("price_currency_code");
		}

        public override string ToString() {
			return string.Format("[SkuDetails: type = {0}, SKU = {1}, title = {2}, price = {3}, description = {4}, priceValue={5}, currency={6}]",
			                     ItemType, Sku, Title, Price, Description, PriceValue, CurrencyCode);        }
    }
}