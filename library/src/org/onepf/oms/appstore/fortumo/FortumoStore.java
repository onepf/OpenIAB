package org.onepf.oms.appstore.fortumo;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import org.onepf.oms.Appstore;
import org.onepf.oms.AppstoreInAppBillingService;
import org.onepf.oms.DefaultAppstore;
import org.onepf.oms.OpenIabHelper;

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
        return fortumoSharedPrefs.getString(productId, null);
    }

    static void removePendingProduct(Context context, String productId) {
        final SharedPreferences fortumoSharedPrefs = getFortumoSharedPrefs(context);
        final SharedPreferences.Editor edit = fortumoSharedPrefs.edit();
        edit.remove(productId);
        edit.commit();
    }

}
