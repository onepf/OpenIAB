package org.onepf.oms;

import android.app.Application;
import android.util.Log;
import org.json.JSONException;
import org.onepf.oms.data.Database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class BillingApplication extends Application implements IBillingApplication {

    public static final String TAG = "OnePF_store";

    Database _database;

    @Override
    public Database getDatabase() {
        return _database;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            _database = new Database(XmlHelper.loadXMLFromString(readTextFileFromAssets("config.xml")));
        } catch (Exception e) {
            Log.e(TAG, "Couldn't parse provided 'config' file", e);
            _database = new Database();
        }
    }

    public String readTextFileFromAssets(String fileName) {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(getAssets().open(fileName)));
            String temp;
            while ((temp = br.readLine()) != null) {
                sb.append(temp);
            }
        } catch (IOException e) {
            Log.e(TAG, "Couldn't read 'config' from assets", e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Couldn't close stream while reading 'config' from assets", e);
            }
        }
        return sb.toString();
    }
}
