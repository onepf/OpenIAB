package org.onepf.oms.appstore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onepf.oms.appstore.nokiaUtils.NokiaSkuFormatException;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Created by krozov on 02.09.14.
 */
@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class NokiaStoreTest {

    @Test
    public void testCorrectSku() throws Exception {
        NokiaStore.checkSku("12351");
    }

    @Test(expected = NokiaSkuFormatException.class)
    public void testWrongFormatSku() throws Exception {
        NokiaStore.checkSku("test_sku");
    }
}
