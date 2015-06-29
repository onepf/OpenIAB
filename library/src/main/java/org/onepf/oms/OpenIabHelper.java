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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
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

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.onepf.oms.appstore.AmazonAppstore;
import org.onepf.oms.appstore.CafeBazaar;
import org.onepf.oms.appstore.FortumoStore;
import org.onepf.oms.appstore.GooglePlay;
import org.onepf.oms.appstore.NokiaStore;
import org.onepf.oms.appstore.OpenAppstore;
import org.onepf.oms.appstore.SamsungApps;
import org.onepf.oms.appstore.SamsungAppsBillingService;
import org.onepf.oms.appstore.SkubitAppstore;
import org.onepf.oms.appstore.SkubitTestAppstore;
import org.onepf.oms.appstore.googleUtils.IabException;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabHelper.OnIabSetupFinishedListener;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.appstore.googleUtils.Purchase;
import org.onepf.oms.appstore.googleUtils.Security;
import org.onepf.oms.util.CollectionUtils;
import org.onepf.oms.util.Logger;
import org.onepf.oms.util.Utils;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import static org.onepf.oms.OpenIabHelper.Options.SEARCH_STRATEGY_INSTALLER;
import static org.onepf.oms.OpenIabHelper.Options.SEARCH_STRATEGY_INSTALLER_THEN_BEST_FIT;
import static org.onepf.oms.OpenIabHelper.Options.VERIFY_EVERYTHING;
import static org.onepf.oms.OpenIabHelper.Options.VERIFY_SKIP;

/**
 * @author Boris Minaev, Oleg Orlov, Kirill Rozov
 * @since 16.04.13
 */
public class OpenIabHelper {
    // Intent to discover and bind to Open Stores
    private static final String BIND_INTENT = "org.onepf.oms.openappstore.BIND";

    /**
     * Setup process was not started.
     */
    public static final int SETUP_RESULT_NOT_STARTED = -1;

    /**
     * Setup process is completed successfully, the billing provider was found.
     */
    public static final int SETUP_RESULT_SUCCESSFUL = 0;

    /**
     * Setup process is completed, the billing provider was not found.
     */
    public static final int SETUP_RESULT_FAILED = 1;

    /**
     * Setup process was disposed.
     */
    public static final int SETUP_DISPOSED = 2;

    /**
     * Setup process is in progress.
     */
    public static final int SETUP_IN_PROGRESS = 3;

    @MagicConstant(intValues = {SETUP_DISPOSED, SETUP_IN_PROGRESS,
            SETUP_RESULT_FAILED, SETUP_RESULT_NOT_STARTED, SETUP_RESULT_SUCCESSFUL})
    private volatile int setupState = SETUP_RESULT_NOT_STARTED;

    // To handle {@link #handleActivityResult(int, int, Intent)} during setup.
    @Nullable
    private volatile Appstore appStoreInSetup;

    /**
     * E.g. non-consumable: book content, video, level; consumable: power, life.
     */
    public static final String ITEM_TYPE_INAPP = "inapp";

    /**
     * E.g. subscription for a music channel.
     */
    public static final String ITEM_TYPE_SUBS = "subs";

    // Billing response codes

    /**
     * Successful billing result.
     */
    public static final int BILLING_RESPONSE_RESULT_OK = 0;

    /**
     * Billing is not available.
     */
    public static final int BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3;

    /**
     * An error occurred during the setup process.
     */
    public static final int BILLING_RESPONSE_RESULT_ERROR = 6;

    // Known wrappers

    /**
     * Internal library name for the Google Play store.
     */
    public static final String NAME_GOOGLE = "com.google.play";

    /**
     * Internal library name for the Amazon Appstore store.
     */
    public static final String NAME_AMAZON = "com.amazon.apps";

    /**
     * Internal library name for the Samsung Apps store.
     */
    public static final String NAME_SAMSUNG = "com.samsung.apps";

    /**
     * Internal library name for the Nokia store.
     */
    public static final String NAME_NOKIA = "com.nokia.nstore";

    /**
     * Internal library name for the Fortumo store.
     * Actually Fortumo is not an application store but for consistency it was wrapped with the common interface.
     */
    public static final String NAME_FORTUMO = "com.fortumo.billing";

    /**
     * Internal library name for the Skubit store.
     */
    public static final String NAME_SKUBIT = "com.skubit.android";

    /**
     * Internal library name for the Skubit test store.
     */
    public static final String NAME_SKUBIT_TEST = "net.skubit.android";

    // Known Open Stores
    /**
     * Internal library name for the Yandex.Store store.
     */
    public static final String NAME_YANDEX = "com.yandex.store";

    /**
     * Internal library name for the family of Appland stores.
     */
    public static final String NAME_APPLAND = "Appland";

    /**
     * Internal library name for SlideME store.
     */
    public static final String NAME_SLIDEME = "SlideME";

    /**
     * Internal library name for Aptoide store.
     */
    public static final String NAME_APTOIDE = "cm.aptoide.pt";

    /**
     * Internal library name for the CafeBazaar store.
     */
    public static final String NAME_CAFEBAZAAR = "com.farsitel.bazaar";

    private final PackageManager packageManager;

    private final Context context;

    //Only for Samsung Apps. An activity is required to check Samsung billing.
    @Nullable
    private Activity activity;

    private final Handler handler = new Handler(Looper.getMainLooper());

    //Store, chosen as the billing provider
    @Nullable
    private volatile Appstore appstore;

    //billing service of the chosen store
    @Nullable
    private volatile AppstoreInAppBillingService appStoreBillingService;

    //Object to store developer's settings (available stores, preferred stores, store search strategy, etc)
    private final Options options;

    //Complete list of Appstores to check. Used if options.getAvailableStores() or options.getAvailableStoreNames is not empty.
    private final Set<Appstore> availableAppstores = new LinkedHashSet<Appstore>();

    @Nullable
    private ExecutorService setupExecutorService;

    @NotNull
    private final ExecutorService inventoryExecutor = Executors.newSingleThreadExecutor();

    //For internal use only. Do not make it public!
    private static interface AppstoreFactory {
        @Nullable
        Appstore get();
    }

    private final Map<String, String> appStorePackageMap = new HashMap<String, String>();
    private final Map<String, AppstoreFactory> appStoreFactoryMap = new HashMap<String, AppstoreFactory>();

