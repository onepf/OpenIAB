package org.onepf.life2.oms.appstore;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import com.samsungapps.plasma.*;
import org.onepf.life2.oms.AppstoreInAppBillingService;
import org.onepf.life2.oms.OpenIabHelper;
import org.onepf.life2.oms.appstore.googleUtils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Boris Minaev
 * Date: 22.04.13
 * Time: 12:29
 */

public class SamsungAppsBillingService implements AppstoreInAppBillingService, PlasmaListener {
    private Plasma mPlasma;

    private int transactionId;
    private Map<Integer, PurchaseInfo> purchases;

    private class PurchaseInfo {
        IabHelper.OnIabPurchaseFinishedListener mListener;
        Activity mActivity;
        String mSku;
        String mItemType;
        int mRequestCode;
        String mExtraData;


        public PurchaseInfo(Activity act, String sku, String itemType, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
            mActivity = act;
            mSku = sku;
            mItemType = itemType;
            mRequestCode = requestCode;
            mListener = listener;
            mExtraData = extraData;
        }
    }

    public SamsungAppsBillingService(Context context, String itemGroupId) {
        mPlasma = new Plasma(itemGroupId, (Activity) context);
        mPlasma.setDeveloperFlag(1);
        mPlasma.setPlasmaListener(this);
        transactionId = 0;
        purchases = new HashMap<>();
    }

    @Override
    public void startSetup(IabHelper.OnIabSetupFinishedListener listener) {

    }

    @Override
    public void launchPurchaseFlow(Activity act, String sku, String itemType, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener, String extraData, String initialSku) {
        PurchaseInfo purchaseInfo = new PurchaseInfo(act, initialSku, itemType, requestCode, listener, extraData);
        purchases.put(transactionId, purchaseInfo);
        mPlasma.requestPurchaseItem(transactionId++, sku);
    }

    @Override
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return false;
    }

    @Override
    public Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus, List<String> moreSubsSkus) throws IabException {
        return null;
    }

    @Override
    public void consume(Purchase itemInfo) throws IabException {

    }

    @Override
    public void onItemInformationListReceived(int i, int i2, ArrayList<ItemInformation> itemInformations) {

    }

    @Override
    public void onPurchasedItemInformationListReceived(int i, int i2, ArrayList<PurchasedItemInformation> purchasedItemInformations) {

    }

    @Override
    public void onPurchaseItemInitialized(int transactionId, int statusCode, PurchaseTicket purchaseTicket) {
        if (statusCode != Plasma.STATUS_CODE_SUCCESS) {
            PurchaseInfo purchaseInfo = purchases.get(transactionId);
            if (purchaseInfo != null) {
                purchases.remove(purchaseInfo);
                // TODO: errors
                IabResult iabResult = new IabResult(OpenIabHelper.BILLING_RESPONSE_RESULT_ERROR, "Some error");
                Purchase purchase = null;
                purchaseInfo.mListener.onIabPurchaseFinished(iabResult, purchase);
            }
        }
    }

    @Override
    public void onPurchaseItemFinished(int transactionId, int statusCode, PurchasedItemInformation purchasedItemInformation) {
        PurchaseInfo purchaseInfo = purchases.get(transactionId);
        if (purchaseInfo != null) {
            purchases.remove(purchaseInfo);

            Purchase purchase = new Purchase();
            purchase.setItemType(purchaseInfo.mItemType);
            purchase.setSku(purchaseInfo.mSku);

            IabResult iabResult;
            if (statusCode == Plasma.STATUS_CODE_SUCCESS) {
                iabResult = new IabResult(OpenIabHelper.BILLING_RESPONSE_RESULT_OK, "OK");
            } else {
                iabResult = new IabResult(OpenIabHelper.BILLING_RESPONSE_RESULT_ERROR, "Some error");
            }
            purchaseInfo.mListener.onIabPurchaseFinished(iabResult, purchase);
        }
    }
}
