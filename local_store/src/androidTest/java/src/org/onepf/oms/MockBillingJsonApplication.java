package org.onepf.oms;

import org.json.JSONException;
import org.onepf.oms.data.Database;

public class MockBillingJsonApplication extends MockBillingApplicationBase  {

    String _json;

    public MockBillingJsonApplication(String json) {
        _json = json;
    }

    @Override
    public void onCreate() {
        try {
            _database = new Database(this);
            _database.deserializeFromAmazonJson(_json);
        } catch (JSONException e) {
            _database = new Database(this);
        }
    }
}
