package org.onepf.oms;

import org.onepf.oms.data.Database;

public class MockBillingXmlApplication extends MockBillingApplicationBase {

    String _xml;

    public MockBillingXmlApplication(String xml) {
        _xml = xml;
    }

    @Override
    public void onCreate() {
        try {
            _database = new Database(this);
            _database.deserializeFromOnePFXML(_xml);
        } catch (Exception e) {
            _database = new Database(this);
        }
    }
}
