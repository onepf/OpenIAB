package org.onepf.store;

import java.util.ArrayList;
import java.util.Collections;

import org.onepf.oms.IOpenInAppBillingService;
import org.onepf.store.data.Database;
import org.onepf.store.data.Purchase;
import org.onepf.store.data.SkuDetails;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;

public class BillingBinder extends IOpenInAppBillingService.Stub {

    public static final String PURCHASE_COMPLETE_INTENT = "org.onepf.oms.PURCHASE_COMPLETE";

    // Response result codes
    public static final int RESULT_OK = 0;
    public static final int RESULT_USER_CANCELED = 1;
    public static final int RESULT_BILLING_UNAVAILABLE = 3;
    public static final int RESULT_ITEM_UNAVAILABLE = 4;
    public static final int RESULT_DEVELOPER_ERROR = 5;
    public static final int RESULT_ERROR = 6;
    public static final int RESULT_ITEM_ALREADY_OWNED = 7;
    public static final int RESULT_ITEM_NOT_OWNED = 8;

    // Keys for the responses
    public static final String RESPONSE_CODE = "RESPONSE_CODE";
    public static final String DETAILS_LIST = "DETAILS_LIST";
    public static final String BUY_INTENT = "BUY_INTENT";
    public static final String INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA";
    public static final String INAPP_DATA_SIGNATURE = "INAPP_DATA_SIGNATURE";
    public static final String INAPP_PURCHASE_ITEM_LIST = "INAPP_PURCHASE_ITEM_LIST";
    public static final String INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST";
    public static final String INAPP_DATA_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST";
    public static final String INAPP_CONTINUATION_TOKEN = "INAPP_CONTINUATION_TOKEN";

    // Param keys
    public static final String ITEM_ID_LIST = "ITEM_ID_LIST";
    public static final String ITEM_TYPE_LIST = "ITEM_TYPE_LIST";

    // Item types
    public static final String ITEM_TYPE_INAPP = "inapp";
    public static final String ITEM_TYPE_SUBS = "subs";

    // Purchase states
    public static final int PURCHASE_STATE_PURCHASED = 0;
    public static final int PURCHASE_STATE_CANCELED = 1;
    public static final int PURCHASE_STATE_REFUNDED = 2;

    final Database _db;
    final Context _context;

    public BillingBinder(Context context, Database database) {
        _db = database;
        _context = context;
    }

    /**
     * Checks support for the requested billing API version, package and in-app type.
     * Minimum API version supported by this interface is 3.
     * @param apiVersion the billing version which the app is using
     * @param packageName the package name of the calling app
     * @param type type of the in-app item being purchased "inapp" for one-time purchases
     *        and "subs" for subscription.
     * @return RESULT_OK(0) on success, corresponding result code on failures
     */
    @Override
    public int isBillingSupported(int apiVersion, String packageName, String type) throws RemoteException {
        if (apiVersion >= 3 &&
                (type.equals(BillingBinder.ITEM_TYPE_INAPP) || type.equals(BillingBinder.ITEM_TYPE_SUBS))) {
            return RESULT_OK;
        } else {
            return RESULT_BILLING_UNAVAILABLE;
        }
    }

