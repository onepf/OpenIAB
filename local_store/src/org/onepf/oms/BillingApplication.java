package org.onepf.oms;

import android.app.Application;
import android.os.Environment;
import android.os.FileObserver;
import android.util.Log;
import org.onepf.oms.data.Database;

import java.io.*;

public class BillingApplication extends Application implements IBillingApplication {

    public static final String TAG = "OnePF-store";
    static final String CONFIG_FILE = "config.xml";

    Database _database;
    FileObserver _configObserver;

    @Override
    public Database getDatabase() {
        return _database;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        copyConfigFromAssets();

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

    private void copyConfigFromAssets() {
        File configDir = new File(getConfigDir());
        if (!configDir.exists()) {
            if (!configDir.mkdirs()) {
                Log.e(TAG, "Problem creating config folder");
                return;
            }
        }
        //Log.i(TAG, "Config file path: " + configDir.toString());

        File outFile = new File(getConfigDir(), CONFIG_FILE);
        if (outFile.exists()) {
            return;
        }

        InputStream in;
        OutputStream out;
        try {
            in = getAssets().open(CONFIG_FILE);
            out = new FileOutputStream(outFile);
            copyFile(in, out);
            in.close();
            out.flush();
            out.close();
        } catch(IOException e) {
            Log.e(TAG, "Failed to copy asset file: config.xml", e);
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
            _database = new Database(XmlHelper.loadXMLFromString(readConfigFromSdCard()));
        } catch (Exception e) {
            Log.e(TAG, "Couldn't parse provided 'config' file", e);
            _database = new Database();
            return false;
        }
        return true;
    }

    private String readConfigFromSdCard() {
        File file = new File(getConfigDir(), CONFIG_FILE);
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
}
