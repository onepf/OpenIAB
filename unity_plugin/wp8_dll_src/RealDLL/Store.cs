using System;
using Windows.ApplicationModel.Store;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Windows.Foundation;
using System.Windows.Threading;
using System.Windows;

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

        public static IEnumerable<string> Inventory
        {
            get
            {
                List<string> productLicensesList = new List<string>();
                IReadOnlyDictionary<string, ProductLicense> productLicenses = CurrentApp.LicenseInformation.ProductLicenses;
                if (productLicenses != null)
                    foreach (var pl in productLicenses.Values)
                        if (pl.IsActive)
                            productLicensesList.Add(pl.ProductId);
                return productLicensesList;
            }
        }

        public static void LoadListings(string[] productIds)
        {
            Deployment.Current.Dispatcher.BeginInvoke(() =>
            {
                Dictionary<string, ProductListing> resultListings = new Dictionary<string, ProductListing>();
                try
                {
                    IAsyncOperation<ListingInformation> asyncOp = CurrentApp.LoadListingInformationByProductIdsAsync(productIds);
                    asyncOp.Completed = (op, status) =>
                    {
                        var listings = op.GetResults();
                        foreach (var l in listings.ProductListings)
                        {
                            var listing = l.Value;
                            var resultListing = new ProductListing(
                                listing.ProductId,
                                listing.Name,
                                listing.Description,
                                listing.FormattedPrice);
                            resultListings[l.Key] = resultListing;
                        }
                        if (LoadListingsSucceeded != null)
                            LoadListingsSucceeded(resultListings);
                    };
                }
                catch (Exception e)
                {
                    if (LoadListingsFailed != null)
                        LoadListingsFailed(e.Message);
                }
            });
        }

        public static void PurchaseProduct(string productId, string developerPayload)
        {
            Deployment.Current.Dispatcher.BeginInvoke(() =>
            {
                // Kick off purchase; don't ask for a receipt when it returns
                IAsyncOperation<string> asyncOp;
                try
                {
                    asyncOp = CurrentApp.RequestProductPurchaseAsync(productId, false);
                }
                catch (Exception e)
                {
                    string errorMessage;
                    // When the user does not complete the purchase (e.g. cancels or navigates back from the Purchase Page), an exception with an HRESULT of E_FAIL is expected.
                    switch ((HResult)e.HResult)
                    {
                        case HResult.E_FAIL:
                            errorMessage = "Purchase cancelled";
                            break;
                        default:
                            errorMessage = e.Message;
                            break;
                    }
                    if (PurchaseFailed != null)
                        PurchaseFailed(errorMessage);
                    return;
                }
                asyncOp.Completed = (op, status) =>
                {
                    string errorMessage;
                    ProductLicense productLicense = null;
                    if (CurrentApp.LicenseInformation.ProductLicenses.TryGetValue(productId, out productLicense))
                    {
                        if (productLicense.IsActive)
                        {
                            if (PurchaseSucceeded != null)
                                PurchaseSucceeded(productId, developerPayload);
                            return;
                        }
                        else
                        {
                            errorMessage = op.ErrorCode.Message;
                        }
                    }
                    else
                    {
                        errorMessage = op.ErrorCode.Message;
                    }

                    if (PurchaseFailed != null)
                        PurchaseFailed(errorMessage);
                };
            });
        }

        public static void ConsumeProduct(string productId)
        {
            Deployment.Current.Dispatcher.BeginInvoke(() =>
            {
                try
                {
                    CurrentApp.ReportProductFulfillment(productId);
                }
                catch (Exception e)
                {
                    if (ConsumeFailed != null)
                        ConsumeFailed(e.Message);
                    return;
                }
                if (ConsumeSucceeded != null)
                    ConsumeSucceeded(productId);
            });
        }
    }
}
