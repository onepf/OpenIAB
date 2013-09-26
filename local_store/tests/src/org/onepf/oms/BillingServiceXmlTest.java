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
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?> " +
                "<store> " +
                "    <application packageName=\"org.onepf.trivialdrive\" " +
                "                 version=\"1\" " +
                "                 installed=\"true\" " +
                "                 billingActive=\"true\"> " +
                " " +
                "        <product productId=\"sku_gas\" " +
                "                 type=\"inapp\"> " +
                "            <locale name=\"en_US\" " +
                "                    title=\"GAS\" " +
                "                    description=\"car fuel\"/> " +
                "            <price autofill=\"true\"> " +
                "                <country name=\"RU\">50000000</country> " +
                "            </price> " +
                "        </product> " +
                " " +
                "        <product productId=\"sku_premium\" " +
                "                 type=\"inapp\"/> " +
                " " +
                "        <product productId=\"sku_infinite_gas\" " +
                "                 type=\"subs\"/> " +
                " " +
                "        <inventory> " +
                "            <item>sku_premium</item> " +
                "            <item>sku_infinite_gas</item> " +
                "        </inventory> " +
                "    </application> " +
                "</store> ";
    }
}