    /**
     * Provides details of a list of SKUs
     * Given a list of SKUs of a valid type in the skusBundle, this returns a bundle
     * with a list JSON strings containing the productId, price, title and description.
     * This API can be called with a maximum of 20 SKUs.
     *
     * @param apiVersion  billing API version that the Third-party is using
     * @param packageName the package name of the calling app
     * @param skusBundle  bundle containing a StringArrayList of SKUs with key "ITEM_ID_LIST"
     * @return Bundle containing the following key-value pairs
     *         "RESPONSE_CODE" with int value, RESULT_OK(0) if success, other response codes on
     *         failure as listed above.
     *         "DETAILS_LIST" with a StringArrayList containing purchase information
     *         in JSON format similar to:
     *         '{ "productId" : "exampleSku", "type" : "inapp", "price" : "$5.00",
     *         "title : "Example Title", "description" : "This is an example description" }'
     */
    @Override
    public Bundle getSkuDetails(int apiVersion, String packageName, String type, Bundle skusBundle) throws RemoteException {
        Bundle result = new Bundle();

        if (!skusBundle.containsKey(ITEM_ID_LIST) || apiVersion < 3) {
            result.putInt(RESPONSE_CODE, RESULT_DEVELOPER_ERROR);
            return result;
        }

        ArrayList<String> itemIdList = skusBundle.getStringArrayList(ITEM_ID_LIST);

        if (itemIdList == null || itemIdList.size() <= 0 || itemIdList.size() >= 20) {
            result.putInt(RESPONSE_CODE, RESULT_DEVELOPER_ERROR);
            return result;
        }

        ArrayList<String> detailsList = new ArrayList<String>();
        for (String itemId : itemIdList) {
            SkuDetails skuDetails = _db.getSkuDetails(itemId);
            if (skuDetails != null) {
                detailsList.add(skuDetails.toJson());
            }
        }

        if (detailsList.size() <= 0) {
            result.putInt(RESPONSE_CODE, RESULT_ITEM_UNAVAILABLE);
        } else {
            result.putInt(RESPONSE_CODE, RESULT_OK);
            result.putStringArrayList(DETAILS_LIST, detailsList);
        }

        return result;
    }

