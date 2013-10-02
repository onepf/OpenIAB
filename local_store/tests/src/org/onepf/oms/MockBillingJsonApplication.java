package org.onepf.oms;

import android.test.mock.MockApplication;
import org.json.JSONException;
import org.onepf.oms.data.Database;

public class MockBillingJsonApplication extends MockApplication implements IBillingApplication {

    Database _database;
    String _json;

    public MockBillingJsonApplication(String json) {
        _json = json;
    }

    @Override
    public Database getDatabase() {
        return _database;
    }

    @Override
    public void onCreate() {
//        try {
            //_database = new Database(_json);
            _database = new Database();
//        } catch (JSONException e) {
//            _database = new Database();
//        }
    }
}
