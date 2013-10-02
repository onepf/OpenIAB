package org.onepf.oms;

import android.test.mock.MockApplication;
import org.onepf.oms.data.Database;
import org.w3c.dom.Document;

public class MockBillingXmlApplication extends MockApplication implements IBillingApplication {

    Database _database;
    Document _xml;

    public MockBillingXmlApplication(String xml) throws Exception {
        _xml = XmlHelper.loadXMLFromString(xml);
    }

    @Override
    public Database getDatabase() {
        return _database;
    }

    @Override
    public void onCreate() {
//        try {
//            _database = new Database(_xml);
//        } catch (Exception e) {
            _database = new Database();
//        }
    }
}
