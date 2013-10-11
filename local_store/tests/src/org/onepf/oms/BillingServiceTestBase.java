package org.onepf.oms;

import android.content.Intent;
import android.os.Bundle;
import android.test.ServiceTestCase;
import android.test.mock.MockApplication;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class BillingServiceTestBase extends ServiceTestCase<BillingService> {

    protected BillingBinder _binder;
    protected MockApplication _app;

    public BillingServiceTestBase(Class<BillingService> serviceClass) {
        super(serviceClass);
    }

    protected abstract String getConfig();
    protected abstract MockApplication getApp(String config) throws Exception;

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        _binder = null;
        _app = null;
    }

    void start(String config) throws Exception {
        _app = getApp(config);
        _app.onCreate();
        setApplication(_app);
        _binder = (BillingBinder) bindService(new Intent());
    }

    Bundle createSkuBundle(List<String> skuList) {
        Bundle skuBundle = new Bundle();
        skuBundle.putStringArrayList(BillingBinder.ITEM_ID_LIST, new ArrayList<String>(skuList));
        return skuBundle;
    }

    @MediumTest
    public void testIsBillingSupported() throws Exception {
        start(getConfig());
        assertEquals(_binder.isBillingSupported(3, "org.onepf.trivialdrive", BillingBinder.ITEM_TYPE_INAPP), BillingBinder.RESULT_OK);
        assertEquals(_binder.isBillingSupported(3, "org.onepf.trivialdrive", BillingBinder.ITEM_TYPE_SUBS), BillingBinder.RESULT_OK);
        assertEquals(_binder.isBillingSupported(100500, "org.onepf.trivialdrive", BillingBinder.ITEM_TYPE_INAPP), BillingBinder.RESULT_OK);
        assertEquals(_binder.isBillingSupported(100500, "org.onepf.trivialdrive", BillingBinder.ITEM_TYPE_SUBS), BillingBinder.RESULT_OK);
        assertEquals(_binder.isBillingSupported(0, "org.onepf.trivialdrive", BillingBinder.ITEM_TYPE_INAPP), BillingBinder.RESULT_BILLING_UNAVAILABLE);
        assertEquals(_binder.isBillingSupported(0, "org.onepf.trivialdrive", BillingBinder.ITEM_TYPE_SUBS), BillingBinder.RESULT_BILLING_UNAVAILABLE);
        //assertEquals(_binder.isBillingSupported(3, "wrong.app.package", BillingBinder.ITEM_TYPE_INAPP), BillingBinder.RESULT_BILLING_UNAVAILABLE);
        //assertEquals(_binder.isBillingSupported(3, "wrong.app.package", BillingBinder.ITEM_TYPE_INAPP), BillingBinder.RESULT_BILLING_UNAVAILABLE);
        assertEquals(_binder.isBillingSupported(0, "org.onepf.trivialdrive", "UNKNOWN_TYPE"), BillingBinder.RESULT_BILLING_UNAVAILABLE);
    }

    @MediumTest
    public void testGetSkuDetails_wrong() throws Exception {
        start(getConfig());

        //Bundle result = _binder.getSkuDetails(3, "org.wrong.package", BillingBinder.ITEM_TYPE_INAPP, createSkuBundle(Arrays.asList("sku_gas")));
        //assertEquals(result.getInt(BillingBinder.RESPONSE_CODE), BillingBinder.RESULT_ITEM_UNAVAILABLE);

        Bundle result = _binder.getSkuDetails(1, "org.onepf.trivialdrive", BillingBinder.ITEM_TYPE_INAPP, createSkuBundle(Arrays.asList("sku_gas")));
        assertEquals(result.getInt(BillingBinder.RESPONSE_CODE), BillingBinder.RESULT_DEVELOPER_ERROR);

        result = _binder.getSkuDetails(3, "org.onepf.trivialdrive", BillingBinder.ITEM_TYPE_INAPP, createSkuBundle(Collections.nCopies(21, "sku")));
        assertEquals(result.getInt(BillingBinder.RESPONSE_CODE), BillingBinder.RESULT_DEVELOPER_ERROR);
    }

    @MediumTest
    public void testGetSkuDetails_proper() throws Exception {
        start(getConfig());
        Bundle result = _binder.getSkuDetails(3, "org.onepf.trivialdrive", BillingBinder.ITEM_TYPE_INAPP, createSkuBundle(Arrays.asList("sku_gas")));
        assertEquals(result.getInt(BillingBinder.RESPONSE_CODE), BillingBinder.RESULT_OK);
        ArrayList<String> detailsList = result.getStringArrayList(BillingBinder.DETAILS_LIST);
        assertEquals(detailsList.size(), 1);
        JSONObject o = new JSONObject(detailsList.get(0));
        assertEquals(o.getString("productId"), "sku_gas");
        assertEquals(o.getString("type"), "inapp");
        assertEquals(o.getString("title"), "Some Gas");
        assertEquals(o.getString("description"), "Fulfil tank for 1/4");
        assertEquals(o.getInt("price"), 50000000);

        result = _binder.getSkuDetails(3, "org.onepf.trivialdrive", BillingBinder.ITEM_TYPE_SUBS, createSkuBundle(Arrays.asList("sku_gas")));
        assertEquals(result.getInt(BillingBinder.RESPONSE_CODE), BillingBinder.RESULT_OK);

        result = _binder.getSkuDetails(3, "org.onepf.trivialdrive", null, createSkuBundle(Arrays.asList("sku_gas")));
        assertEquals(result.getInt(BillingBinder.RESPONSE_CODE), BillingBinder.RESULT_OK);

        result = _binder.getSkuDetails(3, "org.onepf.trivialdrive", null, createSkuBundle(Arrays.asList("sku_premium")));
        assertEquals(result.getInt(BillingBinder.RESPONSE_CODE), BillingBinder.RESULT_OK);

        result = _binder.getSkuDetails(3, "org.onepf.trivialdrive", null, createSkuBundle(Arrays.asList("sku_infinite_gas")));
        assertEquals(result.getInt(BillingBinder.RESPONSE_CODE), BillingBinder.RESULT_OK);
    }

    @MediumTest
    public void testGetBuyIntent_wrong() throws Exception {
        start(getConfig());

        Bundle result = _binder.getBuyIntent(1, "org.onepf.trivialdrive", "sku_gas", BillingBinder.ITEM_TYPE_INAPP, "payload");
        assertEquals(result.getInt(BillingBinder.RESPONSE_CODE), BillingBinder.RESULT_DEVELOPER_ERROR);
        assertTrue(result.getParcelable(BillingBinder.BUY_INTENT) != null);

//        result = _binder.getBuyIntent(3, "org.wrong.package", "sku_gas", BillingBinder.ITEM_TYPE_INAPP, "payload");
//        assertEquals(result.getInt(BillingBinder.RESPONSE_CODE), BillingBinder.RESULT_ITEM_UNAVAILABLE);
//        assertTrue(result.getParcelable(BillingBinder.BUY_INTENT) != null);

        result = _binder.getBuyIntent(3, "org.onepf.trivialdrive", "sku_unknown", BillingBinder.ITEM_TYPE_INAPP, "payload");
        assertEquals(result.getInt(BillingBinder.RESPONSE_CODE), BillingBinder.RESULT_ITEM_UNAVAILABLE);
        assertTrue(result.getParcelable(BillingBinder.BUY_INTENT) != null);

//        result = _binder.getBuyIntent(3, "org.onepf.trivialdrive", "sku_gas", BillingBinder.ITEM_TYPE_SUBS, "payload");
//        assertEquals(result.getInt(BillingBinder.RESPONSE_CODE), BillingBinder.RESULT_DEVELOPER_ERROR);
//        assertTrue(result.getParcelable(BillingBinder.BUY_INTENT) != null);
    }


    @SmallTest
    public void testGetBuyIntent_proper() throws Exception {
        start(getConfig());

        Bundle result = _binder.getBuyIntent(3, "org.onepf.trivialdrive", "sku_gas", BillingBinder.ITEM_TYPE_INAPP, "payload");
        assertEquals(result.getInt(BillingBinder.RESPONSE_CODE), BillingBinder.RESULT_OK);

        result = _binder.getBuyIntent(3, "org.onepf.trivialdrive", "sku_infinite_gas", BillingBinder.ITEM_TYPE_SUBS, "payload");
        assertEquals(result.getInt(BillingBinder.RESPONSE_CODE), BillingBinder.RESULT_OK);
    }

    @SmallTest
    public void testGetPurchases_wrong() throws Exception {
        start(getConfig());

        Bundle result = _binder.getPurchases(2, "org.onepf.trivialdrive", BillingBinder.ITEM_TYPE_INAPP, "");
        assertEquals(result.getInt(BillingBinder.RESPONSE_CODE), BillingBinder.RESULT_DEVELOPER_ERROR);

        result = _binder.getPurchases(3, "org.onepf.trivialdrive", "wrong_type", "");
        assertEquals(result.getInt(BillingBinder.RESPONSE_CODE), BillingBinder.RESULT_DEVELOPER_ERROR);
    }