    /**
     * Returns a pending intent to launch the purchase flow for an in-app item by providing a SKU,
     * the type, a unique purchase token and an optional developer payload.
     * @param apiVersion billing API version that the app is using
     * @param packageName package name of the calling app
     * @param sku the SKU of the in-app item as published in the developer console
     * @param type the type of the in-app item ("inapp" for one-time purchases
     *        and "subs" for subscription).
     * @param developerPayload optional argument to be sent back with the purchase information
     * @return Bundle containing the following key-value pairs
     *         "RESPONSE_CODE" with int value, RESULT_OK(0) if success, other response codes on
     *              failure as listed above.
     *         "BUY_INTENT" - PendingIntent to start the purchase flow
     *
     * The Pending intent should be launched with startIntentSenderForResult. When purchase flow
     * has completed, the onActivityResult() will give a resultCode of OK or CANCELED.
     * If the purchase is successful, the result data will contain the following key-value pairs
     *         "RESPONSE_CODE" with int value, RESULT_OK(0) if success, other response codes on
     *              failure as listed above.
     *         "INAPP_PURCHASE_DATA" - String in JSON format similar to
     *              '{"orderId":"12999763169054705758.1371079406387615",
     *                "packageName":"com.example.app",
     *                "productId":"exampleSku",
     *                "purchaseTime":1345678900000,
     *                "purchaseToken" : "122333444455555",
     *                "developerPayload":"example developer payload" }'
     *         "INAPP_DATA_SIGNATURE" - String containing the signature of the purchase data that
     *                                  was signed with the private key of the developer
     *                                  TODO: change this to app-specific keys.
     */
    @Override
    public Bundle getBuyIntent(int apiVersion, String packageName, String sku, String type, String developerPayload) throws RemoteException {
        Bundle result = new Bundle();

        PendingIntent pendingIntent;
        Intent purchaseIntent = new Intent(_context, PurchaseActivity.class);

        if (apiVersion < 3 || !(type.equals(ITEM_TYPE_INAPP) || type.equals(ITEM_TYPE_SUBS))) {
            result.putInt(RESPONSE_CODE, RESULT_DEVELOPER_ERROR);
        } else {
            SkuDetails skuDetails = _db.getSkuDetails(sku);
            if (skuDetails == null) {
                result.putInt(RESPONSE_CODE, RESULT_ITEM_UNAVAILABLE);
            } else if (!skuDetails.getType().equals(type)) {
                result.putInt(RESPONSE_CODE, RESULT_DEVELOPER_ERROR);
            } else {
                purchaseIntent.putExtra("packageName", packageName);
                purchaseIntent.putExtra("sku", sku);
                purchaseIntent.putExtra("developerPayload", developerPayload);
                result.putInt(RESPONSE_CODE, RESULT_OK);
            }
        }

        pendingIntent = PendingIntent.getActivity(_context, 0, purchaseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        result.putParcelable(BUY_INTENT, pendingIntent);
        return result;
    }

    /**
     * Returns the current SKUs owned by the user of the type and package name specified along with
     * purchase information and a signature of the data to be validated.
     * This will return all SKUs that have been purchased in V3 and managed items purchased using
     * V1 and V2 that have not been consumed.
     * @param apiVersion billing API version that the app is using
     * @param packageName package name of the calling app
     * @param type the type of the in-app items being requested
     *        ("inapp" for one-time purchases and "subs" for subscription).
     * @param continuationToken to be set as null for the first call, if the number of owned
     *        skus are too many, a continuationToken is returned in the response bundle.
     *        This method can be called again with the continuation token to get the next set of
     *        owned skus.
     * @return Bundle containing the following key-value pairs
     *         "RESPONSE_CODE" with int value, RESULT_OK(0) if success, other response codes on
     *              failure as listed above.
     *         "INAPP_PURCHASE_ITEM_LIST" - StringArrayList containing the list of SKUs
     *         "INAPP_PURCHASE_DATA_LIST" - StringArrayList containing the purchase information
     *         "INAPP_DATA_SIGNATURE_LIST"- StringArrayList containing the signatures
     *                                      of the purchase information
     *         "INAPP_CONTINUATION_TOKEN" - String containing a continuation token for the
     *                                      next set of in-app purchases. Only set if the
     *                                      user has more owned skus than the current list.
     */
    @Override
    public Bundle getPurchases(int apiVersion, String packageName, String type, String continuationToken) throws RemoteException {
        Bundle result = new Bundle();

        if (apiVersion < 3 || !(type.equals(ITEM_TYPE_INAPP) || type.equals(ITEM_TYPE_SUBS))) {
            result.putInt(RESPONSE_CODE, RESULT_DEVELOPER_ERROR);
            return result;
        }

        result.putInt(RESPONSE_CODE, RESULT_OK);

        ArrayList<Purchase> purchaseHistory = _db.getInventory(packageName, type);
        int size = purchaseHistory.size();

        ArrayList<String> purchaseItemList = new ArrayList<String>(size);
        ArrayList<String> purchaseDataList = new ArrayList<String>(size);
        ArrayList<String> purchaseSignatureList = new ArrayList<String>(Collections.nCopies(size, ""));

        for (Purchase aPurchaseHistory : purchaseHistory) {
            purchaseItemList.add(aPurchaseHistory.getSku());
            purchaseDataList.add(aPurchaseHistory.toJson());
        }

        result.putStringArrayList(INAPP_PURCHASE_ITEM_LIST, purchaseItemList);
        result.putStringArrayList(INAPP_PURCHASE_DATA_LIST, purchaseDataList);
        result.putStringArrayList(INAPP_DATA_SIGNATURE_LIST, purchaseSignatureList);

        return result;
    }

    /**
     * Consume the last purchase of the given SKU. This will result in this item being removed
     * from all subsequent responses to getPurchases() and allow re-purchase of this item.
     * @param apiVersion billing API version that the app is using
     * @param packageName package name of the calling app
     * @param purchaseToken token in the purchase information JSON that identifies the purchase
     *        to be consumed
     * @return 0 if consumption succeeded. Appropriate error values for failures.
     */
    @Override
    public int consumePurchase(int apiVersion, String packageName, String purchaseToken) throws RemoteException {
        return apiVersion < 3 ? RESULT_DEVELOPER_ERROR : _db.consume(purchaseToken);
    }
}
