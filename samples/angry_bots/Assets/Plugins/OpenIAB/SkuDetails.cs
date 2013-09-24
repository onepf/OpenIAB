/**
 * Represents an in-app product's listing details.
 */
namespace OpenIabPlugin {
    public class SkuDetails {
        public string ItemType { get; private set; }
        public string Sku { get; private set; }
        public string Type { get; private set; }
        public string Price { get; private set; }
        public string Title { get; private set; }
        public string Description { get; private set; }
        public string Json { get; private set; }

        public SkuDetails(string json) {
            var j = new JSON(json);
            ItemType = j.ToString("itemType");
            Sku = j.ToString("sku");
            Type = j.ToString("type");
            Price = j.ToString("price");
            Title = j.ToString("title");
            Description = j.ToString("description");
            Json = j.ToString("json");
        }

        public override string ToString() {
            return string.Format("SkuDetails: type = {0}, SKU = {1}, title = {2}, price = {3}, description = {4}", ItemType, Sku, Title, Price, Description);
        }
    }
}