    {
        // Known packages for open stores
        appStorePackageMap.put("com.yandex.store", NAME_YANDEX);
        appStorePackageMap.put("cm.aptoide.pt", NAME_APTOIDE);

        // Knows package independent wrappers
        appStoreFactoryMap.put(NAME_FORTUMO, new AppstoreFactory() {
            @NotNull
            @Override
            public Appstore get() {
                return new FortumoStore(context);
            }
        });

        appStorePackageMap.put(GooglePlay.ANDROID_INSTALLER, NAME_GOOGLE);
        appStoreFactoryMap.put(NAME_GOOGLE, new AppstoreFactory() {
            @NotNull
            @Override
            public Appstore get() {
                final String googleKey = options.getVerifyMode() != VERIFY_SKIP
                        ? options.getStoreKeys().get(NAME_GOOGLE)
                        : null;
                return new GooglePlay(new ContextWrapper(context.getApplicationContext()) {
                    @Override
                    public Context getApplicationContext() {
                        return this;
                    }

                    @Override
                    public boolean bindService(final Intent service, final ServiceConnection conn, final int flags) {
                        final List<ResolveInfo> infos = getPackageManager().queryIntentServices(service, 0);
                        if (CollectionUtils.isEmpty(infos)) {
                            return super.bindService(service, conn, flags);
                        }
                        final ResolveInfo serviceInfo = infos.get(0);
                        final String packageName = serviceInfo.serviceInfo.packageName;
                        final String className = serviceInfo.serviceInfo.name;
                        final ComponentName component = new ComponentName(packageName, className);
                        final Intent explicitIntent = new Intent(service);
                        explicitIntent.setComponent(component);
                        return super.bindService(explicitIntent, conn, flags);
                    }
                }, googleKey);
            }
        });

        appStorePackageMap.put(AmazonAppstore.AMAZON_INSTALLER, NAME_AMAZON);
        appStoreFactoryMap.put(NAME_AMAZON, new AppstoreFactory() {
            @NotNull
            @Override
            public Appstore get() {
                return new AmazonAppstore(context);
            }
        });

        appStorePackageMap.put(SamsungApps.SAMSUNG_INSTALLER, NAME_SAMSUNG);
        appStoreFactoryMap.put(NAME_SAMSUNG, new AppstoreFactory() {
            @Nullable
            @Override
            public Appstore get() {
                return new SamsungApps(activity, options);
            }
        });

        appStorePackageMap.put(NokiaStore.NOKIA_INSTALLER, NAME_NOKIA);
        appStoreFactoryMap.put(NAME_NOKIA, new AppstoreFactory() {
            @NotNull
            @Override
            public Appstore get() {
                return new NokiaStore(context);
            }
        });

        appStorePackageMap.put(SkubitAppstore.SKUBIT_INSTALLER, NAME_SKUBIT);
        appStoreFactoryMap.put(NAME_SKUBIT, new AppstoreFactory() {
            @NotNull
            @Override
            public Appstore get() {
                return new SkubitAppstore(context);
            }
        });

        appStorePackageMap.put(SkubitTestAppstore.SKUBIT_INSTALLER, NAME_SKUBIT_TEST);
        appStoreFactoryMap.put(NAME_SKUBIT_TEST, new AppstoreFactory() {
            @NotNull
            @Override
            public Appstore get() {
                return new SkubitTestAppstore(context);
            }
        });

        appStorePackageMap.put(CafeBazaar.ANDROID_INSTALLER, NAME_CAFEBAZAAR);
        appStoreFactoryMap.put(NAME_CAFEBAZAAR, new AppstoreFactory() {
            @NotNull
            @Override
            public Appstore get() {
                final String cafebazaarKey = options.getVerifyMode() != VERIFY_SKIP
                        ? options.getStoreKeys().get(NAME_CAFEBAZAAR)
                        : null;
                return new CafeBazaar(new ContextWrapper(context.getApplicationContext()) {
                    @Override
                    public Context getApplicationContext() {
                        return this;
                    }

                    @Override
                    public boolean bindService(final Intent service, final ServiceConnection conn, final int flags) {
                        final List<ResolveInfo> infos = getPackageManager().queryIntentServices(service, 0);
                        if (CollectionUtils.isEmpty(infos)) {
                            return super.bindService(service, conn, flags);
                        }
                        final ResolveInfo serviceInfo = infos.get(0);
                        final String packageName = serviceInfo.serviceInfo.packageName;
                        final String className = serviceInfo.serviceInfo.name;
                        final ComponentName component = new ComponentName(packageName, className);
                        final Intent explicitIntent = new Intent(service);
                        explicitIntent.setComponent(component);
                        return super.bindService(explicitIntent, conn, flags);
                    }
                }, cafebazaarKey);
            }
        });
    }


    /**
     * Maps the sku and the storeSku for a particular store.
     * The best practice is to use SKU like <code>com.companyname.application.item</code>.
     * Such SKU fits most of stores so it doesn't need to be mapped.
     * If the recommended approach is not applicable, use application internal SKU in the code (usually it is a SKU for Google Play)
     * and map SKU of other stores using this method. OpenIAB will map SKU in both directions,
     * so you can use only your inner SKU to purchase, consume and check.
     *
     * @param sku       The logical internal SKU. E.g. redhat
     * @param storeSku  The store-specific SKU. Shouldn't duplicate already mapped values. E.g. appland.redhat
     * @param storeName The name of a store. @see {@link IOpenAppstore#getAppstoreName()} or {@link #NAME_AMAZON}, {@link #NAME_GOOGLE}
     * @throws java.lang.IllegalArgumentException If one of the arguments is null or empty.
     * @deprecated Use {@link org.onepf.oms.SkuManager#mapSku(String, String, String)}
     */
    public static void mapSku(String sku, String storeName, @NotNull String storeSku) {
        SkuManager.getInstance().mapSku(sku, storeName, storeSku);
    }

    /**
     * Return the previously mapped store SKU for the internal SKU
     *
     * @param appStoreName The store name
     * @param sku          The internal SKU
     * @return SKU used in the store for the specified internal SKU
     * @see org.onepf.oms.SkuManager#mapSku(String, String, String)
     * @deprecated Use {@link org.onepf.oms.SkuManager#getStoreSku(String, String)}
     * <p/>
     */
    @NotNull
    public static String getStoreSku(@NotNull final String appStoreName, @NotNull String sku) {
        return SkuManager.getInstance().getStoreSku(appStoreName, sku);
    }

    /**
     * Returns a mapped application internal SKU using the store name and a store SKU.
     *
     * @see org.onepf.oms.SkuManager#mapSku(String, String, String)
     * @deprecated Use {@link org.onepf.oms.SkuManager#getSku(String, String)}
     */
    @NotNull
    public static String getSku(@NotNull final String appStoreName, @NotNull String storeSku) {
        return SkuManager.getInstance().getSku(appStoreName, storeSku);
    }

    /**
     * @param appStoreName The store name, not package! E.g. {@link OpenIabHelper#NAME_AMAZON}
     * @return list of SKU that have mappings for the app store
     * @deprecated Use {@link org.onepf.oms.SkuManager#getAllStoreSkus(String)}
     */
    @Nullable
    public static List<String> getAllStoreSkus(@NotNull final String appStoreName) {
        final Collection<String> allStoreSkus =
                SkuManager.getInstance().getAllStoreSkus(appStoreName);
        return allStoreSkus == null ? Collections.<String>emptyList()
                : new ArrayList<String>(allStoreSkus);
    }

    /**
     * @param storeKeys The map [store name - store public key]. Is applicable only to Google Play and all Open Stores.
     * @param context   The context. If you want to support Samsung Apps you must pass an Activity as the context, in other cases any context is acceptable
     * @see {@link OpenIabHelper#OpenIabHelper(Context, Options)} for the details.
     * @deprecated Use {@link org.onepf.oms.OpenIabHelper#OpenIabHelper(android.content.Context, org.onepf.oms.OpenIabHelper.Options)}
     * Will be removed in 1.0 release.
     */
    public OpenIabHelper(@NotNull Context context, @NotNull Map<String, String> storeKeys) {
        this(context,
                new Options.Builder()
                        .addStoreKeys(storeKeys)
                        .build()
        );
    }

