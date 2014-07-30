package org.onepf.oms;

import android.content.Intent;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.SmallTest;

public class AppstoreServiceXmlTest extends ServiceTestCase<AppstoreService> {

    AppstoreBinder _binder;
    MockBillingXmlApplication _app;

    final String _xmlConfig = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><store><application packageName=\"org.onepf.trivialdrive\" version=\"1\" installed=\"true\" billingActive=\"true\"><product productId=\"sku_gas\" type=\"inapp\"><locale name=\"en_US\" title=\"GAS\" description=\"car fuel\" /><price autofill=\"true\"><country name=\"RU\">50000000</country></price></product><product productId=\"sku_premium\" type=\"inapp\" /><product productId=\"sku_infinite_gas\" type=\"subs\" /><inventory><item>sku_premium</item><item>sku_infinite_gas</item></inventory></application></store> ";

    public AppstoreServiceXmlTest() {
        super(AppstoreService.class);
    }

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

    private void start(String xml) throws Exception {
        _app = new MockBillingXmlApplication(xml);
        _app.onCreate();
        setApplication(_app);
        _binder = (AppstoreBinder) bindService(new Intent());
    }

    @SmallTest
    public void testNullBind() throws Exception {
        start(null);
        assertTrue(_binder != null);
        assertTrue(_app != null);
        assertTrue(_app.getDatabase() != null);
    }

    @SmallTest
    public void testEmptyBind() throws Exception {
        start("");
        assertTrue(_binder != null);
        assertTrue(_app != null);
        assertTrue(_app.getDatabase() != null);
    }

    @SmallTest
    public void testEmptyXmlBind() throws Exception {
        start("<?xml version=\"1.0\" encoding=\"UTF-8\"?><store/>");
        assertTrue(_binder != null);
        assertTrue(_app != null);
        assertTrue(_app.getDatabase() != null);
    }

    @SmallTest
    public void testInvalidXmlBind() throws Exception {
        start("<boo>");
        assertTrue(_binder != null);
        assertTrue(_app != null);
        assertTrue(_app.getDatabase() != null);
    }

    @SmallTest
    public void testProperXmlBind() throws Exception {
        start(_xmlConfig);
        assertTrue(_binder != null);
        assertTrue(_app != null);
        assertTrue(_app.getDatabase() != null);
    }

    @SmallTest
    public void testIsPackageInstaller() throws Exception {
        start("<?xml version=\"1.0\" encoding=\"UTF-8\" ?> " +
                "<store> " +
                "    <application packageName=\"org.some.app\" " +
                "                 version=\"1\" " +
                "                 installed=\"true\"/> " +
                "</store> ");
        assertTrue(_binder.isPackageInstaller("org.some.app"));
    }

//    @SmallTest
//    public void testIsNotPackageInstaller() throws Exception {
//        start("<?xml version=\"1.0\" encoding=\"UTF-8\" ?> " +
//                "<store> " +
//                "    <application packageName=\"org.some.app\" " +
//                "                 version=\"1\" " +
//                "                 installed=\"false\"/> " +
//                "</store> ");
//        assertFalse(_binder.isPackageInstaller("org.some.app"));
//    }

    @SmallTest
    public void testIsBillingAvailable() throws Exception {
        start("<?xml version=\"1.0\" encoding=\"UTF-8\" ?> " +
                "<store> " +
                "    <application packageName=\"org.some.app\" " +
                "                 version=\"1\" " +
                "                 billingActive=\"true\"/> " +
                "</store> ");
        assertTrue(_binder.isBillingAvailable("org.some.app"));
    }

//    @SmallTest
//    public void testIsNotBillingAvailable() throws Exception {
//        start("<?xml version=\"1.0\" encoding=\"UTF-8\" ?> " +
//                "<store> " +
//                "    <application packageName=\"org.some.app\" " +
//                "                 version=\"1\" " +
//                "                 billingActive=\"false\"/> " +
//                "</store> ");
//        assertFalse(_binder.isBillingAvailable("org.some.app"));
//    }

//    @SmallTest
//    public void testVersion() throws Exception {
//        start("<?xml version=\"1.0\" encoding=\"UTF-8\" ?> " +
//                "<store> " +
//                "    <application packageName=\"org.some.app\" " +
//                "                 version=\"666\"/> " +
//                "</store> ");
//        assertTrue(_binder.getPackageVersion("org.some.app") == 666);
//    }

    @SmallTest
    public void testBillingServiceIntent() throws Exception {
        start("");
        Intent intent = _binder.getBillingServiceIntent();
        assertTrue(intent != null);
        assertEquals(intent.getAction(), "org.onepf.oms.billing.BIND");
    }
}
