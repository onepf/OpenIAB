package org.onepf.oms.appstore.fortumo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import mp.*;
import org.onepf.oms.Appstore;
import org.onepf.oms.AppstoreInAppBillingService;
import org.onepf.oms.DefaultAppstore;
import org.onepf.oms.OpenIabHelper;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * Created by akarimova on 23.12.13.
 */

/**
 * Fortumo, an international mobile payment provider, is not actually an app store.
 * This class was made to provide in-app purchasing compatibility with other, "real", stores.
 */
public class FortumoStore extends DefaultAppstore {
    public static final String IN_APP_PRODUCTS_FILE_NAME = "inapps_products.xml";
    public static final String FORTUMO_DETATILS_FILE_NAME = "fortumo_inapps_details.xml";

    static final String SHARED_PREFS_FORTUMO = "onepf_shared_prefs_fortumo";


    private Context context;
    private FortumoBillingService billingService;

    public FortumoStore(Context context) {
        this.context = context;
    }


    @Override
    public boolean isPackageInstaller(String packageName) {
        //Fortumo is not an app. It can't be an installer.
        return false;
    }

    @Override
    public boolean isBillingAvailable(String packageName) {
        //SMS support is required to make payments
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    @Override
    public int getPackageVersion(String packageName) {
        return Appstore.PACKAGE_VERSION_UNDEFINED;
    }

    @Override
    public String getAppstoreName() {
        return OpenIabHelper.NAME_FORTUMO;
    }

    @Override
    public AppstoreInAppBillingService getInAppBillingService() {
        if (billingService == null) {
            billingService = new FortumoBillingService(context);
        }
        return billingService;
    }


    static void startPaymentActivityForResult(Activity activity, int requestCode, PaymentRequest paymentRequest) {
        Intent localIntent = paymentRequest.toIntent(activity);
        activity.startActivityForResult(localIntent, requestCode);
    }

    private static void checkPermission(Context context, String paramString) {
        if (context.checkCallingOrSelfPermission(paramString) != PackageManager.PERMISSION_GRANTED)
            throwFortumoNotConfiguredException(String.format("Required permission \"%s\" is NOT granted.", paramString));
    }


    /**
     * Checks for the presence of permissions and components' declarations that are required to support Fortumo billing.<br>
     * Full Example of AndroidManifest.xml:<br>
     * <pre>
     * {@code
     * <!-- Permissions -->
     * <uses-permission android:name="android.permission.INTERNET"/>
     * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>"
     * <uses-permission android:name="android.permission.READ_PHONE_STATE"/>"
     * <uses-permission android:name="android.permission.RECEIVE_SMS"/>"
     * <uses-permission android:name="android.permission.SEND_SMS"/>"
     *   <!-- Define your own permission to protect payment broadcast -->
     *   <permission android:name="com.your.domain.PAYMENT_BROADCAST_PERMISSION"
     *                android:label="Read payment status"
     *                android:protectionLevel="signature"/>
     *   <!-- "signature" permission granted automatically by system, without notifying user. -->
     *   <uses-permission android:name="com.your.domain.PAYMENT_BROADCAST_PERMISSION"/>
     *   <application android:icon="@drawable/ic_launcher" android:label="@string/app_name">
     * <!-- Declare these objects, this is part of Fortumo SDK,
     * and should not be called directly -->
     * <receiver android:name="mp.MpSMSReceiver">
     * <intent-filter>
     * <action android:name="android.provider.Telephony.SMS_RECEIVED" />
     * </intent-filter>
     * </receiver>
     * <service android:name="mp.MpService" />
     * <service android:name="mp.StatusUpdateService" />
     * <activity android:name="mp.MpActivity"
     * android:theme="@android:style/Theme.Translucent.NoTitleBar"
     * android:configChanges="orientation|keyboardHidden|screenSize" />
     * <!-- Other application objects -->
     * <activity android:label="@string/app_name" android:name=".YourActivity">
     * <intent-filter>
     * <action android:name="android.intent.action.MAIN" />
     * <category android:name="android.intent.category.LAUNCHER" />
     * </intent-filter>
     * </activity>
     * ...
     * }
     * </pre>
     */
    public static void checkManifest(Context context) {
        checkPermission(context, "android.permission.INTERNET");
        checkPermission(context, "android.permission.ACCESS_NETWORK_STATE");
        checkPermission(context, "android.permission.READ_PHONE_STATE");
        checkPermission(context, "android.permission.RECEIVE_SMS");
        checkPermission(context, "android.permission.SEND_SMS");

//        String appDeclaredPermission = null;
//        try {
//            Class permissionClass = Class.forName(context.getPackageName() + ".Manifest$permission");
//            Field paymentBroadcastPermission = permissionClass.getField("PAYMENT_BROADCAST_PERMISSION");
//            appDeclaredPermission = (String) paymentBroadcastPermission.get(null);
//        } catch (Exception ignored) {
//        }
//        if (TextUtils.isEmpty(appDeclaredPermission)) {
//            throwFortumoNotConfiguredException("PAYMENT_BROADCAST_PERMISSION is NOT declared.");
//        }

        Intent paymentActivityIntent = new Intent();
        paymentActivityIntent.setClassName(context.getPackageName(), MpActivity.class.getName());
        if (context.getPackageManager().resolveActivity(paymentActivityIntent, 0) == null) {
            throwFortumoNotConfiguredException("mp.MpActivity is NOT declared.");
        }

        Intent mpServerIntent = new Intent();
        mpServerIntent.setClassName(context.getPackageName(), MpService.class.getName());
        if (context.getPackageManager().resolveService(mpServerIntent, 0) == null) {
            throwFortumoNotConfiguredException("mp.MpService is NOT declared.");
        }

        Intent statusUpdateServiceIntent = new Intent();
        statusUpdateServiceIntent.setClassName(context.getPackageName(), StatusUpdateService.class.getName());
        if (context.getPackageManager().resolveService(statusUpdateServiceIntent, 0) == null) {
            throwFortumoNotConfiguredException("mp.StatusUpdateService is NOT declared.");
        }
    }

    private static void throwFortumoNotConfiguredException(String itemDescription) {
        throw new IllegalStateException(itemDescription + "\nTo support Fortumo your AndroidManifest.xml must contain:\n" +
                " <!-- Permissions -->\n" +
                "  <uses-permission android:name=\"android.permission.INTERNET\" />\n" +
                "  <uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\" />\n" +
                "  <uses-permission android:name=\"android.permission.READ_PHONE_STATE\" />\n" +
                "  <uses-permission android:name=\"android.permission.RECEIVE_SMS\" />\n" +
                "  <uses-permission android:name=\"android.permission.SEND_SMS\" />\n" +
                "\n" +
                "  <!-- Define your own permission to protect payment broadcast -->\n" +
                "  <permission android:name=\"com.your.domain.PAYMENT_BROADCAST_PERMISSION\" \n" +
                "               android:label=\"Read payment status\" \n" +
                "               android:protectionLevel=\"signature\" />\n" +
                "  <!-- \"signature\" permission granted automatically by system, without notifying user. -->\n" +
                "  <uses-permission android:name=\"com.your.domain.PAYMENT_BROADCAST_PERMISSION\" />\n" +
                "\n" +
                "  <application android:icon=\"@drawable/ic_launcher\" android:label=\"@string/app_name\">\n" +
                "    <!-- Declare these objects, this is part of Fortumo SDK,\n" +
                "    and should not be called directly -->\n" +
                "    <receiver android:name=\"mp.MpSMSReceiver\">\n" +
                "      <intent-filter>\n" +
                "      <action android:name=\"android.provider.Telephony.SMS_RECEIVED\" />\n" +
                "    </intent-filter>\n" +
                "  </receiver>\n" +
                "  <service android:name=\"mp.MpService\" />\n" +
                "  <service android:name=\"mp.StatusUpdateService\" />\n" +
                "  <activity android:name=\"mp.MpActivity\" \n" +
                "             android:theme=\"@android:style/Theme.Translucent.NoTitleBar\"\n" +
                "             android:configChanges=\"orientation|keyboardHidden|screenSize\" />\n" +
                "\n" +
                "  <!-- Other application objects -->\n" +
                "  <activity android:label=\"@string/app_name\" android:name=\".YourActivity\">\n" +
                "    <intent-filter>\n" +
                "      <action android:name=\"android.intent.action.MAIN\" />\n" +
                "      <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
                "    </intent-filter>\n" +
                "  </activity>\n" +
                "  ...");
    }

    /**
     * Checks all required by Fortumo elements
     *
     * @param context must be not null
     */
    public static void checkSettings(Context context) {
        checkManifest(context);
        checkJars();
        checkDataXmlFiles(context);
    }

    /**
     * Checks for the presence of fortumo classes
     */
    private static void checkJars() {
        try {
            FortumoStore.class.getClassLoader().loadClass("mp.MpUtils");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("To support Fortumo a fortumo jar is needed.");
        }
    }

    /**
     * To support Fortumo 2 files need to be added: a file with all inapp-products description and a file with fortumo-specific elements (service id, service inapp secret, is item consumable or not)
     *
     * @param context to get access to asset manager
     */
    private static void checkDataXmlFiles(Context context) {
        try {
            final List<String> strings = Arrays.asList(context.getResources().getAssets().list(""));
            final boolean hasProductFile = strings.contains(FortumoStore.IN_APP_PRODUCTS_FILE_NAME);
            final boolean hasFortumoDetailsFile = strings.contains(FortumoStore.FORTUMO_DETATILS_FILE_NAME);
            if (!(hasProductFile && hasFortumoDetailsFile)) {
                throw new IllegalStateException("To support Fortumo you need the following xml files: " + FortumoStore.IN_APP_PRODUCTS_FILE_NAME + "&" + FortumoStore.FORTUMO_DETATILS_FILE_NAME);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Can't parse the required xml files: " + FortumoStore.IN_APP_PRODUCTS_FILE_NAME + "&" + FortumoStore.FORTUMO_DETATILS_FILE_NAME);
        }
    }

    static SharedPreferences getFortumoSharedPrefs(Context context) {
        return context.getSharedPreferences(FortumoStore.SHARED_PREFS_FORTUMO, Context.MODE_PRIVATE);
    }

    static void addPendingPayment(Context context, String productId, String messageId) {
        final SharedPreferences fortumoSharedPrefs = getFortumoSharedPrefs(context);
        final SharedPreferences.Editor editor = fortumoSharedPrefs.edit();
        editor.putString(productId, messageId);
        editor.commit();
    }

    static String getMessageIdInPending(Context context, String productId) {
        final SharedPreferences fortumoSharedPrefs = getFortumoSharedPrefs(context);
        final String messageIdForProduct = fortumoSharedPrefs.getString(productId, null);
        return messageIdForProduct;
    }

    static void removePendingProduct(Context context, String productId) {
        final SharedPreferences fortumoSharedPrefs = getFortumoSharedPrefs(context);
        final SharedPreferences.Editor edit = fortumoSharedPrefs.edit();
        edit.remove(productId);
        edit.commit();
    }

}
