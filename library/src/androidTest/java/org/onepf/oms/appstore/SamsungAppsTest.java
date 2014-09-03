package org.onepf.oms.appstore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Created by krozov on 02.09.14.
 */
@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class SamsungAppsTest {

    @Test
    public void testCorrectSku() throws Exception {
        SamsungApps.checkSku("2014/test_sku_samsung");
    }

    @Test(expected = SamsungSkuFormatException.class)
    public void testEmptyGroupIdSku() throws Exception {
        SamsungApps.checkSku("/test_sku_samsung");
    }

    @Test(expected = SamsungSkuFormatException.class)
    public void testEmptyItemIdSku() throws Exception {
        SamsungApps.checkSku("2014/");
    }

    @Test(expected = SamsungSkuFormatException.class)
    public void testNotNumericGroupIdSku() throws Exception {
        SamsungApps.checkSku("test_group/test_item_id");
    }
}
