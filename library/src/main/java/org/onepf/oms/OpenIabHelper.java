/*
 * Copyright 2012-2014 One Platform Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onepf.oms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.onepf.oms.appstore.AmazonAppstore;
import org.onepf.oms.appstore.GooglePlay;
import org.onepf.oms.appstore.NokiaStore;
import org.onepf.oms.appstore.OpenAppstore;
import org.onepf.oms.appstore.SamsungApps;
import org.onepf.oms.appstore.SamsungAppsBillingService;
import org.onepf.oms.appstore.googleUtils.IabException;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabHelper.OnIabSetupFinishedListener;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.appstore.googleUtils.Purchase;
import org.onepf.oms.appstore.googleUtils.Security;
import org.onepf.oms.util.Logger;
import org.onepf.oms.util.Utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.text.TextUtils;

import static org.onepf.oms.OpenIabHelper.Options.SEARCH_STRATEGY_INSTALLER;
import static org.onepf.oms.OpenIabHelper.Options.SEARCH_STRATEGY_INSTALLER_THEN_BEST_FIT;
import static org.onepf.oms.OpenIabHelper.Options.VERIFY_EVERYTHING;

/**
 * @author Boris Minaev, Oleg Orlov, Kirill Rozov
 * @since 16.04.13
 */
public class OpenIabHelper {

    private static final String BIND_INTENT = "org.onepf.oms.openappstore.BIND";

    /**
     * Used for all communication with Android services
     */
    private final Context context;
    private final PackageManager packageManager;

    /**
     * Necessary to initialize SamsungApps. For other stuff {@link #context} is used
     */
    private Activity activity;

    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * selected appstore
     */
    private Appstore mAppstore;

    /**
     * selected appstore billing service
     */
    private AppstoreInAppBillingService mAppstoreBillingService;

    private final Options options;

    public static final int SETUP_RESULT_NOT_STARTED = -1;
    public static final int SETUP_RESULT_SUCCESSFUL = 0;
    public static final int SETUP_RESULT_FAILED = 1;
    public static final int SETUP_DISPOSED = 2;
    public static final int SETUP_IN_PROGRESS = 3;

    @MagicConstant(intValues = {SETUP_DISPOSED, SETUP_IN_PROGRESS,
            SETUP_RESULT_FAILED, SETUP_RESULT_NOT_STARTED, SETUP_RESULT_SUCCESSFUL})
    private int setupState = SETUP_RESULT_NOT_STARTED;

    /**
     * SamsungApps requires {@link #handleActivityResult(int, int, Intent)} but it doesn't
     * work until setup is completed.
     */
    private volatile SamsungApps samsungInSetup;

    // Is an asynchronous operation in progress?
    // (only one at a time can be in progress)
    private volatile boolean mAsyncInProgress = false;

    // (for logging/debugging)
    // if mAsyncInProgress == true, what asynchronous operation is in progress?
    private volatile String mAsyncOperation = "";

    // Item types
    public static final String ITEM_TYPE_INAPP = "inapp";
    public static final String ITEM_TYPE_SUBS = "subs";

    // Billing response codes
    public static final int BILLING_RESPONSE_RESULT_OK = 0;
    public static final int BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3;
    public static final int BILLING_RESPONSE_RESULT_ERROR = 6;

    // Known wrappers
    public static final String NAME_GOOGLE = "com.google.play";
    public static final String NAME_AMAZON = "com.amazon.apps";
    public static final String NAME_SAMSUNG = "com.samsung.apps";
    public static final String NAME_NOKIA = "com.nokia.nstore";

    // Knows open stores
    public static final String NAME_YANDEX = "com.yandex.store";
    public static final String NAME_APPLAND = "Appland";
    public static final String NAME_SLIDEME = "SlideME";
    public static final String NAME_APTOIDE = "cm.aptoide.pt";

    private static interface AppstoreFactory{
        Appstore get();
    }

    private final Map<String, String> appstorePackageMap = new HashMap<String, String>();
    private final Map<String, AppstoreFactory> appstoreFactoryMap = new HashMap<String, AppstoreFactory>();

    {
        // Known packages for open stores
        appstorePackageMap.put("com.yandex.store", NAME_YANDEX);
        appstorePackageMap.put("cm.aptoide.pt", NAME_APTOIDE);


        appstorePackageMap.put(GooglePlay.ANDROID_INSTALLER, NAME_GOOGLE);
        appstoreFactoryMap.put(NAME_GOOGLE, new AppstoreFactory() {
            @Override
            public Appstore get() {
                final String googleKey = options.getStoreKeys().get(NAME_GOOGLE);
                return new GooglePlay(context, googleKey);
            }
        });

        appstorePackageMap.put(AmazonAppstore.AMAZON_INSTALLER, NAME_AMAZON);
        appstoreFactoryMap.put(NAME_AMAZON, new AppstoreFactory() {
            @Override
            public Appstore get() {
                return new AmazonAppstore(context);
            }
        });

        appstorePackageMap.put(SamsungApps.SAMSUNG_INSTALLER, NAME_SAMSUNG);
        appstoreFactoryMap.put(NAME_SAMSUNG, new AppstoreFactory() {
            @Override
            public Appstore get() {
                return new SamsungApps(activity, options);
            }
        });

        appstorePackageMap.put(NokiaStore.NOKIA_INSTALLER, NAME_NOKIA);
        appstoreFactoryMap.put(NAME_NOKIA, new AppstoreFactory() {
            @Override
            public Appstore get() {
                return new NokiaStore(context);
            }
        });
    }


    /**
     * @param sku       - application inner SKU
     * @param storeSku  - shouldn't duplicate already mapped values
     * @param storeName - @see {@link IOpenAppstore#getAppstoreName()} or {@link #NAME_AMAZON}, {@link #NAME_GOOGLE}
     * @throws java.lang.IllegalArgumentException If one of arguments is empty or null string.
     * @deprecated Use {@link org.onepf.oms.SkuManager#mapSku(String, String, String)}
     * <p/>
     * Map sku and storeSku for particular store.
     * <p/>
     * The best approach is to use SKU that unique in universe like <code>com.companyname.application.item</code>.
     * Such SKU fit most of stores so it doesn't need to be mapped.
     * <p/>
     * If best approach is not applicable use application inner SKU in code (usually it is SKU for Google Play)
     * and map SKU from other stores using this method. OpenIAB will map SKU in both directions,
     * so you can use only your inner SKU
     */
    public static void mapSku(String sku, String storeName, String storeSku) {
        SkuManager.getInstance().mapSku(sku, storeName, storeSku);
    }

    /**
     * @param appstoreName - Name of store.
     * @param sku          - inner SKU
     * @return SKU used in store for specified inner SKU
     * @see org.onepf.oms.SkuManager#mapSku(String, String, String)
     * @deprecated Use {@link org.onepf.oms.SkuManager#getStoreSku(String, String)}
     * <p/>
     * Return previously mapped store SKU for specified inner SKU
     */
    public static String getStoreSku(final String appstoreName, String sku) {
        return SkuManager.getInstance().getStoreSku(appstoreName, sku);
    }

    /**
     * @see org.onepf.oms.SkuManager#mapSku(String, String, String)
     * @deprecated Use {@link org.onepf.oms.SkuManager#getSku(String, String)}
     * <p/>
     * Return mapped application inner SKU using store name and store SKU.
     */
    public static String getSku(final String appstoreName, String storeSku) {
        return SkuManager.getInstance().getSku(appstoreName, storeSku);
    }

