package org.onepf.oms;

import android.content.Intent;
import android.os.RemoteException;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.SmallTest;

public class AppstoreServiceJsonTest extends ServiceTestCase<AppstoreService> {

    AppstoreBinder _binder;
    MockBillingJsonApplication _app;

    final String _jsonConfig = "{\"applications\":[{\"packageName\":\"org.onepf.trivialdrive\",\"version\":\"1\",\"installed\":\"true\",\"billingActive\":\"true\",\"products\":[{\"productId\":\"sku_gas\",\"type\":\"inapp\",\"price\":\"50\",\"title\":\"GAS\",\"description\":\"car fuel\"},{\"productId\":\"sku_premium\",\"type\":\"inapp\"},{\"productId\":\"sku_infinite_gas\",\"type\":\"subs\"}],\"inventory\":[\"sku_premium\",\"sku_infinite_gas\"]}]}";

    public AppstoreServiceJsonTest() {
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

    private void start(String json) {
        _app = new MockBillingJsonApplication(json);
        _app.onCreate();
        setApplication(_app);
        _binder = (AppstoreBinder) bindService(new Intent());
    }

    @SmallTest
    public void testNullBind() {
        start(null);
        assertTrue(_binder != null);
        assertTrue(_app != null);
        assertTrue(_app.getDatabase() != null);
    }

    @SmallTest
    public void testEmptyBind() {
        start("");
        assertTrue(_binder != null);
        assertTrue(_app != null);
        assertTrue(_app.getDatabase() != null);
    }

    @SmallTest
    public void testEmptyJsonBind() {
        start("{}");
        assertTrue(_binder != null);
        assertTrue(_app != null);
        assertTrue(_app.getDatabase() != null);
    }

    @SmallTest
    public void testInvalidJsonBind() {
        start("{ error }");
        assertTrue(_binder != null);
        assertTrue(_app != null);
        assertTrue(_app.getDatabase() != null);
    }

    @SmallTest
    public void testProperJsonBind() {
        start(_jsonConfig);
        assertTrue(_binder != null);
        assertTrue(_app != null);
        assertTrue(_app.getDatabase() != null);
    }

    @SmallTest
    public void testIsPackageInstaller() throws RemoteException {
        start("{\"applications\":[{\"packageName\":\"org.some.app\",\"installed\":\"true\"}]}");
        assertTrue(_binder.isPackageInstaller("org.some.app"));
    }

//    @SmallTest
//    public void testIsNotPackageInstaller() throws RemoteException {
//        start("{\"applications\":[{\"packageName\":\"org.some.app\",\"installed\":\"false\"}]}");
//        assertFalse(_binder.isPackageInstaller("org.some.app"));
//    }

    @SmallTest
    public void testIsBillingAvailable() throws RemoteException {
        start("{\"applications\":[{\"packageName\":\"org.some.app\",\"billingActive\":\"true\"}]}");
        assertTrue(_binder.isBillingAvailable("org.some.app"));
    }

//    @SmallTest
//    public void testIsNotBillingAvailable() throws RemoteException {
//        start("{\"applications\":[{\"packageName\":\"org.some.app\",\"billingActive\":\"false\"}]}");
//        assertFalse(_binder.isBillingAvailable("org.some.app"));
//    }

//    @SmallTest
//    public void testVersion() throws RemoteException {
//        start("{\"applications\":[{\"packageName\":\"org.some.app\",\"version\":\"666\"}]}");
//        assertTrue(_binder.getPackageVersion("org.some.app") == 666);
//    }

    @SmallTest
    public void testBillingServiceIntent() throws RemoteException {
        start("");
        Intent intent = _binder.getBillingServiceIntent();
        assertTrue(intent != null);
        assertEquals(intent.getAction(), "org.onepf.oms.billing.BIND");
    }
}
