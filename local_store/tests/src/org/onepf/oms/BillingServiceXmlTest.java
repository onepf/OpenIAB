package org.onepf.oms;

import android.test.mock.MockApplication;

public class BillingServiceXmlTest extends BillingServiceTestBase {

    public BillingServiceXmlTest() {
        super(BillingService.class);
    }

    @Override
    protected MockApplication getApp(String config) throws Exception {
        return new MockBillingXmlApplication(config);
    }


    @Override
    protected String getConfig() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><store><product productId=\"sku_gas\" type=\"inapp\" title=\"Some Gas\" description=\"Fulfil tank for 1/4\" price=\"50000000\" /><product productId=\"sku_premium\" type=\"inapp\" /><product productId=\"sku_infinite_gas\" type=\"subs\" /></store>";
    }
}