//    @MediumTest
//    public void testGetPurchases() throws Exception {
//        start(getConfig());
//
//        // No token
//        Bundle result = _binder.getPurchases(3, "org.onepf.trivialdrive", BillingBinder.ITEM_TYPE_INAPP, null);
//        assertEquals(result.getInt(BillingBinder.RESPONSE_CODE), BillingBinder.RESULT_OK);
//
//        // INAPP
//        result = _binder.getPurchases(3, "org.onepf.trivialdrive", BillingBinder.ITEM_TYPE_INAPP, "");
//        assertEquals(result.getInt(BillingBinder.RESPONSE_CODE), BillingBinder.RESULT_OK);
//
//        ArrayList<String> inappPurchaseItemList = result.getStringArrayList(BillingBinder.INAPP_PURCHASE_ITEM_LIST);
//        ArrayList<String> inappPurchaseDataList = result.getStringArrayList(BillingBinder.INAPP_PURCHASE_DATA_LIST);
//        ArrayList<String> inappDataSignatureList = result.getStringArrayList(BillingBinder.INAPP_DATA_SIGNATURE_LIST);
//
//        assertTrue(inappPurchaseItemList.size() == inappPurchaseDataList.size() && inappPurchaseItemList.size() == inappDataSignatureList.size() && inappPurchaseItemList.size() == 1);
//        assertFalse(result.containsKey(BillingBinder.INAPP_CONTINUATION_TOKEN));
//        assertEquals(inappPurchaseItemList.get(0), "sku_premium");
//
//        JSONObject o = new JSONObject(inappPurchaseDataList.get(0));
//        assertEquals(o.getString("productId"), "sku_premium");
//
//
//        // SUBS
//        result = _binder.getPurchases(3, "org.onepf.trivialdrive", BillingBinder.ITEM_TYPE_SUBS, "");
//        assertEquals(result.getInt(BillingBinder.RESPONSE_CODE), BillingBinder.RESULT_OK);
//
//        inappPurchaseItemList = result.getStringArrayList(BillingBinder.INAPP_PURCHASE_ITEM_LIST);
//        inappPurchaseDataList = result.getStringArrayList(BillingBinder.INAPP_PURCHASE_DATA_LIST);
//        inappDataSignatureList = result.getStringArrayList(BillingBinder.INAPP_DATA_SIGNATURE_LIST);
//
//        assertTrue(inappPurchaseItemList.size() == inappPurchaseDataList.size() && inappPurchaseItemList.size() == inappDataSignatureList.size() && inappPurchaseItemList.size() == 1);
//        assertFalse(result.containsKey(BillingBinder.INAPP_CONTINUATION_TOKEN));
//        assertEquals(inappPurchaseItemList.get(0), "sku_infinite_gas");
//
//        o = new JSONObject(inappPurchaseDataList.get(0));
//        assertEquals(o.getString("productId"), "sku_infinite_gas");
//    }
}
