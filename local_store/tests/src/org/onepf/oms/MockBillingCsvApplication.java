package org.onepf.oms;

import android.test.mock.MockApplication;
import org.onepf.oms.data.CSVException;
import org.onepf.oms.data.Database;

public class MockBillingCsvApplication extends MockApplication implements IBillingApplication {

    Database _database;
    String _csv;

    public MockBillingCsvApplication(String csv) {
        _csv = csv;
    }

    @Override
    public Database getDatabase() {
        return _database;
    }

    @Override
    public void onCreate() {
        try {
            _database = new Database();
            _database.deserializeFromGoogleCSV(_csv);
        } catch (CSVException e) {
            _database = new Database();
        }
    }
}