    /**
     * @param storeKeys       see {@link org.onepf.oms.OpenIabHelper.Options#getStoreKeys()}
     * @param preferredStores see {@link org.onepf.oms.OpenIabHelper.Options#getPreferredStoreNames()}
     * @param context         if you want to support Samsung Apps you must pass an Activity, in other cases any context is acceptable
     * @see {@link OpenIabHelper#OpenIabHelper(Context, Options)} for details
     * @deprecated Use {@link org.onepf.oms.OpenIabHelper#OpenIabHelper(android.content.Context, org.onepf.oms.OpenIabHelper.Options)}
     * Will be removed in 1.0 release.
     */
    public OpenIabHelper(@NotNull Context context, @NotNull Map<String, String> storeKeys, String[] preferredStores) {
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
     * See {@link OpenIabHelper#OpenIabHelper(Context, Options)} for details
     */
    public OpenIabHelper(@NotNull Context context, @NotNull Map<String, String> storeKeys, String[] preferredStores, Appstore[] availableStores) {
        this(context,
                new Options.Builder()
                        .addStoreKeys(storeKeys)
                        .addPreferredStoreName(preferredStores)
                        .addAvailableStores(availableStores)
                        .build()
        );
    }

    /**
     * @param options Specify all necessary options: <br/>
     *                - public keys if you want to delegate purchase verification to OpenIAB<br/>
     *                - search strategy, do we work only with the app installer or the billing provider can be selected from the other stores<br/>
     *                -
     * @param context If you want to support Samsung Apps, pass an Activity for the context parameter, in other cases any context is acceptable
     */
    public OpenIabHelper(@NotNull Context context, Options options) {
        this.context = context.getApplicationContext();
        packageManager = context.getPackageManager();
        this.options = options;
        if (context instanceof Activity) {
            this.activity = (Activity) context;
        }

        checkOptions();
    }


    /**
     * Discovers all available stores and selects the best billing service.
     * Should be called from the UI thread
     *
     * @param listener The listener to call when setup is completed
     */
    public void startSetup(@NotNull final OnIabSetupFinishedListener listener) {
        if (options != null) {
            Logger.d("startSetup() options = ", options);
        }
        //noinspection ConstantConditions
        if (listener == null) {
            throw new IllegalArgumentException("Setup listener must be not null!");
        }
        if (setupState != SETUP_RESULT_NOT_STARTED && setupState != SETUP_RESULT_FAILED) {
            throw new IllegalStateException("Couldn't be set up. Current state: " + setupStateToString(setupState));
        }
        setupState = SETUP_IN_PROGRESS;
        setupExecutorService = Executors.newSingleThreadExecutor();

        // Compose full list of available stores to check billing for
        availableAppstores.clear();
        // Add all manually supplied Appstores
        availableAppstores.addAll(options.getAvailableStores());
        final List<String> storeNames = new ArrayList<String>(options.getAvailableStoreNames());
        // Remove already added stores
        for (final Appstore appstore : availableAppstores) {
            storeNames.remove(appstore.getAppstoreName());
        }
        // Instantiate and add all known wrappers
        final List<Appstore> instantiatedAppstores = new ArrayList<Appstore>();
        for (final String storeName : options.getAvailableStoreNames()) {
            if (appStoreFactoryMap.containsKey(storeName)) {
                final Appstore appstore = appStoreFactoryMap.get(storeName).get();
                instantiatedAppstores.add(appstore);
                availableAppstores.add(appstore);
                storeNames.remove(storeName);
            }
        }
        // Look among open stores for specified store names
        if (!storeNames.isEmpty()) {
            discoverOpenStores(new OpenStoresDiscoveredListener() {
                @Override
                public void openStoresDiscovered(@NotNull final List<Appstore> appStores) {
                    // Add all specified open stores
                    for (final Appstore appstore : appStores) {
                        final String name = appstore.getAppstoreName();
                        if (storeNames.contains(name)) {
                            availableAppstores.add(appstore);
                        } else {
                            final AppstoreInAppBillingService billingService;
                            if ((billingService = appstore.getInAppBillingService()) != null) {
                                billingService.dispose();
                                Logger.d("startSetup() billing service disposed for ", appstore.getAppstoreName());
                            }
                        }
                    }
                    final Runnable cleanup = new Runnable() {
                        @Override
                        public void run() {
                            for (final Appstore appstore : instantiatedAppstores) {
                                final AppstoreInAppBillingService billingService;
                                if ((billingService = appstore.getInAppBillingService()) != null) {
                                    billingService.dispose();
                                    Logger.d("startSetup() billing service disposed for ", appstore.getAppstoreName());
                                }
                            }
                        }
                    };
                    if (setupState != SETUP_IN_PROGRESS) {
                        cleanup.run();
                        return;
                    }
                    setupWithStrategy(new OnIabSetupFinishedListener() {
                        @Override
                        public void onIabSetupFinished(final IabResult result) {
                            listener.onIabSetupFinished(result);
                            instantiatedAppstores.remove(OpenIabHelper.this.appstore);
                            cleanup.run();
                        }
                    });
                }
            });
        } else {
            setupWithStrategy(listener);
        }
    }

    private void setupWithStrategy(@NotNull final OnIabSetupFinishedListener listener) {
        final int storeSearchStrategy = options.getStoreSearchStrategy();
        Logger.d("setupWithStrategy() store search strategy = ", storeSearchStrategy);
        final String packageName = context.getPackageName();
        Logger.d("setupWithStrategy() package name = ", packageName);
        final String packageInstaller = packageManager.getInstallerPackageName(packageName);
        Logger.d("setupWithStrategy() package installer = ", packageInstaller);
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

    private void setupForPackage(@NotNull final OnIabSetupFinishedListener listener,
                                 @NotNull final String packageInstaller,
                                 final boolean withFallback) {
        Appstore appstore = null;
        if (appStorePackageMap.containsKey(packageInstaller)) {
            // Package installer is a known appstore
            final String appstoreName = appStorePackageMap.get(packageInstaller);
            if (this.availableAppstores.isEmpty()) {
                if (appStoreFactoryMap.containsKey(appstoreName)) {
                    appstore = appStoreFactoryMap.get(appstoreName).get();
                }
            } else {
                // Developer explicitly specified available stores
                appstore = getAvailableStoreByName(appstoreName);
                if (appstore == null) {
                    // Store is known but isn't available
                    if (withFallback) {
                        setup(listener);
                    } else {
                        finishSetup(listener);
                    }
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
                        if (OpenIabHelper.this.availableAppstores.isEmpty()) {
                            appstore = openAppstore;
                        } else {
                            // Developer explicitly specified available stores
                            appstore = getAvailableStoreByName(openStoreName);
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
            public void onServiceDisconnected(final ComponentName name) {
            }
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

    private void setup(@NotNull final OnIabSetupFinishedListener listener) {
        // List of wrappers to check
        final Set<Appstore> appstoresToCheck = new LinkedHashSet<Appstore>();

        final Set<String> availableStoreNames = options.getAvailableStoreNames();
        if (!this.availableAppstores.isEmpty() || !availableStoreNames.isEmpty()) {
            // Use only stores specified explicitly
            for (final String name : availableStoreNames) {
                // Add available stored according to preferred stores priority
                final Appstore appstore = getAvailableStoreByName(name);
                if (appstore != null) {
                    appstoresToCheck.add(appstore);
                }
            }
            appstoresToCheck.addAll(this.availableAppstores);
            checkBillingAndFinish(listener, appstoresToCheck);
        } else {
            discoverOpenStores(new OpenStoresDiscoveredListener() {
                @Override
                public void openStoresDiscovered(@NotNull final List<Appstore> appstores) {
                    final List<Appstore> allAvailableAppstores = new ArrayList<Appstore>(appstores);
                    // Add all available wrappers
                    for (final String appstorePackage : appStorePackageMap.keySet()) {
                        final String name = appStorePackageMap.get(appstorePackage);
                        if (!TextUtils.isEmpty(name)
                                && appStoreFactoryMap.containsKey(name)
                                && Utils.packageInstalled(context, appstorePackage)) {
                            allAvailableAppstores.add(appStoreFactoryMap.get(name).get());
                        }
                    }
                    // All package independent wrappers
                    for (final String appstoreName : appStoreFactoryMap.keySet()) {
                        if (!appStorePackageMap.values().contains(appstoreName)) {
                            allAvailableAppstores.add(appStoreFactoryMap.get(appstoreName).get());
                        }
                    }
                    // Add available stored according to preferred stores priority
                    for (final String name : availableStoreNames) {
                        for (final Appstore appstore : allAvailableAppstores) {
                            if (TextUtils.equals(appstore.getAppstoreName(), name)) {
                                appstoresToCheck.add(appstore);
                                break;
                            }
                        }
                    }
                    // Add everything else
                    appstoresToCheck.addAll(allAvailableAppstores);
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
            Logger.d("getOpenAppstore() billing is not supported by store: ", name);
        } else if (verifyMode == Options.VERIFY_EVERYTHING && TextUtils.isEmpty(publicKey)) {
            // don't connect to OpenStore if no key provided and verification is strict
            Logger.e("getOpenAppstore() verification is required but publicKey is not provided: ", name);
        } else {
            final OpenAppstore openAppstore =
                    new OpenAppstore(context, appstoreName, openAppstoreService, billingIntent, publicKey, serviceConnection);
            openAppstore.componentName = name;
            Logger.d("getOpenAppstore() returns ", openAppstore.getAppstoreName());
            return openAppstore;
        }

        return null;
    }

    @NotNull
    private Intent getBindServiceIntent(@NotNull final ServiceInfo serviceInfo) {
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
                        appStoreInSetup = appstore;
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
                            // Dispose of all initialized open app stores
                            final Collection<Appstore> appstoresToDispose = new ArrayList<Appstore>(availableAppstores);
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
                        appStoreInSetup = appstore;
                        if (appstore.isBillingAvailable(packageName) && versionOk(appstore)) {
                            checkedAppstore = appstore;
                            break;
                        }
                    }
                    final Appstore foundAppstore = checkedAppstore;
                    final OnIabSetupFinishedListener listenerWrapper = new OnIabSetupFinishedListener() {
                        @Override
                        public void onIabSetupFinished(final IabResult result) {
                            // Dispose of all initialized open app stores
                            final Collection<Appstore> appstoresToDispose = new ArrayList<Appstore>(appstores);
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
            Logger.d("dispose() was called for ", appstore.getAppstoreName());
        }
    }

    private boolean versionOk(@NotNull final Appstore appstore) {
        final String packageName = context.getPackageName();
        int versionCode = Appstore.PACKAGE_VERSION_UNDEFINED;
        try {
            versionCode = context.getPackageManager().getPackageInfo(packageName, 0).versionCode;
        } catch (NameNotFoundException ignore) {
        }
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
        activity = null;
        appStoreInSetup = null;
        setupExecutorService.shutdownNow();
        setupExecutorService = null;
        if (setupState == SETUP_DISPOSED) {
            if (appstore != null) {
                dispose(Arrays.asList(appstore));
            }
            return;
        } else if (setupState != SETUP_IN_PROGRESS) {
            throw new IllegalStateException("Setup is not started or already finished.");
        }
        final boolean setUpSuccessful = iabResult.isSuccess();
        setupState = setUpSuccessful ? SETUP_RESULT_SUCCESSFUL : SETUP_RESULT_FAILED;
        if (setUpSuccessful) {
            if (appstore == null) {
                throw new IllegalStateException("Appstore can't be null if setup is successful");
            }
            this.appstore = appstore;
            appStoreBillingService = appstore.getInAppBillingService();
        }
        Logger.dWithTimeFromUp("finishSetup() === SETUP DONE === result: ", iabResult, " Appstore: ", appstore);
        listener.onIabSetupFinished(iabResult);
    }

    private
    @Nullable
    Appstore getAvailableStoreByName(@NotNull final String name) {
        for (final Appstore appstore : availableAppstores) {
            if (name.equals(appstore.getAppstoreName())) {
                return appstore;
            }
        }
        return null;
    }

    @MagicConstant(intValues = {SETUP_DISPOSED, SETUP_IN_PROGRESS,
            SETUP_RESULT_FAILED, SETUP_RESULT_NOT_STARTED, SETUP_RESULT_SUCCESSFUL})
    public int getSetupState() {
        return setupState;
    }

    /**
     * Discovers a list of all available Open Stores.
     *
     * @return a list of all available Open Stores.
     */
    public
    @Nullable
    List<Appstore> discoverOpenStores() {
        if (Utils.uiThread()) {
            throw new IllegalStateException("Must not be called from UI thread");
        }

        final List<Appstore> openAppstores = new ArrayList<Appstore>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        discoverOpenStores(new OpenStoresDiscoveredListener() {
            @Override
            public void openStoresDiscovered(@NotNull final List<Appstore> appstores) {
                openAppstores.addAll(appstores);
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            return null;
        }
        return openAppstores;
    }

    /**
     * Discovers Open Stores.
     *
     * @param listener The callback to handle the result with a list of Open Stores
     */
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
            // Avoid leaking listener to annonimous ServiceConnection
            final OpenStoresDiscoveredListener[] listeners = new OpenStoresDiscoveredListener[]{listener};
            final ServiceConnection serviceConnection = new ServiceConnection() {

                @Override
                public void onServiceConnected(final ComponentName name, final IBinder service) {
                    if (listeners[0] != null) {
                        Appstore openAppstore = null;
                        try {
                            openAppstore = getOpenAppstore(name, service, this);
                        } catch (RemoteException exception) {
                            Logger.w("onServiceConnected() Error creating appsotre: ", exception);
                        }
                        if (openAppstore != null) {
                            appstores.add(openAppstore);
                        }
                        discoverOpenStores(listeners[0], bindServiceIntents, appstores);
                        listeners[0] = null;
                    }
                }

                @Override
                public void onServiceDisconnected(final ComponentName name) {
                    Logger.d("onServiceDisconnected(): ", name);
                }
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

    @NotNull
    @Deprecated
    /**
     * Use {@link #discoverOpenStores(OpenStoresDiscoveredListener)} or {@link #discoverOpenStores()} instead.
     */
    public static List<Appstore> discoverOpenStores(final Context context, final List<Appstore> dest, final Options options) {
        throw new UnsupportedOperationException("This action is no longer supported.");
    }

    private
    @NotNull
    List<ServiceInfo> queryOpenStoreServices() {
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
     * Must be called when setup is finished. See {@link #startSetup(OnIabSetupFinishedListener)}
     *
     * @return <code>null</code> if no app store connected, otherwise name of an app store OpenIAB has connected to.
     */
    public
    @Nullable
    String getConnectedAppstoreName() {
        if (appstore == null) return null;
        return appstore.getAppstoreName();
    }

    /**
     * Check options are valid. Google, Samsung and Nokia are supported.
     */
    public void checkOptions() {
        Logger.d("checkOptions() ", options);
        checkGoogle();
        checkSamsung();
        checkNokia();
        checkFortumo();
        checkAmazon();
    }

    private void checkNokia() {
        final boolean hasPermission = Utils.hasRequestedPermission(context, NokiaStore.NOKIA_BILLING_PERMISSION);
        Logger.d("checkNokia() has permission = ", hasPermission);
        if (hasPermission) {
            return;
        }
        if (options.getAvailableStoreByName(NAME_NOKIA) != null
                || options.getAvailableStoreNames().contains(NAME_NOKIA)
                || options.getPreferredStoreNames().contains(NAME_NOKIA)) {
            throw new IllegalStateException("Nokia permission \"" +
                    NokiaStore.NOKIA_BILLING_PERMISSION + "\" NOT REQUESTED");
        }
        Logger.d("checkNokia() ignoring Nokia wrapper");
        appStoreFactoryMap.remove(NAME_NOKIA);
    }

    private void checkSamsung() {
        Logger.d("checkSamsung() activity = ", activity);
        if (activity != null) {
            return;
        }
        if (options.getAvailableStoreByName(NAME_SAMSUNG) != null
                || options.getAvailableStoreNames().contains(NAME_SAMSUNG)
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
        appStoreFactoryMap.remove(NAME_SAMSUNG);
    }

    private void checkGoogle() {
        Logger.d("checkGoogle() verify mode = " + options.getVerifyMode());
        if (options.getVerifyMode() == VERIFY_SKIP) {
            return;
        }

        final boolean googleKeyProvided = options.getStoreKeys().containsKey(NAME_GOOGLE);
        Logger.d("checkGoogle() google key available = ", googleKeyProvided);
        if (googleKeyProvided) {
            return;
        }

        final boolean googleRequired = options.getAvailableStoreByName(NAME_GOOGLE) != null
                || options.getAvailableStoreNames().contains(NAME_GOOGLE)
                || options.getPreferredStoreNames().contains(NAME_GOOGLE);
        if (googleRequired && options.getVerifyMode() == VERIFY_EVERYTHING) {
            throw new IllegalStateException("You must supply Google verification key");
        }
        Logger.d("checkGoogle() ignoring GooglePlay wrapper.");
        appStoreFactoryMap.remove(NAME_GOOGLE);
    }

    private void checkFortumo() {
        boolean fortumoAvailable = false;
        try {
            final ClassLoader classLoader = OpenIabHelper.class.getClassLoader();
            classLoader.loadClass("mp.PaymentRequest");
            fortumoAvailable = true;
        } catch (ClassNotFoundException ignore) {
        }
        Logger.d("checkFortumo() fortumo sdk available: ", fortumoAvailable);
        if (fortumoAvailable) {
            return;
        }

        final boolean fortumoRequired = options.getAvailableStoreByName(NAME_FORTUMO) != null
                || options.getAvailableStoreNames().contains(NAME_FORTUMO)
                || options.getPreferredStoreNames().contains(NAME_FORTUMO);
        Logger.d("checkFortumo() fortumo billing required: ", fortumoRequired);
        if (fortumoRequired) {
            throw new IllegalStateException("You must satisfy fortumo sdk dependency.");
        }
        Logger.d("checkFortumo() ignoring fortumo wrapper.");
        appStoreFactoryMap.remove(NAME_FORTUMO);
    }

    private void checkAmazon() {
        // As of Amazon In-App 2.0.1 PurchasingService.getUserData() crashes on Android API 21
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            Logger.d("checkAmazon() Android Lollipop not supported, ignoring amazon wrapper.");
//            appStoreFactoryMap.remove(NAME_AMAZON);
//            return;
//        }

        boolean amazonAvailable = false;
        try {
            final ClassLoader classLoader = OpenIabHelper.class.getClassLoader();
            classLoader.loadClass("com.amazon.device.iap.PurchasingService");
            amazonAvailable = true;
        } catch (ClassNotFoundException ignore) {
        }
        Logger.d("checkAmazon() amazon sdk available: ", amazonAvailable);
        if (amazonAvailable) {
            return;
        }

        final boolean amazonRequired = options.getAvailableStoreByName(NAME_AMAZON) != null
                || options.getAvailableStoreNames().contains(NAME_AMAZON)
                || options.getPreferredStoreNames().contains(NAME_AMAZON);
        Logger.d("checkAmazon() amazon billing required: ", amazonRequired);
        if (amazonRequired) {
            throw new IllegalStateException("You must satisfy amazon sdk dependency.");
        }
        Logger.d("checkAmazon() ignoring amazon wrapper.");
        appStoreFactoryMap.remove(NAME_AMAZON);
    }

    /**
     * Connects to Billing Service of each store and request list of user purchases (inventory).
     * Can be used as a factor when looking for best fitting store.
     *
     * @param availableStores - list of stores to check
     * @return first found store with not empty inventory, null otherwise.
     */
    private
    @Nullable
    Appstore checkInventory(@NotNull final Set<Appstore> availableStores) {
        if (Utils.uiThread()) {
            throw new IllegalStateException("Must not be called from UI thread");
        }

        final Semaphore inventorySemaphore = new Semaphore(0);

        final Appstore[] inventoryAppstore = new Appstore[1];

        for (final Appstore appstore : availableStores) {
            final AppstoreInAppBillingService billingService = appstore.getInAppBillingService();
            final OnIabSetupFinishedListener listener = new OnIabSetupFinishedListener() {
                @Override
                public void onIabSetupFinished(@NotNull final IabResult result) {
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
            // startSetup() must be called from the UI thread
            handler.post(new Runnable() {
                @Override
                public void run() {
                    billingService.startSetup(listener);
                }
            });

            try {
                inventorySemaphore.acquire();
            } catch (InterruptedException exception) {
                Logger.e("checkInventory() Error during inventory check: ", exception);
                return null;
            }
            if (inventoryAppstore[0] != null) {
                return inventoryAppstore[0];
            }
        }

        return null;
    }

    public void dispose() {
        Logger.d("Disposing.");
        if (appStoreBillingService != null) {
            appStoreBillingService.dispose();
        }
        appstore = null;
        appStoreBillingService = null;
        activity = null;
        setupState = SETUP_DISPOSED;
    }

    public boolean subscriptionsSupported() {
        checkSetupDone("subscriptionsSupported");
        if (setupState != SETUP_RESULT_SUCCESSFUL) {
            throw new IllegalStateException("OpenIabHelper is not set up.");
        }
        return appStoreBillingService.subscriptionsSupported();
    }

    public void launchPurchaseFlow(Activity act, @NotNull String sku, int requestCode, IabHelper.OnIabPurchaseFinishedListener listener) {
        launchPurchaseFlow(act, sku, requestCode, listener, "");
    }

    public void launchPurchaseFlow(Activity act, @NotNull String sku, int requestCode,
                                   IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        launchPurchaseFlow(act, sku, ITEM_TYPE_INAPP, requestCode, listener, extraData);
    }

    public void launchSubscriptionPurchaseFlow(Activity act, @NotNull String sku, int requestCode,
                                               IabHelper.OnIabPurchaseFinishedListener listener) {
        launchSubscriptionPurchaseFlow(act, sku, requestCode, listener, "");
    }

    public void launchSubscriptionPurchaseFlow(Activity act, @NotNull String sku, int requestCode,
                                               IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        launchPurchaseFlow(act, sku, ITEM_TYPE_SUBS, requestCode, listener, extraData);
    }

    public void launchPurchaseFlow(Activity act, @NotNull String sku, String itemType, int requestCode,
                                   IabHelper.OnIabPurchaseFinishedListener listener, String extraData) {
        checkSetupDone("launchPurchaseFlow");
        appStoreBillingService.launchPurchaseFlow(act,
                SkuManager.getInstance().getStoreSku(appstore.getAppstoreName(), sku),
                itemType,
                requestCode,
                listener,
                extraData);
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        Logger.dWithTimeFromUp("handleActivityResult() requestCode: ", requestCode, " resultCode: ", resultCode, " data: ", data);
        if (requestCode == options.samsungCertificationRequestCode && appStoreInSetup != null) {
            return appStoreInSetup.getInAppBillingService().handleActivityResult(requestCode, resultCode, data);
        }
        if (setupState != SETUP_RESULT_SUCCESSFUL) {
            Logger.d("handleActivityResult() setup is not done. requestCode: ", requestCode, " resultCode: ", resultCode, " data: ", data);
            return false;
        }
        return appStoreBillingService.handleActivityResult(requestCode, resultCode, data);
    }

    /**
     * See {@link #queryInventory(boolean, List, List)} for details
     */
    public
    @Nullable
    Inventory queryInventory(final boolean querySkuDetails,
                             @Nullable final List<String> moreSkus)
            throws IabException {
        return queryInventory(querySkuDetails, moreSkus, null);
    }

    /**
     * Queries the inventory. This will query all owned items from the server, as well as
     * information on additional skus, if specified. This method may block or take long to execute.
     * Do not call from the UI thread. For that, use the non-blocking version {@link #queryInventoryAsync(boolean, java.util.List, java.util.List, org.onepf.oms.appstore.googleUtils.IabHelper.QueryInventoryFinishedListener)}.
     *
     * @param querySkuDetails if true, SKU details (price, description, etc) will be queried as well
     *                        as purchase information.
     * @param moreItemSkus    additional PRODUCT skus to query information on, regardless of ownership.
     *                        Ignored if null or if querySkuDetails is false.
     * @param moreSubsSkus    additional SUBSCRIPTIONS skus to query information on, regardless of ownership.
     *                        Ignored if null or if querySkuDetails is false.
     * @throws IabException if a problem occurs while refreshing the inventory.
     */
    @Nullable
    public Inventory queryInventory(final boolean querySkuDetails,
                                    @Nullable final List<String> moreItemSkus,
                                    @Nullable final List<String> moreSubsSkus)
            throws IabException {
        if (Utils.uiThread()) {
            throw new IllegalStateException("Must not be called from the UI thread");
        }
        final Appstore appstore = this.appstore;
        final AppstoreInAppBillingService appStoreBillingService = this.appStoreBillingService;
        if (setupState != SETUP_RESULT_SUCCESSFUL
                || appstore == null
                || appStoreBillingService == null) {
            return null;
        }

        final List<String> moreItemStoreSkus;
        final SkuManager skuManager = SkuManager.getInstance();
        if (moreItemSkus != null) {
            moreItemStoreSkus = new ArrayList<String>(moreItemSkus.size());
            for (String sku : moreItemSkus) {
                moreItemStoreSkus.add(skuManager.getStoreSku(appstore.getAppstoreName(), sku));
            }
        } else {
            moreItemStoreSkus = null;
        }

        final List<String> moreSubsStoreSkus;
        if (moreSubsSkus != null) {
            moreSubsStoreSkus = new ArrayList<String>(moreSubsSkus.size());
            for (String sku : moreSubsSkus) {
                moreSubsStoreSkus.add(skuManager.getStoreSku(appstore.getAppstoreName(), sku));
            }
        } else {
            moreSubsStoreSkus = null;
        }
        return appStoreBillingService.queryInventory(querySkuDetails, moreItemStoreSkus, moreSubsStoreSkus);
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
                    Logger.e("queryInventoryAsync() Error : ", exception);
                }

                final IabResult result_f = result;
                final Inventory inv_f = inv;
                handler.post(new Runnable() {
                    public void run() {
                        if (setupState == SETUP_RESULT_SUCCESSFUL) {
                            listener.onQueryInventoryFinished(result_f, inv_f);
                        }
                    }
                });
            }
        }).start();
    }

    public void consume(@NotNull Purchase purchase) throws IabException {
        final Appstore appstore = this.appstore;
        final AppstoreInAppBillingService appStoreBillingService = this.appStoreBillingService;
        if (setupState != SETUP_RESULT_SUCCESSFUL
                || appstore == null
                || appStoreBillingService == null) {
            return;
        }
        Purchase purchaseStoreSku = (Purchase) purchase.clone(); // TODO: use Purchase.getStoreSku()
        purchaseStoreSku.setSku(SkuManager.getInstance().getStoreSku(appstore.getAppstoreName(), purchase.getSku()));
        appStoreBillingService.consume(purchaseStoreSku);
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
                              @Nullable final IabHelper.OnConsumeFinishedListener consumeListener,
                              @Nullable final IabHelper.OnConsumeMultiFinishedListener consumeMultiListener) {
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

                if (consumeListener != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            if (setupState == SETUP_RESULT_SUCCESSFUL) {
                                consumeListener.onConsumeFinished(purchases.get(0), results.get(0));
                            }
                        }
                    });
                }
                if (consumeMultiListener != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            if (setupState == SETUP_RESULT_SUCCESSFUL) {
                                consumeMultiListener.onConsumeMultiFinished(purchases, results);
                            }
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
        } else if (setupState == SETUP_IN_PROGRESS) {
            state = "IAB helper setup is in progress.";
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
        enableDebuglLogging(enabled, null);
    }

    /**
     * @deprecated Use {@link org.onepf.oms.util.Logger#setLoggable(boolean)}. Param 'tag' no effect.
     * <p/>
     * Will be removed in version 1.0.
     */
    public static void enableDebuglLogging(boolean enabled, String tag) {
        Logger.setLogTag(tag);
        Logger.setLoggable(enabled);
    }

    public interface OnInitListener {
        void onInitFinished();
    }

    public interface OnOpenIabHelperInitFinished {
        void onOpenIabHelperInitFinished();
    }

    public interface OpenStoresDiscoveredListener {
        void openStoresDiscovered(@NotNull List<Appstore> appStores);
    }

    /**
     * Contains dev settings for OpenIAB.
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


        /**
         * Look for package installer and try to use it as a billing provider.
         */
        public static final int SEARCH_STRATEGY_INSTALLER = 0;

        /**
         * Look among available billing providers and select one that fits best.
         *
         * @see #getPreferredStoreNames()
         * @see #getAvailableStores()
         */
        public static final int SEARCH_STRATEGY_BEST_FIT = 1;

        /**
         * If package installer is not available or is not suited for billing, look for the best fit store.
         *
         * @see #SEARCH_STRATEGY_INSTALLER
         * @see #SEARCH_STRATEGY_BEST_FIT
         */
        public static final int SEARCH_STRATEGY_INSTALLER_THEN_BEST_FIT = 2;


        /**
         * @deprecated Use {@link #getAvailableStores()}
         * Will be private since 1.0.
         */
        public final Set<Appstore> availableStores;

        private final Set<String> availableStoreNames;

        /**
         * @deprecated Use {@link #getPreferredStoreNames()}
         * Will be private since 1.0.
         */
        public final Set<String> preferredStoreNames;

        /**
         * @deprecated No longer used.
         */
        public final int discoveryTimeoutMs = 0;

        /**
         * @deprecated Use {@link #isCheckInventory()}
         * Will be private since 1.0.
         */
        public final boolean checkInventory;

        /**
         * @deprecated No longer used.
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
            this.availableStoreNames = Collections.emptySet();
            this.storeKeys = Collections.emptyMap();
            this.preferredStoreNames = Collections.emptySet();
            this.verifyMode = VERIFY_SKIP;
            this.samsungCertificationRequestCode = SamsungAppsBillingService.REQUEST_CODE_IS_ACCOUNT_CERTIFICATION;
            this.storeSearchStrategy = SEARCH_STRATEGY_INSTALLER;
        }

        private Options(final Set<Appstore> availableStores,
                        final Set<String> availableStoresNames,
                        final Map<String, String> storeKeys,
                        final boolean checkInventory,
                        final @MagicConstant(intValues = {VERIFY_EVERYTHING, VERIFY_ONLY_KNOWN, VERIFY_SKIP}) int verifyMode,
                        final Set<String> preferredStoreNames,
                        final int samsungCertificationRequestCode,
                        final int storeSearchStrategy) {
            this.checkInventory = checkInventory;
            this.availableStores = availableStores;
            this.availableStoreNames = availableStoresNames;
            this.storeKeys = storeKeys;
            this.preferredStoreNames = preferredStoreNames;
            this.verifyMode = verifyMode;
            this.samsungCertificationRequestCode = samsungCertificationRequestCode;
            this.storeSearchStrategy = storeSearchStrategy;
        }

        /**
         * @return {@link org.onepf.oms.OpenIabHelper.Options.Builder#samsungCertificationRequestCode}
         */
        public int getSamsungCertificationRequestCode() {
            return samsungCertificationRequestCode;
        }

        /**
         * Returns current verify strategy value.
         *
         * @return The current verify mode value.
         * @see Builder#setVerifyMode(int)
         */
        @MagicConstant(intValues = {VERIFY_EVERYTHING, VERIFY_ONLY_KNOWN, VERIFY_SKIP})
        public int getVerifyMode() {
            return verifyMode;
        }

        /**
         * Set strategy to help OpenIAB chose the correct billing provider.
         * <p/>
         *
         * @see Builder#setStoreSearchStrategy(int)
         */
        @MagicConstant(intValues = {SEARCH_STRATEGY_INSTALLER, SEARCH_STRATEGY_BEST_FIT, SEARCH_STRATEGY_INSTALLER_THEN_BEST_FIT})
        public int getStoreSearchStrategy() {
            return storeSearchStrategy;
        }

        /**
         * @return return {@link org.onepf.oms.OpenIabHelper.Options.Builder#checkInventory} value
         */
        public boolean isCheckInventory() {
            return checkInventory;
        }

        /**
         * @deprecated No longer used.
         */
        @Deprecated
        public long getCheckInventoryTimeout() {
            return 0;
        }

        /**
         * @deprecated No longer used.
         */
        @Deprecated
        public long getDiscoveryTimeout() {
            return 0;
        }

        /**
         * @return a list of objects of available stores.
         * @see Builder#addAvailableStores(java.util.Collection)
         */
        public
        @NotNull
        Set<Appstore> getAvailableStores() {
            return availableStores;
        }

        /**
         * @return list of available stores names.
         * @see Builder#addAvailableStoreNames(java.util.Collection)
         */
        public Set<String> getAvailableStoreNames() {
            return availableStoreNames;
        }

        /**
         * Returns the preferred store names.
         *
         * @return The set of the preferred store names.
         */
        public
        @NotNull
        Set<String> getPreferredStoreNames() {
            return preferredStoreNames;
        }

        /**
         * Returns the store key map [app store name -> publicKeyBase64]. Only for Open Stores and Google Play
         *
         * @return The store key map
         */
        @NotNull
        public Map<String, String> getStoreKeys() {
            return storeKeys;
        }

        /**
         * Searches a store by the name among available stores.
         *
         * @param name The store name to search for.
         * @return {@link Appstore} with the name if one is found, null otherwise.
         * @see #getAvailableStores()
         * @see Builder#addAvailableStores(java.util.Collection)
         */
        public
        @Nullable
        Appstore getAvailableStoreByName(@NotNull final String name) {
            for (Appstore s : availableStores) {
                if (name.equals(s.getAppstoreName())) {
                    return s;
                }
            }
            return null;
        }

        /**
         * Builder class for {@link Options}.
         */
        public static final class Builder {

            private final Set<String> preferredStoreNames = new LinkedHashSet<String>();
            private final Set<Appstore> availableStores = new HashSet<Appstore>();
            private final Set<String> availableStoresNames = new LinkedHashSet<String>();
            private final Map<String, String> storeKeys = new HashMap<String, String>();
            private boolean checkInventory = false;
            private int samsungCertificationRequestCode
                    = SamsungAppsBillingService.REQUEST_CODE_IS_ACCOUNT_CERTIFICATION;

            @MagicConstant(intValues = {VERIFY_EVERYTHING, VERIFY_ONLY_KNOWN, VERIFY_SKIP})
            private int verifyMode = VERIFY_EVERYTHING;

            @MagicConstant(intValues = {SEARCH_STRATEGY_INSTALLER, SEARCH_STRATEGY_BEST_FIT, SEARCH_STRATEGY_INSTALLER_THEN_BEST_FIT})
            private int storeSearchStrategy = SEARCH_STRATEGY_INSTALLER;

            /**
             * Same as {@link #addAvailableStores(java.util.Collection)}
             *
             * @param stores The store(s) to be added to the available ones.
             * @see Options#getAvailableStores()
             */
            @NotNull
            public Builder addAvailableStores(@NotNull final Appstore... stores) {
                addAvailableStores(Arrays.asList(stores));
                return this;
            }

            /**
             * If set, only the available stores and stores set by {@link #addAvailableStoreNames(java.util.Collection)} are checked for the billing.
             * No other stores are considered.
             *
             * @param stores The stores to be added to the available ones.
             * @see Options#getAvailableStores()
             */
            @NotNull
            public Builder addAvailableStores(@NotNull final Collection<Appstore> stores) {
                this.availableStores.addAll(stores);
                return this;
            }

            /**
             * Same as {@link #addAvailableStoreNames(java.util.Collection)}
             *
             * @param storesNames Store names to be added to available ones.
             * @see Options#getAvailableStoreNames()
             */
            @NotNull
            public Builder addAvailableStoreNames(@NotNull final String... storesNames) {
                addAvailableStoreNames(Arrays.asList(storesNames));
                return this;
            }

            /**
             * If set, only stores specified by name and stores set buy {@link #addAvailableStores(java.util.Collection)} are checked for the billing.
             * No other stores are considered.
             *
             * @param storesNames Store names to be added to available ones.
             * @see Options#getAvailableStoreNames()
             */
            @NotNull
            public Builder addAvailableStoreNames(@NotNull final Collection<String> storesNames) {
                this.availableStoresNames.addAll(storesNames);
                return this;
            }

            /**
             * Sets the option to check the inventory of stores during the setup, false by default.
             * If true, a store with purchases will have the priority.
             *
             * @param checkInventory Check store inventory during the setup process.
             * @see Options#isCheckInventory()
             */
            @NotNull
            public Builder setCheckInventory(final boolean checkInventory) {
                this.checkInventory = checkInventory;
                return this;
            }

            /**
             * Sets the discovery timeout for the setup process.
             *
             * @deprecated No longer used.
             */
            @NotNull
            @Deprecated
            public Builder setDiscoveryTimeout(final int discoveryTimeout) {
                return this;
            }

            /**
             * Sets the check inventory timeout for the setup process.
             *
             * @param checkInventoryTimeout The ms timeout for inventory checking.
             * @deprecated No longer used.
             */
            @NotNull
            @Deprecated
            public Builder setCheckInventoryTimeout(final int checkInventoryTimeout) {
                return this;
            }

            /**
             * Adds a store key to the map {@link org.onepf.oms.OpenIabHelper.Options.Builder#storeKeys} of the internal Options object.
             *
             * @param storeName The name (not package!) of the store. E.g. {@link org.onepf.oms.OpenIabHelper#NAME_AMAZON}
             * @param publicKey The key of the store.
             * @throws java.lang.IllegalArgumentException If the public key doesn't match base64 format.
             * @see Options#getStoreKeys()
             */
            @NotNull
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
             * Adds store key map as {@link org.onepf.oms.OpenIabHelper.Options.Builder#storeKeys} to the internal Options object.<br/>
             * Stores that support RSA keys: any Open Store (Appland, Aptoide, Yandex.Store, SlideMe, etc) and Google Play.
             *
             * @param storeKeys The map [store name -> store RSA public key].
             * @throws java.lang.IllegalArgumentException If a key in the map doesn't match base64 format.
             * @see Options.Builder#addStoreKeys(java.util.Map)
             * @see Options#getStoreKeys()
             */
            @NotNull
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
             * Sets the verify mode for purchases. By default sets to {@link Options#VERIFY_EVERYTHING}.
             *
             * @param verifyMode The verify mode for stores. Must be one of {@link Options#VERIFY_EVERYTHING},
             *                   {@link Options#VERIFY_SKIP},
             *                   {@link Options#VERIFY_ONLY_KNOWN}.
             * @see Options#getVerifyMode()
             */
            @NotNull
            public Builder setVerifyMode(
                    final @MagicConstant(intValues = {
                            VERIFY_EVERYTHING,
                            VERIFY_ONLY_KNOWN,
                            VERIFY_SKIP}) int verifyMode) {
                this.verifyMode = verifyMode;
                return this;
            }

            /**
             * Sets up the store search strategy.
             *
             * @param storeSearchStrategy The store search strategy for OpenIAB.
             *                            Must be one of {@link #SEARCH_STRATEGY_INSTALLER}, {@link #SEARCH_STRATEGY_BEST_FIT} or {@link #SEARCH_STRATEGY_INSTALLER_THEN_BEST_FIT}
             * @see Options#getStoreSearchStrategy()
             */
            @NotNull
            public Builder setStoreSearchStrategy(
                    final @MagicConstant(intValues = {
                            SEARCH_STRATEGY_INSTALLER,
                            SEARCH_STRATEGY_BEST_FIT,
                            SEARCH_STRATEGY_INSTALLER_THEN_BEST_FIT}) int storeSearchStrategy) {
                this.storeSearchStrategy = storeSearchStrategy;
                return this;
            }

            /**
             * Adds stores to {@link org.onepf.oms.OpenIabHelper.Options.Builder#preferredStoreNames} of the internal Options object.
             * Selection priority is given in the order that the stores were added.
             *
             * @param storeNames The names - not packages! - of the preferred stores. E.g. {@link org.onepf.oms.OpenIabHelper#NAME_APPLAND}.
             * @see #addPreferredStoreName(java.util.Collection)
             * @see Options#getPreferredStoreNames()
             */
            @NotNull
            public Builder addPreferredStoreName(@NotNull final String... storeNames) {
                addPreferredStoreName(Arrays.asList(storeNames));
                return this;
            }

            /**
             * Adds a collection of stores as {@link org.onepf.oms.OpenIabHelper.Options.Builder#preferredStoreNames} to the internal Options object.
             * Selection priority is given in the order that the stores were added.
             *
             * @param storeNames The names(not packages!) of preferred stores. E.g. {@link org.onepf.oms.OpenIabHelper#NAME_APPLAND}.
             * @see Options#getPreferredStoreNames()
             */
            @NotNull
            public Builder addPreferredStoreName(@NotNull final Collection<String> storeNames) {
                this.preferredStoreNames.addAll(storeNames);
                return this;
            }

            /**
             * Sets a request code for the Samsung Certification Activity.
             * Samsung UI will be shown when the user is not logged in.
             *
             * @param code The request code. Must be positive value.
             * @throws java.lang.IllegalArgumentException if the code is not a positive integer.
             * @see Options#getSamsungCertificationRequestCode()
             */
            @NotNull
            public Builder setSamsungCertificationRequestCode(int code) {
                if (code <= 0) {
                    throw new IllegalArgumentException("Value '" + code +
                            "' can't be request code. Request code must be a positive value.");
                }

                this.samsungCertificationRequestCode = code;
                return this;
            }

            /**
             * Creates an instance of {@link Options}.
             *
             * @return new instance of {@link Options}.
             */
            @NotNull
            public Options build() {
                return new Options(
                        Collections.unmodifiableSet(availableStores),
                        Collections.unmodifiableSet(availableStoresNames),
                        Collections.unmodifiableMap(storeKeys),
                        checkInventory,
                        verifyMode,
                        Collections.unmodifiableSet(preferredStoreNames),
                        samsungCertificationRequestCode,
                        storeSearchStrategy);
            }
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("{availableStores=")
                    .append(availableStores)
                    .append(", availableStoreNames=")
                    .append(availableStoreNames)
                    .append(", preferredStoreNames=")
                    .append(preferredStoreNames)
                    .append(", discoveryTimeoutMs=")
                    .append(discoveryTimeoutMs)
                    .append(", checkInventory=")
                    .append(checkInventory)
                    .append(", checkInventoryTimeoutMs=")
                    .append(checkInventoryTimeoutMs)
                    .append(", verifyMode=")
                    .append(verifyMode)
                    .append(", storeSearchStrategy=")
                    .append(storeSearchStrategy)
                    .append(", storeKeys=[");
            final StringBuilder storeKeysBuilder = new StringBuilder();
            for (final Map.Entry<String, String> entry : storeKeys.entrySet()) {
                if (!TextUtils.isEmpty(entry.getValue())) {
                    if (!TextUtils.isEmpty(storeKeysBuilder.toString())) {
                        storeKeysBuilder.append(", ");
                    }
                    storeKeysBuilder.append(entry.getKey());
                }
            }
            builder.append(storeKeysBuilder)
                    .append("]\n, samsungCertificationRequestCode=")
                    .append(samsungCertificationRequestCode)
                    .append('}');
            return builder.toString();
        }
    }

}
