package org.onepf.oms;

import android.test.mock.MockApplication;

public class BillingServiceCsvTest extends BillingServiceTestBase {

    public BillingServiceCsvTest() {
        super(BillingService.class);
    }

    @Override
    protected MockApplication getApp(String config) throws Exception {
        return new MockBillingCsvApplication(config);
    }


    @Override
    protected String getConfig() {
        return "Product ID,Published State,Purchase Type,Auto Translate,Locale; Title; Description,Auto Fill Prices,Price\n" +
                "sku_infinite_gas,published,managed_by_android,false,en_US; Infinite Gas Subscription; For 1 month,true,100000000,subs\n" +
                "sku_gas,published,managed_by_android,false,en_US; Some Gas; Fulfil tank for 1/4,true,50000000,inapp\n" +
                "sku_premium,published,managed_by_android,false,en_US; Premium Upgrade: Red Color; Red color for your car,false,RU; 30000000; AU; 990000; AT; 680000; BE; 680000; BR; 2070000; CA; 990000; CZ; 19500000; DK; 6000000; EE; 680000; FI; 680000; FR; 680000; DE; 680000; GR; 680000; HK; 7000000; HU; 225000000; IN; 58890000; IE; 680000; IL; 3270000; IT; 680000; JP; 99000000; LU; 680000; MX; 11900000; NL; 680000; NZ; 1130000; NO; 6000000; PL; 2990000; PT; 680000; SG; 1150000; SK; 680000; SI; 680000; KR; 999000000; ES; 680000; SE; 7000000; CH; 990000; TW; 30000000; GB; 580000; US; 990000\n";
    }
}
