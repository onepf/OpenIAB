package org.onepf.oms.appstore;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.vending.billing.IInAppBillingService;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.onepf.oms.Appstore;
import org.onepf.oms.AppstoreInAppBillingService;
import org.onepf.oms.DefaultAppstore;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.cafebazaarUtils.IabHelper;
import org.onepf.oms.util.CollectionUtils;
import org.onepf.oms.util.Logger;
import org.onepf.oms.util.Utils;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * CafeBazaar copies one to one the implementation of Google In App Billing v3. Therefore, this implementation of the store is based on the implementation for Google In App Billing done by Ruslan Sayfutdinov on 16.04.13
 * @author Sergio R. Lumley
 * @since 25.06.15.
 * @see <a href="https://cafebazaar.ir/developers/docs/iab/implementation/?l=en">Cafe Bazaar implementation details</a>
 */
public class CafeBazaar extends DefaultAppstore {

    public static final String VENDING_ACTION = "ir.cafebazaar.pardakht.InAppBillingService.BIND";
    public static final String ANDROID_INSTALLER = "com.farsitel.bazaar";

    private final Context context;
    private IabHelper mBillingService;
    private final String publicKey;

    // isDebugMode = true |-> always returns app installed via Cafe Bazaar store
    private final boolean isDebugMode = Boolean.parseBoolean("false"); // Avoid warnings by parsing its string value

    @Nullable
    private volatile Boolean billingAvailable = null; // undefined until isBillingAvailable() is called

    public CafeBazaar(Context context, String publicKey) {
        this.context = context;
        this.publicKey = publicKey;
    }

    @Override
    public boolean isPackageInstaller(final String packageName) {
        return isDebugMode || Utils.isPackageInstaller(context, ANDROID_INSTALLER);
    }

    @Override
    public boolean isBillingAvailable(final String packageName) {
        Logger.d("isBillingAvailable() packageName: ", packageName);
        if (billingAvailable != null) {
            return billingAvailable; // return previosly checked result
        }

        if (Utils.uiThread()) {
            throw new IllegalStateException("Must no be called from UI thread.");
        }

        if (!packageExists(context, ANDROID_INSTALLER)) {
            Logger.d("isBillingAvailable() Cafe Bazaar is not available.");
            // don't set billingAvailable variable in case Cafe Bazaar gets installed later
            return false;
        }

        final Intent intent = new Intent(VENDING_ACTION);
        intent.setPackage(ANDROID_INSTALLER);
        final List<ResolveInfo> infoList = context.getPackageManager().queryIntentServices(intent, 0);
        if (CollectionUtils.isEmpty(infoList)) {
            Logger.e("isBillingAvailable() billing service is not available, even though Cafe Bazaar application seems to be installed.");
            return false;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = new boolean[1];
        final ServiceConnection serviceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
                final IInAppBillingService mService = IInAppBillingService.Stub.asInterface(service);
                try {
                    final int response = mService.isBillingSupported(3, packageName, IabHelper.ITEM_TYPE_INAPP);
                    result[0] = response == IabHelper.BILLING_RESPONSE_RESULT_OK;
                } catch (RemoteException e) {
                    result[0] = false;
                    Logger.e("isBillingAvailable() RemoteException while setting up in-app billing", e);
                } finally {
                    latch.countDown();
                    context.unbindService(this);
                }
                Logger.d("isBillingAvailable() Cafe Bazaar result: ", result[0]);
            }

            public void onServiceDisconnected(ComponentName name) {/*do nothing*/}
        };
        if (context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Logger.e("isBillingAvailable() InterruptedException while setting up in-app billing", e);
            }
        } else {
            result[0] = false;
            Logger.e("isBillingAvailable() billing is not supported. Initialization error.");
        }
        return (billingAvailable = result[0]);
    }

    @Override
    public int getPackageVersion(final String packageName) {
        return Appstore.PACKAGE_VERSION_UNDEFINED;
    }

    @Override
    public String getAppstoreName() {
        return OpenIabHelper.NAME_CAFEBAZAAR;
    }

    @Nullable
    @Override
    public AppstoreInAppBillingService getInAppBillingService() {
        if (mBillingService == null) {
            mBillingService = new IabHelper(context, publicKey, this);
        }
        return mBillingService;
    }

    private boolean packageExists(@NotNull Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            Logger.d(packageName, " package was not found.");
            return false;
        }
    }
}
