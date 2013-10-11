package org.onepf.oms;

import android.app.Application;
import android.os.Environment;
import android.os.FileObserver;
import android.util.Log;
import org.onepf.oms.data.Database;

import java.io.*;

public class BillingApplication extends Application implements IBillingApplication {

    public static final String TAG = "OnePF-store";
    static final String GOOGLE_CONFIG_FILE = "google-play.csv";
    static final String AMAZON_CONFIG_FILE = "amazon.sdktester.json";
    static final String ONEPF_CONFIG_FILE = "onepf.xml";

    Database _database;
    FileObserver _configObserver;

    @Override
    public Database getDatabase() {
        return _database;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        copyConfigFromAssets(GOOGLE_CONFIG_FILE);
        copyConfigFromAssets(AMAZON_CONFIG_FILE);
        copyConfigFromAssets(ONEPF_CONFIG_FILE);

        if (createDbFromConfig()) {
            _configObserver = new FileObserver(getConfigDir()) {
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

    private void copyConfigFromAssets(String configFile) {
        File configDir = new File(getConfigDir());
        if (!configDir.exists()) {
            if (!configDir.mkdirs()) {
                Log.e(TAG, "Problem creating config folder");
                return;
            }
        }

        File outFile = new File(getConfigDir(), configFile);
        if (outFile.exists()) {
            return;
        }

        InputStream in;
        OutputStream out;
        try {
            in = getAssets().open(configFile);
            out = new FileOutputStream(outFile);
            copyFile(in, out);
            in.close();
            out.flush();
            out.close();
        } catch(IOException e) {
            Log.e(TAG, "Failed to copy asset file: " + configFile, e);
        }
    }

    private String getConfigDir() {
        return Environment.getExternalStorageDirectory() + File.separator + "OnePF-store";
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    private boolean createDbFromConfig() {
        try {
            _database = new Database(this);
            _database.deserializeFromGoogleCSV(readConfigFromSdCard(GOOGLE_CONFIG_FILE));
            _database.deserializeFromAmazonJson(readConfigFromSdCard(AMAZON_CONFIG_FILE));
            _database.deserializeFromOnePFXML(readConfigFromSdCard(ONEPF_CONFIG_FILE));
        } catch (Exception e) {
            Log.e(TAG, "Couldn't parse provided 'config' file", e);
            _database = new Database(this);
            return false;
        }
        return true;
    }

    private String readConfigFromSdCard(String configFile) {
        File file = new File(getConfigDir(), configFile);
        if (!file.exists()) {
            Log.i(TAG, "'config' file not found");
            return "";
        }
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String temp;
            while ((temp = br.readLine()) != null) {
                sb.append(temp).append("\n");
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
}