    /**
     * @param appstoreName for example {@link OpenIabHelper#NAME_AMAZON}
     * @return list of skus those have mappings for specified appstore
     * @deprecated Use {@link org.onepf.oms.SkuManager#getAllStoreSkus(String)}
     */
    public static List<String> getAllStoreSkus(final String appstoreName) {
        final Collection<String> allStoreSkus =
                SkuManager.getInstance().getAllStoreSkus(appstoreName);
        return allStoreSkus == null ? Collections.<String>emptyList()
                : new ArrayList<String>(allStoreSkus);
    }

    /**
     * @param storeKeys - see {@link Options#storeKeys}
     * @param context   - if you want to support Samsung Apps you must pass an Activity, in other cases any context is acceptable
     * @deprecated Use {@link org.onepf.oms.OpenIabHelper#OpenIabHelper(android.content.Context, org.onepf.oms.OpenIabHelper.Options)}
     * Will be removed in 1.0 release.
     * <p/>
     * <p/>
     * Simple constructor for OpenIabHelper.
     * <p>See {@link OpenIabHelper#OpenIabHelper(Context, Options)} for details
     */
    public OpenIabHelper(Context context, Map<String, String> storeKeys) {
        this(context,
                new Options.Builder()
                        .addStoreKeys(storeKeys)
                        .build()
        );
    }

    /**
     * @param storeKeys       - see {@link org.onepf.oms.OpenIabHelper.Options#getStoreKeys()}
     * @param preferredStores - see {@link org.onepf.oms.OpenIabHelper.Options#getPreferredStoreNames()}
     * @param context         - if you want to support Samsung Apps you must pass an Activity, in other cases any context is acceptable
     * @deprecated Use {@link org.onepf.oms.OpenIabHelper#OpenIabHelper(android.content.Context, org.onepf.oms.OpenIabHelper.Options)}
     * Will be removed in 1.0 release.
     * <p/>
     * <p/>
     * Simple constructor for OpenIabHelper.
     * <p>See {@link OpenIabHelper#OpenIabHelper(Context, Options)} for details
     */
    public OpenIabHelper(Context context, Map<String, String> storeKeys, String[] preferredStores) {
        this(context,
                new Options.Builder()
                        .addStoreKeys(storeKeys)
                        .addPreferredStoreName(preferredStores)
                        .build()
        );
    }

    /**
     * @param storeKeys       - see {@link org.onepf.oms.OpenIabHelper.Options#getStoreKeys()}
     * @param preferredStores - see {@link org.onepf.oms.OpenIabHelper.Options#getPreferredStoreNames()}
     * @param availableStores - see {@link org.onepf.oms.OpenIabHelper.Options#getAvailableStores()}
     * @param context         - if you want to support Samsung Apps you must pass an Activity, in other cases any context is acceptable
     * @deprecated Use {@link org.onepf.oms.OpenIabHelper#OpenIabHelper(android.content.Context, org.onepf.oms.OpenIabHelper.Options)}
     * Will be removed in 1.0 release.
     * <p/>
     * Simple constructor for OpenIabHelper.
     * <p>See {@link OpenIabHelper#OpenIabHelper(Context, Options)} for details
     */
    public OpenIabHelper(Context context, Map<String, String> storeKeys, String[] preferredStores, Appstore[] availableStores) {
        this(context,
                new Options.Builder()
                        .addStoreKeys(storeKeys)
                        .addPreferredStoreName(preferredStores)
                        .addAvailableStores(availableStores)
                        .build()
        );
    }

    /**
     * Before start ensure you already have <li>
     * - permission <code>org.onepf.openiab.permission.BILLING</code> in your AndroidManifest.xml<li>
     * - publicKey for store you decided to work with (you can find it in Developer Console of your store)<li>
     * - map SKUs for your store if they differs using {@link #mapSku(String, String, String)}</li>
     * <p/>
     * <p/>
     * You can specify publicKeys for stores (excluding Amazon and SamsungApps those don't use
     * verification based on RSA keys). See {@link Options#storeKeys} for details
     * <p/>
     * By default verification will be performed for receipt from every store. To aviod verification
     * exception OpenIAB doesn't connect to store that key is not specified for
     * <p/>
     * If you don't want to put publicKey in code and verify receipt remotely, you need to set
     * {@link Options#verifyMode} to {@link Options#VERIFY_SKIP}.
     * To make OpenIAB connect even to stores key is not specified for, use {@link Options#VERIFY_ONLY_KNOWN}
     * <p/>
     * {@link org.onepf.oms.OpenIabHelper.Options#getPreferredStoreNames()} is useful option when you test your app on device with multiple
     * stores installed. Specify store name you want to work with here and it would be selected if you
     * install application using adb.
     *
     * @param options - specify all necessary options
     * @param context - if you want to support Samsung Apps you must pass an Activity, in other cases any context is acceptable
     */
    public OpenIabHelper(Context context, Options options) {
        this.context = context.getApplicationContext();
        packageManager = context.getPackageManager();
        this.options = options;
        if (context instanceof Activity) {
            this.activity = (Activity) context;
        }

        checkOptions();
        Logger.init();
    }

    private ExecutorService setupExecutorService;

    /**
     * Discover all available stores and select the best billing service.
     * <p/>
     * Should be called from UI thread
     *
     * @param listener - called when setup is completed
     */
    public void startSetup(@NotNull final OnIabSetupFinishedListener listener) {
        //noinspection ConstantConditions
        if (listener == null) {
            throw new IllegalArgumentException("Setup listener must be not null!");
        }
        if (setupState != SETUP_RESULT_NOT_STARTED && setupState != SETUP_RESULT_FAILED) {
            throw new IllegalStateException("Couldn't be set up. Current state: " + setupStateToString(setupState));
        }
        Logger.init();
        setupState = SETUP_IN_PROGRESS;
        setupExecutorService = Executors.newSingleThreadExecutor();

        final int storeSearchStrategy = options.getStoreSearchStrategy();
        final String packageName = context.getPackageName();
        final String packageInstaller = packageManager.getInstallerPackageName(packageName);
        final boolean packageInstallerSet = !TextUtils.isEmpty(packageInstaller);

        if (storeSearchStrategy == SEARCH_STRATEGY_INSTALLER) {
            // Check only package installer
            if (packageInstallerSet) {
                // Check without fallback
                setupForPackage(listener, packageInstaller, false);
            } else {
                // Package installer isn't available
                finishSetup(listener);
            }
        } else if (storeSearchStrategy == SEARCH_STRATEGY_INSTALLER_THEN_BEST_FIT) {
            // Check package installer then all others
            if (packageInstallerSet) {
                // Check with fallback
                setupForPackage(listener, packageInstaller, true);
            } else {
                // Check other stores
                setup(listener);
            }
        } else {
            setup(listener);
        }
    }

