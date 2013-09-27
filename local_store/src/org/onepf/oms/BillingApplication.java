package org.onepf.oms;

import android.app.Application;
import android.os.FileObserver;
import android.util.Log;
import org.onepf.oms.data.Database;

import java.io.*;

public class BillingApplication extends Application implements IBillingApplication {

    public static final String TAG = "OnePF_store";
    static final String CONFIG_PATH = "/mnt/sdcard";

    Database _database;
    FileObserver _configObserver;

    @Override
    public Database getDatabase() {
        return _database;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (createDbFromConfig()) {
            _configObserver = new FileObserver(CONFIG_PATH) {
                @Override
                public void onEvent(int event, String file) {
                    switch (event) {
                        case FileObserver.CLOSE_WRITE:
                            createDbFromConfig();
                            break;
                    }
                }
            };
            _configObserver.startWatching();
        }
    }

    private boolean createDbFromConfig() {
        try {
            _database = new Database(XmlHelper.loadXMLFromString(readTextFileFromSdCard("config.xml")));
        } catch (Exception e) {
            Log.e(TAG, "Couldn't parse provided 'config' file", e);
            _database = new Database();
            return false;
        }
        return true;
    }

    public String readTextFileFromSdCard(String fileName) {
        File file = new File(CONFIG_PATH, fileName);
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String temp;
            while ((temp = br.readLine()) != null) {
                sb.append(temp);
            }
        } catch (IOException e) {
            Log.e(TAG, "Couldn't read 'config'", e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Couldn't close stream while reading 'config'", e);
            }
        }
        return sb.toString();
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
