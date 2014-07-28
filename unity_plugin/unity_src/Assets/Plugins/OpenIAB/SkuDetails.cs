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

using UnityEngine;

namespace OnePF
{
    /**
     * Represents an in-app product's listing details.
     */
    public class SkuDetails
    {
        public string ItemType { get; private set; }
        public string Sku { get; private set; }
        public string Type { get; private set; }
        public string Price { get; private set; }
        public string Title { get; private set; }
        public string Description { get; private set; }
        public string Json { get; private set; }
        public string CurrencyCode { get; private set; }
        public string PriceValue { get; private set; }

        // Used for Android
        public SkuDetails(string jsonString)
        {
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

        private void ParseFromJson()
        {
            if (string.IsNullOrEmpty(Json)) return;
            var json = new JSON(Json);
            if (string.IsNullOrEmpty(PriceValue))
            {
                float val = json.ToFloat("price_amount_micros");
                val /= 1000000;
                PriceValue = val.ToString();
            }
            if (string.IsNullOrEmpty(CurrencyCode))
                CurrencyCode = json.ToString("price_currency_code");
        }

        /**
         * ToString
         * @return formatted string
         */ 
        public override string ToString()
        {
            return string.Format("[SkuDetails: type = {0}, SKU = {1}, title = {2}, price = {3}, description = {4}, priceValue={5}, currency={6}]",
                                 ItemType, Sku, Title, Price, Description, PriceValue, CurrencyCode);
        }
    }
}