    private void setupForPackage(final OnIabSetupFinishedListener listener,
                                 final String packageInstaller,
                                 final boolean withFallback) {
        if (!Utils.packageInstalled(context, packageInstaller)) {
            // Package installer is no longer available
            if (withFallback) {
                // Check other stores
                setup(listener);
            } else {
                finishSetup(listener);
            }
            return;
        }

        Appstore appstore = null;
        if (appstorePackageMap.containsKey(packageInstaller)) {
            // Package installer is a known appstore
            final String appstoreName = appstorePackageMap.get(packageInstaller);
            if (options.getAvailableStores().isEmpty()) {
                if (appstoreFactoryMap.containsKey(appstoreName)) {
                    appstore = appstoreFactoryMap.get(appstoreName).get();
                }
            } else {
                // Developer explicitly specified available stores
                appstore = options.getAvailableStoreWithName(appstoreName);
                if (appstore == null) {
                    // Store is known but isn't available
                    finishSetup(listener);
                    return;
                }
            }
        }

        if (appstore != null) {
            // Package installer found
            checkBillingAndFinish(listener, appstore);
            return;
        }

        // Look among open stores
        Intent bindServiceIntent = null;
        for (final ServiceInfo serviceInfo : queryOpenStoreServices()) {
            if (TextUtils.equals(serviceInfo.packageName, packageInstaller)) {
                bindServiceIntent = getBindServiceIntent(serviceInfo);
                break;
            }
        }
        if (bindServiceIntent == null) {
            // Package installer not found
            if (withFallback) {
                // Check other stores
                setup(listener);
            } else {
                finishSetup(listener);
            }
            return;
        }

        if (!context.bindService(bindServiceIntent, new ServiceConnection() {
            @Override
            public void onServiceConnected(final ComponentName name, final IBinder service) {
                Appstore appstore = null;
                try {
                    final Appstore openAppstore = getOpenAppstore(name, service, this);
                    if (openAppstore != null) {
                        // Found open store
                        final String openStoreName = openAppstore.getAppstoreName();
                        if (options.getAvailableStores().isEmpty()) {
                            appstore = openAppstore;
                        } else {
                            // Developer explicitly specified available stores
                            appstore = options.getAvailableStoreWithName(openStoreName);
                        }
                    }
                } catch (RemoteException exception) {
                    Logger.e("setupForPackage() Error binding to open store service : ", exception);
                }
                if (appstore == null && withFallback) {
                    setup(listener);
                } else {
                    checkBillingAndFinish(listener, appstore);
                }
            }

            @Override
            public void onServiceDisconnected(final ComponentName name) {}
        }, Context.BIND_AUTO_CREATE)) {
            // Can't bind to open store service
            Logger.e("setupForPackage() Error binding to open store service");
            if (withFallback) {
                setup(listener);
            } else {
                finishSetupWithError(listener);
            }
        }
    }

    private void setup(final OnIabSetupFinishedListener listener) {
        // List of wrappers to check
        final Set<Appstore> appstoresToCheck = new LinkedHashSet<Appstore>();

        final Set<Appstore> availableStores;
        if (!(availableStores = options.getAvailableStores()).isEmpty()) {
            // Use only stores specified explicitly
            for (final String name : options.getPreferredStoreNames()) {
                // Add available stored according to preferred stores priority
                final Appstore appstore = options.getAvailableStoreWithName(name);
                if (appstore != null) {
                    appstoresToCheck.add(appstore);
                }
            }
            appstoresToCheck.addAll(availableStores);
            checkBillingAndFinish(listener, appstoresToCheck);
        } else {
            discoverOpenStores(new OpenStoresDiscoveredListener() {
                @Override
                public void openStoresDiscovered(@NotNull final List<Appstore> appstores) {
                    final List<Appstore> allAvailableAppsotres = new ArrayList<Appstore>(appstores);
                    // Add all available wrappers
                    for (final String appstorePackage : appstorePackageMap.keySet()) {
                        final String name = appstorePackageMap.get(appstorePackage);
                        if (!TextUtils.isEmpty(name)
                                && appstoreFactoryMap.containsKey(name)
                                && Utils.packageInstalled(context, appstorePackage)) {
                            allAvailableAppsotres.add(appstoreFactoryMap.get(name).get());
                        }
                    }
                    // Add available stored according to preferred stores priority
                    for (final String name : options.getPreferredStoreNames()) {
                        for (final Appstore appstore : allAvailableAppsotres) {
                            if (TextUtils.equals(appstore.getAppstoreName(), name)) {
                                appstoresToCheck.add(appstore);
                                break;
                            }
                        }
                    }
                    // Add everything else
                    appstoresToCheck.addAll(allAvailableAppsotres);
                    checkBillingAndFinish(listener, appstoresToCheck);
                }
            });
        }
    }

    @Nullable
    private OpenAppstore getOpenAppstore(final ComponentName name,
                                         final IBinder service,
                                         final ServiceConnection serviceConnection)
            throws RemoteException {
        final IOpenAppstore openAppstoreService = IOpenAppstore.Stub.asInterface(service);
        final String appstoreName = openAppstoreService.getAppstoreName();
        final Intent billingIntent = openAppstoreService.getBillingServiceIntent();
        final int verifyMode = options.getVerifyMode();
        final String publicKey = verifyMode == Options.VERIFY_SKIP
                ? null
                : options.getStoreKeys().get(appstoreName);

        if (TextUtils.isEmpty(appstoreName)) { // no name - no service
            Logger.d("getOpenAppstore() Appstore doesn't have name. Skipped. ComponentName: ", name);
        } else if (billingIntent == null) {
            Logger.d("getOpenAppstore(): billing is not supported by store: ", name);
        } else if (verifyMode == Options.VERIFY_EVERYTHING && TextUtils.isEmpty(publicKey)) {
            // don't connect to OpenStore if no key provided and verification is strict
            Logger.e("getOpenAppstore() verification is required but publicKey is not provided: ", name);
        } else {
            final OpenAppstore openAppstore =
                    new OpenAppstore(context, appstoreName, openAppstoreService, billingIntent, publicKey, serviceConnection);
            openAppstore.componentName = name;
            return openAppstore;
        }

        return null;
    }

    private Intent getBindServiceIntent(final ServiceInfo serviceInfo) {
        final Intent bindServiceIntent = new Intent(BIND_INTENT);
        bindServiceIntent.setClassName(serviceInfo.packageName, serviceInfo.name);
        return bindServiceIntent;
    }

    private void checkBillingAndFinish(@NotNull final OnIabSetupFinishedListener listener,
                                       @Nullable final Appstore appstore) {
        if (appstore == null) {
            finishSetup(listener);
        } else {
            checkBillingAndFinish(listener, Arrays.asList(appstore));
        }
    }

