package org.onepf.oms;

import org.onepf.oms.data.CSVException;
import org.onepf.oms.data.Database;

public class MockBillingCsvApplication extends MockBillingApplicationBase {

    String _csv;

    public MockBillingCsvApplication(String csv) {
        _csv = csv;
    }

    @Override
    public void onCreate() {
        try {
            _database = new Database(this);
            _database.deserializeFromGoogleCSV(_csv);
        } catch (CSVException e) {
            _database = new Database(this);
        }
    }
}
