package org.onepf.oms.appstore;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import org.onepf.oms.Appstore;
import org.onepf.oms.AppstoreInAppBillingService;
import org.onepf.oms.DefaultAppstore;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.IabException;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Inventory;

import java.util.concurrent.CountDownLatch;

/**
 * Fortumo, an international mobile payment provider, is not actually an app store.
 * This class was made to provide in-app purchasing compatibility with other, "real", stores.
 *
 * @author akarimova@onepf.org
 * @since 23.12.13
 */
public class FortumoStore extends DefaultAppstore {
    private static final String TAG = FortumoStore.class.getSimpleName();
    private Boolean isNookDevice;
    private Boolean isBillingAvailable;

    /**
     * Contains information about all in-app products
     */
    public static final String IN_APP_PRODUCTS_FILE_NAME = "inapps_products.xml";

    /**
     * Contains additional information about Fortumo services
     */
    public static final String FORTUMO_DETAILS_FILE_NAME = "fortumo_inapps_details.xml";

    private static boolean isDebugLog() {
        return OpenIabHelper.isDebugLog();
    }

    private Context context;
    private FortumoBillingService billingService;

    public FortumoStore(Context context) {
        this.context = context.getApplicationContext();
        isNookDevice = isNookDevice();
    }

    /**
     * Fortumo doesn't have an app store. It can't be an installer.
     *
     * @return false
     */
    @Override
    public boolean isPackageInstaller(String packageName) {
        return false;
    }

    @Override
    public boolean isBillingAvailable(String packageName) {
        if (isBillingAvailable != null) {
            return isBillingAvailable;
        }
        if (!isNookDevice) {
            final boolean hasTelephonyFeature = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
            if (isDebugLog()) {
                Log.d(TAG, "isBillingAvailable: has FEATURE_TELEPHONY " + hasTelephonyFeature);
            }
            if (!hasTelephonyFeature) {
                return isBillingAvailable = false;
            }
        }
        billingService = (FortumoBillingService) getInAppBillingService();
        isBillingAvailable = billingService.setupBilling(isNookDevice);
        if (isDebugLog()) {
            Log.d(TAG, "isBillingAvailable: " + isBillingAvailable);
        }
        return isBillingAvailable;
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
            billingService = new FortumoBillingService(context, isNookDevice);
        }
        return billingService;
    }

    //todo check for different devices
    private static boolean isNookDevice() {
        String brand = Build.BRAND;
        String manufacturer = System.getProperty("ro.nook.manufacturer");
        return ((brand != null && brand.equalsIgnoreCase("nook")) ||
                manufacturer != null && manufacturer.equalsIgnoreCase("nook"));
    }

    //todo rename the method
    public static FortumoStore initFortumoStore(Context context, final boolean checkInventory) {
        final FortumoStore[] storeToReturn = {null};
        final FortumoStore fortumoStore = new FortumoStore(context);
        if (fortumoStore.isBillingAvailable(context.getPackageName())) {
            final CountDownLatch latch = new CountDownLatch(1);
            fortumoStore.getInAppBillingService().startSetup(new IabHelper.OnIabSetupFinishedListener() {
                @Override
                public void onIabSetupFinished(IabResult setupResult) {
                    if (setupResult.isSuccess()) {
                        if (checkInventory) {
                            try {
                                final Inventory inventory = fortumoStore.getInAppBillingService().queryInventory(false, null, null);
                                if (!inventory.getAllPurchases().isEmpty()) {
                                    storeToReturn[0] = fortumoStore;
                                } else {
                                    if (isDebugLog()) {
                                        Log.d(TAG, "Purchases not found");
                                    }
                                }
                            } catch (IabException e) {
                                Log.e(TAG, "Error while requesting purchases", e);
                            }
                        } else {
                            storeToReturn[0] = fortumoStore;
                        }
                    }
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                Log.e(TAG, "Setup was interrupted", e);
            }
        }
        return storeToReturn[0];
    }

}