    private void checkBillingAndFinish(@NotNull final OnIabSetupFinishedListener listener,
                                       @NotNull final Collection<Appstore> appstores) {
        if (setupState != SETUP_IN_PROGRESS) {
            throw new IllegalStateException("Can't check billing. Current state: " + setupStateToString(setupState));
        }

        final String packageName = context.getPackageName();
        if (appstores.isEmpty()) {
            finishSetup(listener);
            return;
        }

        final Runnable checkStoresRunnable;
        if (options.isCheckInventory()) {
            checkStoresRunnable = new Runnable() {
                @Override
                public void run() {
                    final List<Appstore> availableAppstores = new ArrayList<Appstore>();
                    for (final Appstore appstore : appstores) {
                        if (appstore.isBillingAvailable(packageName) && versionOk(appstore)) {
                            availableAppstores.add(appstore);
                        }
                    }
                    Appstore checkedAppstore = checkInventory(new HashSet<Appstore>(availableAppstores));
                    final Appstore foundAppstore;
                    if (checkedAppstore == null) {
                        foundAppstore = availableAppstores.isEmpty() ? null : availableAppstores.get(0);
                    } else {
                        foundAppstore = checkedAppstore;
                    }
                    final OnIabSetupFinishedListener listenerWrapper = new OnIabSetupFinishedListener() {
                        @Override
                        public void onIabSetupFinished(final IabResult result) {
                            // Dispose of all initialized open appstores
                            final Collection<Appstore> appstoresToDispose = new ArrayList<Appstore>();
                            if (foundAppstore != null) {
                                appstoresToDispose.remove(foundAppstore);
                            }
                            dispose(appstoresToDispose);
                            listener.onIabSetupFinished(result);
                        }
                    };
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            finishSetup(listenerWrapper, foundAppstore);
                        }
                    });
                }
            };
        } else {
            checkStoresRunnable = new Runnable() {
                @Override
                public void run() {
                    Appstore checkedAppstore = null;
                    for (final Appstore appstore : appstores) {
                        if (appstore.isBillingAvailable(packageName) && versionOk(appstore)) {
                            checkedAppstore = appstore;
                            break;
                        }
                    }
                    final Appstore foundAppstore = checkedAppstore;
                    final OnIabSetupFinishedListener listenerWrapper = new OnIabSetupFinishedListener() {
                        @Override
                        public void onIabSetupFinished(final IabResult result) {
                            // Dispose of all initialized open appstores
                            final Collection<Appstore> appstoresToDispose = new ArrayList<Appstore>();
                            if (foundAppstore != null) {
                                appstoresToDispose.remove(foundAppstore);
                            }
                            dispose(appstoresToDispose);
                            if (foundAppstore != null) {
                                foundAppstore.getInAppBillingService().startSetup(listener);
                            } else {
                                listener.onIabSetupFinished(result);
                            }
                        }
                    };
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            finishSetup(listenerWrapper, foundAppstore);
                        }
                    });
                }
            };
        }

        setupExecutorService.execute(checkStoresRunnable);
    }

    private void dispose(@NotNull final Collection<Appstore> appstores) {
        for (final Appstore appstore : appstores) {
            final AppstoreInAppBillingService billingService = appstore.getInAppBillingService();
            billingService.dispose();
        }
    }

    private boolean versionOk(@NotNull final Appstore appstore) {
        final String packageName = context.getPackageName();
        int versionCode = Appstore.PACKAGE_VERSION_UNDEFINED;
        try {
            versionCode = context.getPackageManager().getPackageInfo(packageName, 0).versionCode;
        } catch (NameNotFoundException ignore) {}
        // TODO investigate getPackageVersion() behaviour
//        return appstore.getPackageVersion(packageName) >= versionCode;
        return true;
    }

    private void finishSetupWithError(@NotNull final OnIabSetupFinishedListener listener) {
        finishSetupWithError(listener, null);
    }

    private void finishSetupWithError(@NotNull final OnIabSetupFinishedListener listener,
                                      @Nullable final Exception exception) {
        Logger.e("finishSetupWithError() error occurred during setup", exception == null ? "" : " : " + exception);
        finishSetup(listener, new IabResult(BILLING_RESPONSE_RESULT_ERROR, "Error occured, setup failed"), null);
    }

    private void finishSetup(@NotNull final OnIabSetupFinishedListener listener) {
        finishSetup(listener, null);
    }

    private void finishSetup(@NotNull final OnIabSetupFinishedListener listener,
                             @Nullable final Appstore appstore) {
        final IabResult iabResult = appstore == null
                ? new IabResult(BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE, "No suitable appstore was found")
                : new IabResult(BILLING_RESPONSE_RESULT_OK, "Setup ok");
        finishSetup(listener, iabResult, appstore);
    }

    private void finishSetup(@NotNull final OnIabSetupFinishedListener listener,
                             @NotNull final IabResult iabResult,
                             @Nullable final Appstore appstore) {
        if (!Utils.uiThread()) {
            throw new IllegalStateException("Must be called from UI thread.");
        }
        if (setupState != SETUP_IN_PROGRESS) {
            throw new IllegalStateException("Setup is not started or already finished.");
        }

        final boolean setUpSuccessful = iabResult.isSuccess();
        setupState = setUpSuccessful ? SETUP_RESULT_SUCCESSFUL : SETUP_RESULT_FAILED;
        activity = null;
        setupExecutorService.shutdownNow();
        setupExecutorService = null;
        if (setUpSuccessful) {
            if (appstore == null) {
                throw new IllegalStateException("Appstore can't be null if setup is successful");
            }
            mAppstore = appstore;
            mAppstoreBillingService = appstore.getInAppBillingService();
        }
        Logger.dWithTimeFromUp("finishSetup() === SETUP DONE === result: ", iabResult, " Appstore: ", appstore);
        listener.onIabSetupFinished(iabResult);
    }

    @MagicConstant(intValues = {SETUP_DISPOSED, SETUP_IN_PROGRESS,
            SETUP_RESULT_FAILED, SETUP_RESULT_NOT_STARTED, SETUP_RESULT_SUCCESSFUL})
    public int getSetupState() {
        return setupState;
    }

    public @Nullable List<Appstore> discoverOpenStores() {
        if (Utils.uiThread()) {
            throw new IllegalStateException("Must not be called from UI thread");
        }

        final List<Appstore> openAppstores = new ArrayList<Appstore>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        discoverOpenStores(new OpenStoresDiscoveredListener() {
            @Override
            public void openStoresDiscovered(@NotNull final List<Appstore> appstores) {
                openAppstores.addAll(appstores);
                countDownLatch.notify();
            }
        });
        try {
            countDownLatch.await();
        }catch (InterruptedException e) {
            return null;
        }
        return openAppstores;
    }

    public void discoverOpenStores(@NotNull final OpenStoresDiscoveredListener listener) {
        final List<ServiceInfo> serviceInfos = queryOpenStoreServices();
        final Queue<Intent> bindServiceIntents = new LinkedList<Intent>();
        for (final ServiceInfo serviceInfo : serviceInfos) {
            bindServiceIntents.add(getBindServiceIntent(serviceInfo));
        }

        discoverOpenStores(listener, bindServiceIntents, new ArrayList<Appstore>());
    }

    private void discoverOpenStores(@NotNull final OpenStoresDiscoveredListener listener,
                                    @NotNull final Queue<Intent> bindServiceIntents,
                                    @NotNull final List<Appstore> appstores) {
        while (!bindServiceIntents.isEmpty()) {
            final Intent intent = bindServiceIntents.poll();
            final ServiceConnection serviceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(final ComponentName name, final IBinder service) {
                    Appstore openAppstore = null;
                    try {
                        openAppstore = getOpenAppstore(name, service, this);
                    } catch (RemoteException exception) {
                        Logger.w("onServiceConnected() Error creating appsotre: ", exception);
                    }
                    if (openAppstore != null) {
                        appstores.add(openAppstore);
                    }
                    discoverOpenStores(listener, bindServiceIntents, appstores);
                }

                @Override
                public void onServiceDisconnected(final ComponentName name) {}
            };

            if (context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)) {
                // Wait for open store service
                return;
            } else {
                // TODO It seems serviceConnection still might be called in this point hopefully this will help
                context.unbindService(serviceConnection);
                Logger.e("discoverOpenStores() Couldn't connect to open store: " + intent);
            }
        }

        listener.openStoresDiscovered(Collections.unmodifiableList(appstores));
    }

    @Deprecated
    /**
     * Use {@link #discoverOpenStores(OpenStoresDiscoveredListener)} or {@link #discoverOpenStores()} instead.
     */
    public static List<Appstore> discoverOpenStores(final Context context, final List<Appstore> dest, final Options options) {
        throw new UnsupportedOperationException("This action is no longer supported.");
    }

    private @NotNull List<ServiceInfo> queryOpenStoreServices() {
        final Intent intentAppstoreServices = new Intent(BIND_INTENT);
        final PackageManager packageManager = context.getPackageManager();
        final List<ResolveInfo> resolveInfos = packageManager.queryIntentServices(intentAppstoreServices, 0);
        final List<ServiceInfo> serviceInfos = new ArrayList<ServiceInfo>();
        for (final ResolveInfo resolveInfo : resolveInfos) {
            serviceInfos.add(resolveInfo.serviceInfo);
        }
        return Collections.unmodifiableList(serviceInfos);
    }

    /**
     * Must be called after setup is finished. See {@link #startSetup(OnIabSetupFinishedListener)}
     *
     * @return <code>null</code> if no appstore connected, otherwise name of Appstore OpenIAB has connected to.
     */
    public @Nullable String getConnectedAppstoreName() {
        if (mAppstore == null) return null;
        return mAppstore.getAppstoreName();
    }

    /**
     * Check options are valid
     */
    public void checkOptions() {
        checkGoogle();
        checkSamsung();
        checkNokia();
    }

    private void checkNokia() {
        final boolean hasPermission = Utils.hasRequestedPermission(context, NokiaStore.NOKIA_BILLING_PERMISSION);
        Logger.d("checkNokia() has permission : ", hasPermission);
        if (hasPermission) {
            return;
        }
        if (options.getAvailableStoreWithName(NAME_NOKIA) != null
                || options.getPreferredStoreNames().contains(NAME_NOKIA)) {
            throw new IllegalStateException("Nokia permission \"" +
                    NokiaStore.NOKIA_BILLING_PERMISSION + "\" NOT REQUESTED");
        }
        Logger.d("checkNokia() ignoring Nokia wrapper");
        appstoreFactoryMap.remove(NAME_NOKIA);
    }

    private void checkSamsung() {
        Logger.d("checkSamsung() activity is : ", activity);
        if (activity != null) {
            return;
        }
        if (options.getAvailableStoreWithName(NAME_SAMSUNG) != null
                || options.getPreferredStoreNames().contains(NAME_SAMSUNG)) {
            // Unfortunately, SamsungApps requires to launch their own "Certification Activity"
            // in order to connect to billing service. So it's also needed for OpenIAB.
            //
            // Instance of Activity needs to be passed to OpenIAB constructor to launch
            // Samsung Certification Activity.
            // Activity also need to pass activityResult to OpenIABHelper.handleActivityResult()
            throw new IllegalArgumentException("You must supply Activity object as context in order to use " + NAME_SAMSUNG + " store");
        }
        Logger.d("checkSamsung() ignoring Samsung wrapper");
        appstoreFactoryMap.remove(NAME_SAMSUNG);
    }

    private void checkGoogle() {
        final boolean googleKeyProvided = options.getStoreKeys().containsKey(NAME_GOOGLE);
        Logger.d("checkGoogle() google key available : ", googleKeyProvided);
        if (googleKeyProvided) {
            return;
        }

        final boolean googleRequired = options.getAvailableStoreWithName(NAME_GOOGLE) != null
                || options.getPreferredStoreNames().contains(NAME_GOOGLE);
        if (googleRequired && options.getVerifyMode() == VERIFY_EVERYTHING) {
            throw new IllegalStateException("You must supply Google verification key");
        }
        Logger.d("checkGoogle() ignoring GooglePlay wrapper", googleKeyProvided);
        appstoreFactoryMap.remove(NAME_GOOGLE);
    }

    /**
     * Connects to Billing Service of each store and request list of user purchases (inventory).
     * Can be used as a factor when looking for best fitting store.
     *
     * @param availableStores - list of stores to check
     * @return first found store with not empty inventory, null otherwise.
     */
    private @Nullable Appstore checkInventory(@NotNull final Set<Appstore> availableStores) {
        if (Utils.uiThread()) {
            throw new IllegalStateException("Must not be called from UI thread");
        }

        final Semaphore inventorySemaphore = new Semaphore(0);
        final ExecutorService inventoryExecutor = Executors.newSingleThreadExecutor();
        final Appstore[] inventoryAppstore = new Appstore[1];

        for (final Appstore appstore : availableStores) {
            final AppstoreInAppBillingService billingService = appstore.getInAppBillingService();
            final OnIabSetupFinishedListener listener = new OnIabSetupFinishedListener() {
                @Override
                public void onIabSetupFinished(final IabResult result) {
                    if (!result.isSuccess()) {
                        inventorySemaphore.release();
                        return;
                    }
                    // queryInventory() is a blocking call and must be call from background
                    final Runnable checkInventoryRunnable = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                final Inventory inventory = billingService.queryInventory(false, null, null);
                                if (inventory != null && !inventory.getAllPurchases().isEmpty()) {
                                    inventoryAppstore[0] = appstore;
                                    Logger.dWithTimeFromUp("inventoryCheck() in ",
                                            appstore.getAppstoreName(), " found: ",
                                            inventory.getAllPurchases().size(), " purchases");
                                }
                            } catch (IabException exception) {
                                Logger.e("inventoryCheck() failed for ", appstore.getAppstoreName() + " : ", exception);
                            }
                            inventorySemaphore.release();
                        }
                    };
                    inventoryExecutor.execute(checkInventoryRunnable);
                }
            };
            // startSetup() must be called form UI thread
            handler.post(new Runnable() {
                @Override
                public void run() {
                    billingService.startSetup(listener);
                }
            });

            try {
                inventorySemaphore.acquire();
            } catch (InterruptedException exception) {
                Logger.e("checkInventory() Error during inventory check: ",exception);
                return null;
            }
            if (inventoryAppstore[0] != null) {
                inventoryExecutor.shutdownNow();
                return inventoryAppstore[0];
            }
        }

        inventoryExecutor.shutdownNow();
        return null;
    }

    public void dispose() {
        Logger.d("Disposing.");
        if (mAppstoreBillingService != null) {
            mAppstoreBillingService.dispose();
        }
        mAppstore = null;
        mAppstoreBillingService = null;
        activity = null;
        setupState = SETUP_DISPOSED;
    }

    public boolean subscriptionsSupported() {
        checkSetupDone("subscriptionsSupported");
        return mAppstoreBillingService.subscriptionsSupported();
    }

    public void launchPurchaseFlow(Activity act, String sku, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener) {
        launchPurchaseFlow(act, sku, requestCode, listener, "");
    }

    public void launchPurchaseFlow(Activity act, String sku, int requestCode,
                                   IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        launchPurchaseFlow(act, sku, ITEM_TYPE_INAPP, requestCode, listener, extraData);
    }

    public void launchSubscriptionPurchaseFlow(Activity act, String sku, int requestCode,
                                               IabHelper.OnIabPurchaseFinishedListener listener) {
        launchSubscriptionPurchaseFlow(act, sku, requestCode, listener, "");
    }

    public void launchSubscriptionPurchaseFlow(Activity act, String sku, int requestCode,
                                               IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        launchPurchaseFlow(act, sku, ITEM_TYPE_SUBS, requestCode, listener, extraData);
    }

    public void launchPurchaseFlow(Activity act, String sku, String itemType, int requestCode,
                                   IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        checkSetupDone("launchPurchaseFlow");
        mAppstoreBillingService.launchPurchaseFlow(act,
                SkuManager.getInstance().getStoreSku(mAppstore.getAppstoreName(), sku),
                itemType,
                requestCode,
                listener,
                extraData);
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        Logger.dWithTimeFromUp("handleActivityResult() requestCode: ", requestCode, " resultCode: ", resultCode, " data: ", data);
        if (requestCode == options.samsungCertificationRequestCode && samsungInSetup != null) {
            return samsungInSetup.getInAppBillingService().handleActivityResult(requestCode, resultCode, data);
        }
        if (setupState != SETUP_RESULT_SUCCESSFUL) {
            Logger.d("handleActivityResult() setup is not done. requestCode: ", requestCode, " resultCode: ", resultCode, " data: ", data);
            return false;
        }
        return mAppstoreBillingService.handleActivityResult(requestCode, resultCode, data);
    }

    /**
     * See {@link #queryInventory(boolean, List, List)} for details
     */
    public @Nullable Inventory queryInventory(final boolean querySkuDetails,
                                              @Nullable final List<String> moreSkus)
            throws IabException {
        return queryInventory(querySkuDetails, moreSkus, null);
    }

    /**
     * Queries the inventory. This will query all owned items from the server, as well as
     * information on additional skus, if specified. This method may block or take long to execute.
     * Do not call from a UI thread. For that, use the non-blocking version {@link #queryInventoryAsync(boolean, java.util.List, java.util.List, org.onepf.oms.appstore.googleUtils.IabHelper.QueryInventoryFinishedListener)}.
     *
     * @param querySkuDetails if true, SKU details (price, description, etc) will be queried as well
     *                        as purchase information.
     * @param moreItemSkus    additional PRODUCT skus to query information on, regardless of ownership.
     *                        Ignored if null or if querySkuDetails is false.
     * @param moreSubsSkus    additional SUBSCRIPTIONS skus to query information on, regardless of ownership.
     *                        Ignored if null or if querySkuDetails is false.
     * @throws IabException if a problem occurs while refreshing the inventory.
     */
    public @Nullable Inventory queryInventory(final boolean querySkuDetails,
                                              @Nullable final List<String> moreItemSkus,
                                              @Nullable final List<String> moreSubsSkus)
            throws IabException {
        if (Utils.uiThread()) {
            throw new IllegalStateException("Must not be called from UI thread");
        }
        checkSetupDone("queryInventory");

        final List<String> moreItemStoreSkus;
        final SkuManager skuManager = SkuManager.getInstance();
        if (moreItemSkus != null) {
            moreItemStoreSkus = new ArrayList<String>(moreItemSkus.size());
            for (String sku : moreItemSkus) {
                moreItemStoreSkus.add(skuManager.getStoreSku(mAppstore.getAppstoreName(), sku));
            }
        } else {
            moreItemStoreSkus = null;
        }

        final List<String> moreSubsStoreSkus;
        if (moreSubsSkus != null) {
            moreSubsStoreSkus = new ArrayList<String>(moreSubsSkus.size());
            for (String sku : moreSubsSkus) {
                moreSubsStoreSkus.add(skuManager.getStoreSku(mAppstore.getAppstoreName(), sku));
            }
        } else {
            moreSubsStoreSkus = null;
        }
        return mAppstoreBillingService.queryInventory(querySkuDetails, moreItemStoreSkus, moreSubsStoreSkus);
    }

    /**
     * @see #queryInventoryAsync(boolean, List, List, IabHelper.QueryInventoryFinishedListener)
     */
    public void queryInventoryAsync(@NotNull final IabHelper.QueryInventoryFinishedListener listener) {
        queryInventoryAsync(true, listener);
    }

    /**
     * @see #queryInventoryAsync(boolean, List, List, IabHelper.QueryInventoryFinishedListener)
     */
    public void queryInventoryAsync(final boolean querySkuDetails,
                                    @NotNull IabHelper.QueryInventoryFinishedListener listener) {
        queryInventoryAsync(querySkuDetails, null, listener);
    }

    /**
     * @see #queryInventoryAsync(boolean, List, List, IabHelper.QueryInventoryFinishedListener)
     */
    public void queryInventoryAsync(final boolean querySkuDetails,
                                    @Nullable final List<String> moreSkus,
                                    @NotNull final IabHelper.QueryInventoryFinishedListener listener) {
        queryInventoryAsync(querySkuDetails, moreSkus, null, listener);
    }

    /**
     * Queries the inventory. This will query all owned items from the server, as well as
     * information on additional skus, if specified. This method may block or take long to execute.
     *
     * @param querySkuDetails if true, SKU details (price, description, etc) will be queried as well
     *                        as purchase information.
     * @param moreItemSkus    additional PRODUCT skus to query information on, regardless of ownership.
     *                        Ignored if null or if querySkuDetails is false.
     * @param moreSubsSkus    additional SUBSCRIPTIONS skus to query information on, regardless of ownership.
     *                        Ignored if null or if querySkuDetails is false.
     */
    public void queryInventoryAsync(final boolean querySkuDetails,
                                    @Nullable final List<String> moreItemSkus,
                                    @Nullable final List<String> moreSubsSkus,
                                    @NotNull final IabHelper.QueryInventoryFinishedListener listener) {
        checkSetupDone("queryInventory");
        //noinspection ConstantConditions
        if (listener == null) {
            throw new IllegalArgumentException("Inventory listener must be not null");
        }
        new Thread(new Runnable() {
            public void run() {
                IabResult result;
                Inventory inv = null;
                try {
                    inv = queryInventory(querySkuDetails, moreItemSkus, moreSubsSkus);
                    result = new IabResult(BILLING_RESPONSE_RESULT_OK, "Inventory refresh successful.");
                } catch (IabException exception) {
                    result = exception.getResult();
                    Logger.e("queryInventoryAsync() Error : ",exception);
                }

                final IabResult result_f = result;
                final Inventory inv_f = inv;
                if (setupState != SETUP_DISPOSED) {
                    handler.post(new Runnable() {
                        public void run() {
                            listener.onQueryInventoryFinished(result_f, inv_f);
                        }
                    });
                }
            }
        }).start();
    }

    public void consume(Purchase purchase) throws IabException {
        checkSetupDone("consume");
        Purchase purchaseStoreSku = (Purchase) purchase.clone(); // TODO: use Purchase.getStoreSku()
        purchaseStoreSku.setSku(SkuManager.getInstance().getStoreSku(mAppstore.getAppstoreName(), purchase.getSku()));
        mAppstoreBillingService.consume(purchaseStoreSku);
    }

    public void consumeAsync(@NotNull final Purchase purchase,
                             @NotNull final IabHelper.OnConsumeFinishedListener listener) {
        consumeAsyncInternal(Arrays.asList(purchase), listener, null);
    }

    public void consumeAsync(@NotNull final List<Purchase> purchases,
                             @NotNull final IabHelper.OnConsumeMultiFinishedListener listener) {
        //noinspection ConstantConditions
        if (listener == null) {
            throw new IllegalArgumentException("Consume listener must be not null!");
        }
        consumeAsyncInternal(purchases, null, listener);
    }

    void consumeAsyncInternal(@NotNull final List<Purchase> purchases,
                              @Nullable final IabHelper.OnConsumeFinishedListener singleListener,
                              @Nullable final IabHelper.OnConsumeMultiFinishedListener multiListener) {
        checkSetupDone("consume");
        if (purchases.isEmpty()) {
            throw new IllegalArgumentException("Nothing to consume.");
        }
        new Thread(new Runnable() {
            public void run() {
                final List<IabResult> results = new ArrayList<IabResult>();
                for (final Purchase purchase : purchases) {
                    try {
                        consume(purchase);
                        results.add(new IabResult(BILLING_RESPONSE_RESULT_OK, "Successful consume of sku " + purchase.getSku()));
                    } catch (IabException exception) {
                        results.add(exception.getResult());
                        Logger.e("consumeAsyncInternal() Error : ", exception);
                    }
                }

                if (setupState != SETUP_DISPOSED && singleListener != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            singleListener.onConsumeFinished(purchases.get(0), results.get(0));
                        }
                    });
                }
                if (setupState != SETUP_DISPOSED && multiListener != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            multiListener.onConsumeMultiFinished(purchases, results);
                        }
                    });
                }
            }
        }).start();
    }

    // Checks that setup was done; if not, throws an exception.
    void checkSetupDone(String operation) {
        if (!setupSuccessful()) {
            String stateToString = setupStateToString(setupState);
            Logger.e("Illegal state for operation (", operation, "): ", stateToString);
            throw new IllegalStateException(stateToString + " Can't perform operation: " + operation);
        }
    }

    private static String setupStateToString(int setupState) {
        String state;
        if (setupState == SETUP_RESULT_NOT_STARTED) {
            state = " IAB helper is not set up.";
        } else if (setupState == SETUP_DISPOSED) {
            state = "IAB helper was disposed of.";
        } else if (setupState == SETUP_RESULT_SUCCESSFUL) {
            state = "IAB helper is set up.";
        } else if (setupState == SETUP_RESULT_FAILED) {
            state = "IAB helper setup failed.";
        } else {
            throw new IllegalStateException("Wrong setup state: " + setupState);
        }
        return state;
    }


    /**
     * @return True if this OpenIabHelper instance is set up successfully.
     */
    public boolean setupSuccessful() {
        return setupState == SETUP_RESULT_SUCCESSFUL;
    }

    /**
     * @deprecated Use {@link org.onepf.oms.util.Logger#isLoggable()}
     * <p/>
     * Will be removed in version 1.0.
     */
    public static boolean isDebugLog() {
        return Logger.isLoggable();
    }

    /**
     * @deprecated Use {@link org.onepf.oms.util.Logger#setLoggable(boolean)}
     * <p/>
     * Will be removed in version 1.0.
     */
    public static void enableDebugLogging(boolean enabled) {
        Logger.setLoggable(enabled);
    }

    /**
     * @deprecated Use {@link org.onepf.oms.util.Logger#setLoggable(boolean)}. Param 'tag' no effect.
     * <p/>
     * Will be removed in version 1.0.
     */
    public static void enableDebuglLogging(boolean enabled, String tag) {
        Logger.setLoggable(enabled);
    }

    public interface OnInitListener {
        void onInitFinished();
    }

    public interface OnOpenIabHelperInitFinished {
        void onOpenIabHelperInitFinished();
    }

    public interface OpenStoresDiscoveredListener{
        void openStoresDiscovered(@NotNull List<Appstore> appstores);
    }

    /**
     * All options of OpenIAB can be found here.
     * Create instance of this class via {@link Builder}.
     */
    public static class Options {

        /**
         * Verify signatures in any store.
         * <p/>
         * By default in Google's IabHelper. Throws exception if key is not available or invalid.
         * To prevent crashes OpenIAB wouldn't connect to OpenStore if no publicKey provided
         */
        public static final int VERIFY_EVERYTHING = 0;

        /**
         * Don't verify signatures. To perform verification on server-side
         */
        public static final int VERIFY_SKIP = 1;

        /**
         * Verify signatures only if publicKey is available. Otherwise skip verification.
         * <p/>
         * Developer is responsible for verify
         */
        public static final int VERIFY_ONLY_KNOWN = 2;


        public static final int SEARCH_STRATEGY_INSTALLER = 0;
        public static final int SEARCH_STRATEGY_BEST_FIT = 1;
        public static final int SEARCH_STRATEGY_INSTALLER_THEN_BEST_FIT = 2;


        /**
         * @deprecated Use {@link #getAvailableStores()}
         * Will be private since 1.0.
         */
        public final Set<Appstore> availableStores;

        /**
         * @deprecated Use {@link #getPreferredStoreNames()}
         * Will be private since 1.0.
         */
        public final Set<String> preferredStoreNames;

        /**
         * @deprecated
         * No longer used.
         */
        public final int discoveryTimeoutMs = 0;

        /**
         * @deprecated Use {@link #isCheckInventory()}
         * Will be private since 1.0.
         */
        public final boolean checkInventory;

        /**
         * @deprecated
         * No longer used.
         */
        public final int checkInventoryTimeoutMs = 0;

        /**
         * @deprecated Use {@link #getVerifyMode()}
         * Will be private since 1.0.
         */
        @MagicConstant(intValues = {VERIFY_EVERYTHING, VERIFY_ONLY_KNOWN, VERIFY_SKIP})
        public final int verifyMode;

        @MagicConstant(intValues = {SEARCH_STRATEGY_INSTALLER, SEARCH_STRATEGY_BEST_FIT, SEARCH_STRATEGY_INSTALLER_THEN_BEST_FIT})
        private final int storeSearchStrategy;

        /**
         * @deprecated Use {@link #getStoreKeys()}
         * Will be private since 1.0.
         */
        private final Map<String, String> storeKeys;

        /**
         * @deprecated Usr {@link #getSamsungCertificationRequestCode()}
         * Will be private since 1.0.
         */
        public final int samsungCertificationRequestCode;

        /**
         * @deprecated Use {@link Builder} instead.
         */
        public Options() {
            this.checkInventory = false;
            this.availableStores = Collections.emptySet();
            this.storeKeys = Collections.emptyMap();
            this.preferredStoreNames = Collections.emptySet();
            this.verifyMode = VERIFY_SKIP;
            this.samsungCertificationRequestCode = SamsungAppsBillingService.REQUEST_CODE_IS_ACCOUNT_CERTIFICATION;
            this.storeSearchStrategy = SEARCH_STRATEGY_INSTALLER;
        }

        private Options(final Set<Appstore> availableStores,
                        final Map<String, String> storeKeys,
                        final boolean checkInventory,
                        final @MagicConstant(intValues = {VERIFY_EVERYTHING, VERIFY_ONLY_KNOWN, VERIFY_SKIP}) int verifyMode,
                        final Set<String> preferredStoreNames,
                        final int samsungCertificationRequestCode,
                        final int storeSearchStrategy) {
            this.checkInventory = checkInventory;
            this.availableStores = availableStores;
            this.storeKeys = storeKeys;
            this.preferredStoreNames = preferredStoreNames;
            this.verifyMode = verifyMode;
            this.samsungCertificationRequestCode = samsungCertificationRequestCode;
            this.storeSearchStrategy = storeSearchStrategy;
        }

        /**
         * Used for SamsungApps setup. Specify your own value using {@link Builder#setSamsungCertificationRequestCode(int)} if default one interfere your with code.
         * <p/>
         * default value is {@link SamsungAppsBillingService#REQUEST_CODE_IS_ACCOUNT_CERTIFICATION}
         */
        public int getSamsungCertificationRequestCode() {
            return samsungCertificationRequestCode;
        }

        /**
         * OpenIAB could skip receipt verification by publicKey for GooglePlay and OpenStores
         * <p/>
         * Receipt could be verified in {@link IabHelper.OnIabPurchaseFinishedListener#onIabPurchaseFinished(IabResult, Purchase)}
         * using {@link Purchase#getOriginalJson()} and {@link Purchase#getSignature()}
         * @see Builder#setVerifyMode(int)
         */
        @MagicConstant(intValues = {VERIFY_EVERYTHING, VERIFY_ONLY_KNOWN, VERIFY_SKIP})
        public int getVerifyMode() {
            return verifyMode;
        }

        /**
         * Set strategy to help OpenIAB pick correct store
         * <p/>
         * @see Builder#setStoreSearchStrategy(int)
         */
        @MagicConstant(intValues = {SEARCH_STRATEGY_INSTALLER, SEARCH_STRATEGY_BEST_FIT, SEARCH_STRATEGY_INSTALLER_THEN_BEST_FIT})
        public int getStoreSearchStrategy() {
            return storeSearchStrategy;
        }

        /**
         * Check user inventory in every store to help with selecting proper store
         * <p/>
         * Will try to connect to each billingService and extract user's purchases.
         * If purchases have been found in the only store that store will be used for further purchases.
         * If purchases have been found in multiple stores only such stores will be used for further elections
         */
        public boolean isCheckInventory() {
            return checkInventory;
        }

        /**
         * @deprecated
         */
        @Deprecated
        public long getCheckInventoryTimeout() {
            return 0;
        }

        /**
         * @deprecated
         */
        @Deprecated
        public long getDiscoveryTimeout() {
            return 0;
        }

        /**
         * List of stores to be used for store elections. By default GooglePlay, Amazon, SamsungApps and
         * all installed OpenStores are used.
         * <p/>
         * To specify your own list, you need to instantiate Appstore object manually.
         * GooglePlay, Amazon and SamsungApps could be instantiated directly. OpenStore can be discovered
         * using {@link OpenIabHelper#discoverOpenStores()}
         * <p/>
         * <b>If not empty, only this instances of {@link Appstore} will be considered by OpenIAB</b>
         */
        public @NotNull Set<Appstore> getAvailableStores() {
            return availableStores;
        }

        /**
         * Used as priority list if store that installed app is not found and there are
         * multiple stores installed on device that supports billing.
         */
        public @NotNull Set<String> getPreferredStoreNames() {
            return preferredStoreNames;
        }

        /**
         * storeKeys is map of [ appstore name -> publicKeyBase64 ]
         * <p/>
         * <b>publicKey</b> key is used to verify receipt is created by genuine Appstore using
         * provided signature. It can be found in Developer Console of particular store
         * <p/>
         * <b>name</b> of particular store can be provided by local_store tool if you run it on device.
         * For Google Play OpenIAB uses {@link OpenIabHelper#NAME_GOOGLE}.
         * <p/>
         * <p>Note:
         * AmazonApps and SamsungApps doesn't use RSA keys for receipt verification, so you don't need
         * to specify it
         */
        @NotNull public Map<String, String> getStoreKeys() {
            return storeKeys;
        }

        /**
         * Look for store with <b>name</b> among available stores
         * @return {@link Appstore} with <b>name</b> if one found, null otherwise.
         * @see #getAvailableStores()
         * @see Builder#addAvailableStores(java.util.Collection)
         */
        public @Nullable Appstore getAvailableStoreWithName(@NotNull final String name) {
            for (Appstore s : availableStores) {
                if (name.equals(s.getAppstoreName())) {
                    return s;
                }
            }
            return null;
        }

        /**
         * Builder class for {@link Options}
         */
        public static final class Builder {

            private final Set<String> preferredStoreNames = new LinkedHashSet<String>();
            private final Set<Appstore> availableStores = new HashSet<Appstore>();
            private final Map<String, String> storeKeys = new HashMap<String, String>();
            private boolean checkInventory = false;
            private int samsungCertificationRequestCode
                    = SamsungAppsBillingService.REQUEST_CODE_IS_ACCOUNT_CERTIFICATION;

            @MagicConstant(intValues = {VERIFY_EVERYTHING, VERIFY_ONLY_KNOWN, VERIFY_SKIP})
            private int verifyMode = VERIFY_EVERYTHING;

            @MagicConstant(intValues = {SEARCH_STRATEGY_INSTALLER, SEARCH_STRATEGY_BEST_FIT, SEARCH_STRATEGY_INSTALLER_THEN_BEST_FIT})
            private int storeSearchStrategy = SEARCH_STRATEGY_INSTALLER;

            /**
             * Add available store to options.
             *
             * @param stores Stores to add.
             * @see #addAvailableStores(Collection)
             * @see Options#getAvailableStores()
             */
            public Builder addAvailableStores(@NotNull final Appstore... stores) {
                addAvailableStores(Arrays.asList(stores));
                return this;
            }

            /**
             * Add available store to options.
             *
             * @param stores Stores to add.
             * @see Options#getAvailableStores().
             */
            public Builder addAvailableStores(@NotNull final Collection<Appstore> stores) {
                this.availableStores.addAll(stores);
                return this;
            }

            /**
             * Set check inventory, false.
             *
             * @see Options#isCheckInventory()
             */
            public Builder setCheckInventory(final boolean checkInventory) {
                this.checkInventory = checkInventory;
                return this;
            }

            /**
             * No longer used.
             */
            @Deprecated
            public Builder setDiscoveryTimeout(final int discoveryTimeout) {
                return this;
            }

            /**
             * No longer used.
             */
            @Deprecated
            public Builder setCheckInventoryTimeout(final int checkInventoryTimeout) {
                return this;
            }

            /**
             * Add single store keys to options.
             *
             * @param storeName Name of store.
             * @param publicKey Key of store.
             * @throws java.lang.IllegalArgumentException When store public key is not in valid base64 format.
             * @see Options#getStoreKeys()
             */
            public Builder addStoreKey(@NotNull final String storeName, @NotNull final String publicKey) {
                try {
                    Security.generatePublicKey(publicKey);
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            String.format("Invalid publicKey for store: %s, key: %s.",
                                    storeName, publicKey),
                            e);
                }
                this.storeKeys.put(storeName, publicKey);
                return this;
            }

            /**
             * Add store keys to options.
             *
             * @param storeKeys Map storeName - store public key.
             * @throws java.lang.IllegalArgumentException If some key in map is not in valid base64 format.
             * @see Options.Builder#addStoreKeys(java.util.Map)
             * @see Options#getStoreKeys()
             */
            public Builder addStoreKeys(@NotNull final Map<String, String> storeKeys) {
                for (final String key : storeKeys.keySet()) {
                    final String value;
                    if (!TextUtils.isEmpty(value = storeKeys.get(key))) {
                        addStoreKey(key, value);
                    }
                }
                return this;
            }

            /**
             * Set verify mode for store. By default set to {@link Options#VERIFY_EVERYTHING}.
             *
             * @param verifyMode Verify mode for store. Must be one of {@link Options#VERIFY_EVERYTHING},
             *                   {@link Options#VERIFY_SKIP},
             *                   {@link Options#VERIFY_ONLY_KNOWN}.
             * @see Options#getVerifyMode()
             */
            public Builder setVerifyMode(
                   final @MagicConstant(intValues = {
                           VERIFY_EVERYTHING,
                           VERIFY_ONLY_KNOWN,
                           VERIFY_SKIP}) int verifyMode) {
                this.verifyMode = verifyMode;
                return this;
            }

            /**
             *
             * @param storeSearchStrategy Store search strategy for OpenIAB.
             *                            Must be one of {@link #SEARCH_STRATEGY_INSTALLER}, {@link #SEARCH_STRATEGY_BEST_FIT} or {@link #SEARCH_STRATEGY_INSTALLER_THEN_BEST_FIT}
             * @see Options#getStoreSearchStrategy()
             */
            public Builder setStoreSearchStrategy(
                    final @MagicConstant(intValues = {
                            SEARCH_STRATEGY_INSTALLER,
                            SEARCH_STRATEGY_BEST_FIT,
                            SEARCH_STRATEGY_INSTALLER_THEN_BEST_FIT}) int storeSearchStrategy) {
                this.storeSearchStrategy = storeSearchStrategy;
                return this;
            }

            /**
             * Add preferred stores to options. Priority of selection is order in what stores add.
             *
             * @see #addPreferredStoreName(java.util.Collection)
             * @see Options#getPreferredStoreNames()
             */
            public Builder addPreferredStoreName(@NotNull final String... storeNames) {
                addPreferredStoreName(Arrays.asList(storeNames));
                return this;
            }

            /**
             * Add preferred stores to options. Priority of selection is order in what stores add.
             *
             * @see Options#getPreferredStoreNames()
             */
            public Builder addPreferredStoreName(@NotNull final Collection<String> storeNames) {
                this.preferredStoreNames.addAll(storeNames);
                return this;
            }

            /**
             * Set request code for samsung certification.
             *
             * @param code Request code. Must be positive value.
             * @throws java.lang.IllegalArgumentException if code negative or zero value.
             * @see Options#getSamsungCertificationRequestCode()
             */
            public Builder setSamsungCertificationRequestCode(int code) {
                if (code < 0) {
                    throw new IllegalArgumentException("Value '" + code +
                            "' can't be request code. Request code must be a positive value.");
                }

                this.samsungCertificationRequestCode = code;
                return this;
            }

            /**
             * @return Create new instance of {@link Options}.
             */
            public Options build() {
                return new Options(
                        Collections.unmodifiableSet(availableStores),
                        Collections.unmodifiableMap(storeKeys),
                        checkInventory,
                        verifyMode,
                        Collections.unmodifiableSet(preferredStoreNames),
                        samsungCertificationRequestCode,
                        storeSearchStrategy);
            }
        }
    }

}
