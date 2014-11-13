package org.onepf.oms;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onepf.oms.appstore.SamsungSkuFormatException;
import org.onepf.oms.appstore.nokiaUtils.NokiaSkuFormatException;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

/**
 * Created by krozov on 01.09.14.
 */
@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class SkuManagerTest {

    private static final String STORE_SKU_GOOGLE = "test_sku_google";
    private static final String STORE_SKU_SAMSUNG = "2014/test_sku_samsung";
    private static final String STORE_SKU_NOKIA = "12451";
    private static final String ITEM_SKU = "test_sku";

    @Test
    public void testMapSku() throws Exception {
        SkuManager sm = SkuManager.getInstance();
        sm.mapSku(ITEM_SKU, OpenIabHelper.NAME_GOOGLE, STORE_SKU_GOOGLE);

        String storeSku = sm.getStoreSku(OpenIabHelper.NAME_GOOGLE, ITEM_SKU);
        Assert.assertNotNull(storeSku);
        Assert.assertEquals(STORE_SKU_GOOGLE, storeSku);

        String sku = sm.getSku(OpenIabHelper.NAME_GOOGLE, STORE_SKU_GOOGLE);
        Assert.assertNotNull(sku);
        Assert.assertEquals(ITEM_SKU, sku);

        List<String> googlePlaySkus = sm.getAllStoreSkus(OpenIabHelper.NAME_GOOGLE);
        Assert.assertNotNull(googlePlaySkus);
        Assert.assertEquals(1, googlePlaySkus.size());
        Assert.assertNotNull(googlePlaySkus.get(0));
        Assert.assertEquals(STORE_SKU_GOOGLE, googlePlaySkus.get(0));

    }

    @Test
    public void testMapNokiaStoreSku() {
        SkuManager sm = SkuManager.getInstance();
        sm.mapSku(ITEM_SKU, OpenIabHelper.NAME_NOKIA, STORE_SKU_NOKIA);

        String storeSku = sm.getStoreSku(OpenIabHelper.NAME_NOKIA, ITEM_SKU);
        Assert.assertNotNull(storeSku);
        Assert.assertEquals(STORE_SKU_NOKIA, storeSku);

        String sku = sm.getSku(OpenIabHelper.NAME_NOKIA, STORE_SKU_NOKIA);
        Assert.assertNotNull(sku);
        Assert.assertEquals(ITEM_SKU, sku);

        List<String> samsungAppsSkus = sm.getAllStoreSkus(OpenIabHelper.NAME_NOKIA);
        Assert.assertNotNull(samsungAppsSkus);
        Assert.assertEquals(1, samsungAppsSkus.size());
        Assert.assertNotNull(samsungAppsSkus.get(0));
        Assert.assertEquals(STORE_SKU_NOKIA, samsungAppsSkus.get(0));
    }

    @Test
    public void testMapSamsungSku() {
        SkuManager sm = SkuManager.getInstance();
        sm.mapSku(ITEM_SKU, OpenIabHelper.NAME_SAMSUNG, STORE_SKU_SAMSUNG);

        String storeSku = sm.getStoreSku(OpenIabHelper.NAME_SAMSUNG, ITEM_SKU);
        Assert.assertNotNull(storeSku);
        Assert.assertEquals(STORE_SKU_SAMSUNG, storeSku);

        String sku = sm.getSku(OpenIabHelper.NAME_SAMSUNG, STORE_SKU_SAMSUNG);
        Assert.assertNotNull(sku);
        Assert.assertEquals(ITEM_SKU, sku);

        List<String> samsungAppsSkus = sm.getAllStoreSkus(OpenIabHelper.NAME_SAMSUNG);
        Assert.assertNotNull(samsungAppsSkus);
        Assert.assertEquals(1, samsungAppsSkus.size());
        Assert.assertNotNull(samsungAppsSkus.get(0));
        Assert.assertEquals(STORE_SKU_SAMSUNG, samsungAppsSkus.get(0));
    }

    @Test(expected = SamsungSkuFormatException.class)
    public void testIllegalFormatSamsungSKUMapping() {
        SkuManager sm = SkuManager.getInstance();
        sm.mapSku("wrong_sku", OpenIabHelper.NAME_SAMSUNG, "test_group/test_item_id");
    }

    @Test(expected = NokiaSkuFormatException.class)
    public void testIllegalFormatNokiaSKUMapping() {
        SkuManager sm = SkuManager.getInstance();
        sm.mapSku("wrong_sku", OpenIabHelper.NAME_NOKIA, "test_nokia_store_sku");
    }

    @Test(expected = SkuMappingException.class)
    public void testMapNullSkuMapping() {
        final SkuManager sm = SkuManager.getInstance();
        sm.mapSku(null, OpenIabHelper.NAME_GOOGLE, STORE_SKU_GOOGLE);
    }

    @Test(expected = SkuMappingException.class)
    public void testMapNullStoreNameMapping() {
        final SkuManager sm = SkuManager.getInstance();
        sm.mapSku(ITEM_SKU, null, STORE_SKU_GOOGLE);
    }

    @Test(expected = SkuMappingException.class)
    public void testMapEmptyStoreSkuMapping() {
        final SkuManager sm = SkuManager.getInstance();
        sm.mapSku(ITEM_SKU, OpenIabHelper.NAME_GOOGLE, "");
    }
}
