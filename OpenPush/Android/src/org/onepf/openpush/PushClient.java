package org.onepf.openpush;

import android.app.Activity;
import android.os.AsyncTask;
import android.text.TextUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.unity3d.player.UnityPlayer;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class PushClient {

    private static final String TAG = "OpenPush";

    private static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private static final String EVENT_RECEIVER = "OpenPush";
    private static final String INIT_SUCCEEDED_CALLBACK = "OnInitSucceeded";
    private static final String INIT_FAILED_CALLBACK = "OnInitFailed";

    GoogleCloudMessaging _gcm;
    Activity _context;
    String _serverUrl;
    String _senderId;
    String _regId;

    private static PushClient _instance;

    public static PushClient instance() {
        if (_instance == null) {
            _instance = new PushClient();
        }
        return _instance;
    }

    public void init(String serverUrl, String senderId) {
        _serverUrl = serverUrl;
        _senderId = senderId;
        _context = UnityPlayer.currentActivity;

        // Check device for Play Services APK.
        if (checkPlayServices()) {
            _gcm = GoogleCloudMessaging.getInstance(_context);
            _regId = getRegistrationId();
            registerInBackground();
        } else {
            UnityPlayer.UnitySendMessage(EVENT_RECEIVER, INIT_FAILED_CALLBACK, "No valid Google Play Services APK found.");
        }
    }

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId() {
        final SharedPreferences prefs = getGcmPreferences(_context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (TextUtils.isEmpty(registrationId)) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(_context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String error = "";
                try {
                    if (TextUtils.isEmpty(_regId)) {
                        _regId = _gcm.register(_senderId);
                        storeRegistrationId(_context, _regId);
                    }
                    sendRegistrationIdToBackend();
                } catch (IOException ex) {
                    error = ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return error;
            }

            @Override
            protected void onPostExecute(String msg) {
                if (!TextUtils.isEmpty(msg))
                    UnityPlayer.UnitySendMessage(EVENT_RECEIVER, INIT_FAILED_CALLBACK, msg);
            }
        }.execute(null, null, null);
    }

    /**
     * Sends the registration ID to the server over HTTP
     */
    private void sendRegistrationIdToBackend() {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(_serverUrl);
        String postData = "{\"platform\":\"android\",\"token\":\"" + _regId + "\"}";
        InputStreamEntity reqEntity = new InputStreamEntity(new ByteArrayInputStream(postData.getBytes()), postData.length());
        reqEntity.setContentType("text/plain");
        httpPost.setEntity(reqEntity);

        HttpResponse httpResponse;
        try {
            httpResponse = httpClient.execute(httpPost);
        } catch (Exception e) {
            UnityPlayer.UnitySendMessage(EVENT_RECEIVER, INIT_FAILED_CALLBACK, e.getMessage());
            return;
        }

        StatusLine statusLine = httpResponse.getStatusLine();
        if (statusLine.getStatusCode() == HttpStatus.SC_OK)
            UnityPlayer.UnitySendMessage(EVENT_RECEIVER, INIT_SUCCEEDED_CALLBACK, "");
        else
            UnityPlayer.UnitySendMessage(EVENT_RECEIVER, INIT_FAILED_CALLBACK, "Registration failed");
    }

    private SharedPreferences getGcmPreferences(Context context) {
        return context.getSharedPreferences("GCM", Context.MODE_PRIVATE);
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // Should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGcmPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(_context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, _context, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                return false;
            }
            return false;
        }
        return true;
    }

}
