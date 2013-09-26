package org.onepf.oms;

import android.test.mock.MockApplication;

public class BillingServiceJsonTest extends BillingServiceTestBase {

    public BillingServiceJsonTest() {
        super(BillingService.class);
    }

    @Override
    protected MockApplication getApp(String config) throws Exception {
        return new MockBillingJsonApplication(config);
    }

    @Override
    protected String getConfig() {
        return "{\"applications\":[{\"packageName\":\"org.onepf.trivialdrive\",\"version\":\"1\",\"installed\":\"true\",\"billingActive\":\"true\",\"products\":[{\"productId\":\"sku_gas\",\"type\":\"inapp\",\"price\":\"50000000\",\"title\":\"GAS\",\"description\":\"car fuel\"},{\"productId\":\"sku_premium\",\"type\":\"inapp\"},{\"productId\":\"sku_infinite_gas\",\"type\":\"subs\"}],\"inventory\":[\"sku_premium\",\"sku_infinite_gas\"]}]}";
    }
}
