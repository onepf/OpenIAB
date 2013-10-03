package org.onepf.oms;

import android.content.SharedPreferences;
import android.test.mock.MockApplication;
import org.onepf.oms.data.Database;

import java.util.Map;

public class MockBillingApplicationBase extends MockApplication implements IBillingApplication {

    protected Database _database;

    @Override
    public Database getDatabase() {
        return _database;
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return new SharedPreferences() {
            @Override
            public Map<String, ?> getAll() {
                return null;
            }

            @Override
            public String getString(String s, String s2) {
                return null;
            }

            @Override
            public int getInt(String s, int i) {
                return 0;
            }

            @Override
            public long getLong(String s, long l) {
                return 0;
            }

            @Override
            public float getFloat(String s, float v) {
                return 0;
            }

            @Override
            public boolean getBoolean(String s, boolean b) {
                return false;
            }

            @Override
            public boolean contains(String s) {
                return false;
            }

            @Override
            public Editor edit() {
                return new Editor() {
                    @Override
                    public Editor putString(String s, String s2) {
                        return null;
                    }

                    @Override
                    public Editor putInt(String s, int i) {
                        return null;
                    }

                    @Override
                    public Editor putLong(String s, long l) {
                        return null;
                    }

                    @Override
                    public Editor putFloat(String s, float v) {
                        return null;
                    }

                    @Override
                    public Editor putBoolean(String s, boolean b) {
                        return null;
                    }

                    @Override
                    public Editor remove(String s) {
                        return null;
                    }

                    @Override
                    public Editor clear() {
                        return null;
                    }

                    @Override
                    public boolean commit() {
                        return false;
                    }
                };
            }

            @Override
            public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
            }

            @Override
            public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
            }
        };
    }
}
