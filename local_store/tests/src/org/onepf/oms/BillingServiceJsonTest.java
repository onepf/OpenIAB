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
        return "{\"sku_premium\":{\"description\":\"Red color for your car\",\"title\":\"Premium Upgrade: Red Color\",\"itemType\":\"ENTITLED\",\"price\":0.0,\"smallIconUrl\":\"https://s3-external-1.amazonaws.com/com-amazon-mas-catalog/M3Q90L0VNS056B%2FM21KGKW42WYYNL%2Fimages%2F_9cbc6f46-9fd6-4aeb-8c80-2d1a00b272f8_137ceaf73d63da7aa18c079d8367ffab\"},\"sku_gas\":{\"description\":\"Fulfil tank for 1/4\",\"title\":\"Some Gas\",\"itemType\":\"CONSUMABLE\",\"price\":50000000,\"smallIconUrl\":\"https://s3-external-1.amazonaws.com/com-amazon-mas-catalog/M3Q90L0VNS056B%2FM15KG0C1BK49B9%2Fimages%2F_59c1b075-c3f8-4c3a-ac91-5a9307ac7172_52a8cf3601aab22b2e2760c8eaf21587\"},\"sku_infinite_gas\":{\"description\":\"For 1 month\",\"title\":\"Infinite Gas Subscription\",\"itemType\":\"SUBSCRIPTION\",\"price\":0.0,\"smallIconUrl\":\"https://s3-external-1.amazonaws.com/com-amazon-mas-catalog/M3Q90L0VNS056B%2FM2NYN8TJDTDG86%2Fimages%2F_e58b3162-0c8a-43da-a766-5625d39b592f_434269651dead989e2eb402cbe2c511f\",\"subscriptionParent\":\"org.onepf.trivialdrive.amazon.infinite_gas\"}}";
    }
}
