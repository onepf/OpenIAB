/*******************************************************************************
 * Copyright 2013 One Platform Foundation
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 ******************************************************************************/

package org.onepf.oms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.onepf.oms.appstore.AmazonAppstore;
import org.onepf.oms.appstore.GooglePlay;
import org.onepf.oms.appstore.OpenAppstore;
import org.onepf.oms.appstore.SamsungApps;
import org.onepf.oms.appstore.SamsungAppsBillingService;
import org.onepf.oms.appstore.TStore;
import org.onepf.oms.appstore.googleUtils.IabException;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabHelper.OnIabPurchaseFinishedListener;
import org.onepf.oms.appstore.googleUtils.IabHelper.OnIabSetupFinishedListener;
import org.onepf.oms.appstore.googleUtils.IabHelper.QueryInventoryFinishedListener;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.appstore.googleUtils.Purchase;
import org.onepf.oms.appstore.googleUtils.Security;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * 
 * 
 * @author Boris Minaev, Oleg Orlov
 * @since 16.04.13
 */
public class OpenIabHelper {
    private static final String TAG = OpenIabHelper.class.getSimpleName();
    // Is debug logging enabled?
    private static final boolean mDebugLog = false;
    
    private static final String BIND_INTENT = "org.onepf.oms.openappstore.BIND";
    
    /** */
    private static final int DISCOVER_TIMEOUT_MS = 5000;
    
    /** 
     * for generic stores it takes 1.5 - 3sec
     * <p>
     * SamsungApps initialization is very time consuming (from 4 to 12 seconds). 
     * TODO: Optimize: ~1sec is consumed for check account certification via account activity + ~3sec for actual setup
     */
    private static final int INVENTORY_CHECK_TIMEOUT_MS = 10000;
    
    private final Context context;
    
    private Handler notifyHandler = null;
    
    /** selected appstore */
    private Appstore mAppstore;

    /** selected appstore billing service */
    private AppstoreInAppBillingService mAppstoreBillingService;
    
    private final Options options;

    private static final int SETUP_RESULT_NOT_STARTED = -1;
    private static final int SETUP_RESULT_SUCCESSFUL = 0;
    private static final int SETUP_RESULT_FAILED = 1;
    private static final int SETUP_DISPOSED = 2;
    private int setupState = SETUP_RESULT_NOT_STARTED;
    
    /** SamsungApps requires {@link #handleActivityResult(int, int, Intent)} but it doesn't 
     *  work until setup is completed. */
    private volatile SamsungApps samsungInSetup;

    /** used to track time used for {@link #startSetup(OnIabSetupFinishedListener)} 
     * TODO: think about smarter time tracker (i.e. Logger built-in) */
    private volatile static long started;
    
    // Is an asynchronous operation in progress?
    // (only one at a time can be in progress)
    private boolean mAsyncInProgress = false;

    // (for logging/debugging)
    // if mAsyncInProgress == true, what asynchronous operation is in progress?
    private String mAsyncOperation = "";

    // The request code used to launch purchase flow
    int mRequestCode;

    // The item type of the current purchase flow
    String mPurchasingItemType;
    
    // Item types
    public static final String ITEM_TYPE_INAPP = "inapp";
    public static final String ITEM_TYPE_SUBS = "subs";

    // Billing response codes
    public static final int BILLING_RESPONSE_RESULT_OK = 0;
    public static final int BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3;
    public static final int BILLING_RESPONSE_RESULT_ERROR = 6;
        
    public static final String NAME_GOOGLE = "com.google.play";
    public static final String NAME_AMAZON = "com.amazon.apps";
    public static final String NAME_TSTORE = "com.tmobile.store";
    public static final String NAME_SAMSUNG = "com.samsung.apps";

    /** 
     * NOTE: used as sync object in related methods<br>
     * 
     * storeName -> [ ... {app_sku1 -> store_sku1}, ... ]
     */
    private static final Map <String, Map<String, String>> sku2storeSkuMappings = new HashMap<String, Map <String, String>>();

    /** 
     * storeName -> [ ... {store_sku1 -> app_sku1}, ... ]
     */
    private static final Map <String, Map<String, String>> storeSku2skuMappings = new HashMap<String, Map <String, String>>();
        
    /**
     * Map sku and storeSku for particular store. 
     * <p>
     * The best approach is to use SKU that unique in universe like <code>com.companyname.application.item</code>.
     * Such SKU fit most of stores so it doesn't need to be mapped.
     * <p>
     * If best approach is not applicable use application inner SKU in code (usually it is SKU for Google Play)
     * and map SKU from other stores using this method. OpenIAB will map SKU in both directions, 
     * so you can use only your inner SKU
     * 
     * @param sku - application inner SKU
     * @param storeSku - shouldn't duplicate already mapped values
     * @param storeName - @see {@link IOpenAppstore#getAppstoreName()} or {@link #NAME_AMAZON} {@link #NAME_GOOGLE} {@link #NAME_TSTORE}
     */
    public static void mapSku(String sku, String storeName, String storeSku) {
        synchronized (sku2storeSkuMappings) {
            Map<String, String> skuMap = sku2storeSkuMappings.get(storeName);
            if (skuMap == null) {
                skuMap = new HashMap<String, String>();
                sku2storeSkuMappings.put(storeName, skuMap);
            }
            if (skuMap.get(sku) != null) {
                throw new IllegalArgumentException("Already specified SKU. sku: " + sku + " -> storeSku: " + skuMap.get(sku));
            }
            ;
            Map<String, String> storeSkuMap = storeSku2skuMappings.get(storeName);
            if (storeSkuMap == null) {
                storeSkuMap = new HashMap<String, String>();
                storeSku2skuMappings.put(storeName, storeSkuMap);
            }
            if (storeSkuMap.get(storeSku) != null) {
                throw new IllegalArgumentException("Ambigous SKU mapping. You try to map sku: " + sku + " -> storeSku: " + storeSku + ", that is already mapped to sku: " + storeSkuMap.get(storeSku));
            }
            skuMap.put(sku, storeSku);
            storeSkuMap.put(storeSku, sku);
        }
    }
    
    /**
     * Return previously mapped store SKU for specified inner SKU
     * @see #mapSku(String, String, String)
     * 
     * @param appstoreName
     * @param sku - inner SKU
     * @return SKU used in store for specified inner SKU
     */
    public static String getStoreSku(final String appstoreName, String sku) {
        synchronized (sku2storeSkuMappings) {
            String currentStoreSku = sku;
            Map<String, String> skuMap = sku2storeSkuMappings.get(appstoreName);
            if (skuMap != null && skuMap.get(sku) != null) {
                currentStoreSku = skuMap.get(sku);
                if (mDebugLog) Log.d(TAG, "getStoreSku() using mapping for sku: " + sku + " -> " + currentStoreSku);
            }
            return currentStoreSku;
        }
    }
    
    /** 
     * Return mapped application inner SKU using store name and store SKU. 
     * @see #mapSku(String, String, String)
     */
    public static String getSku(final String appstoreName, String storeSku) {
        synchronized (sku2storeSkuMappings) {
            String sku = storeSku;
            Map<String, String> skuMap = storeSku2skuMappings.get(appstoreName);
            if (skuMap != null && skuMap.get(sku) != null) {
                sku = skuMap.get(sku);
                if (mDebugLog) Log.d(TAG, "getSku() restore sku from storeSku: " + storeSku + " -> " + sku);
            }
            return sku;
        }
    }

    /**
     * @param appstoreName for example {@link OpenIabHelper#NAME_AMAZON}
     * @return list of skus those have mappings for specified appstore 
     */
    public static List<String> getAllStoreSkus(final String appstoreName) {
        Map<String, String> skuMap = sku2storeSkuMappings.get(appstoreName);
        List<String> result = new ArrayList<String>();
        if (skuMap != null) {
            result.addAll(skuMap.values());
        }
        return result;
    }

    /**
     * Simple constructor for OpenIabHelper. 
     * <p>See {@link OpenIabHelper#OpenIabHelper(Context, Options)} for details
     * 
     * @param storeKeys - see {@link Options#storeKeys}
     * @param context - if you want to support Samsung Apps you must pass an Activity, in other cases any context is acceptable
     */
    public OpenIabHelper(Context context, Map<String, String> storeKeys) {
        this(context, storeKeys, null);
    }
    
    /**
     * Simple constructor for OpenIabHelper. 
     * <p>See {@link OpenIabHelper#OpenIabHelper(Context, Options)} for details
     * 
     * @param storeKeys - see {@link Options#storeKeys}
     * @param prefferedStores - see {@link Options#prefferedStoreNames}
     * @param context - if you want to support Samsung Apps you must pass an Activity, in other cases any context is acceptable
     */
    public OpenIabHelper(Context context, Map<String, String> storeKeys, String[] prefferedStores) {
        this(context, storeKeys, prefferedStores, null);
    }
    
    /**
     * Simple constructor for OpenIabHelper. 
     * <p>See {@link OpenIabHelper#OpenIabHelper(Context, Options)} for details
     * 
     * @param storeKeys - see {@link Options#storeKeys}
     * @param prefferedStores - see {@link Options#prefferedStoreNames}
     * @param availableStores - see {@link Options#availableStores}
     * @param context - if you want to support Samsung Apps you must pass an Activity, in other cases any context is acceptable
     */
    public OpenIabHelper(Context context, Map<String, String> storeKeys, String[] prefferedStores, Appstore[] availableStores) {
        this.context = context;
        this.options = new Options();
        
        options.storeKeys = storeKeys;
        options.prefferedStoreNames = prefferedStores != null ? prefferedStores : options.prefferedStoreNames;
        options.availableStores = availableStores != null ? new ArrayList<Appstore>(Arrays.asList(availableStores)) : null;
        
        checkSettings(options, context);
    }

    /**
     * Before start ensure you already have <li>
     * - permission <code>org.onepf.openiab.permission.BILLING</code> in your AndroidManifest.xml<li>
     * - publicKey for store you decided to work with (you can find it in Developer Console of your store)<li> 
     * - map SKUs for your store if they differs using {@link #mapSku(String, String, String)}</li>
     * 
     * <p>
     * You can specify publicKeys for stores (excluding Amazon and SamsungApps those don't use 
     * verification based on RSA keys). See {@link Options#storeKeys} for details
     * <p>
     * By default verification will be performed for receipt from every store. To aviod verification 
     * exception OpenIAB doesn't connect to store that key is not specified for
     * <p>
     * If you don't want to put publicKey in code and verify receipt remotely, you need to set
     * {@link Options#verifyMode} to {@link Options#VERIFY_SKIP}.
     * To make OpenIAB connect even to stores key is not specified for, use {@link Options#VERIFY_ONLY_KNOWN} 
     * <p> 
     * {@link Options#prefferedStoreNames} is useful option when you test your app on device with multiple 
     * stores installed. Specify store name you want to work with here and it would be selected if you 
     * install application using adb.
     * 
     * @param options - specify all necessary options
     * @param context - if you want to support Samsung Apps you must pass an Activity, in other cases any context is acceptable
     */
    public OpenIabHelper(Context context, Options options) {
        this.context = context;
        this.options = options;
        
        checkSettings(options, context);
    }

    /**
     *  Discover available stores and select the best billing service. 
     *  Calls listener when service is found.
     *  
     *  Should be called from UI thread
     */
    public void startSetup(final IabHelper.OnIabSetupFinishedListener listener) {
        if (listener == null){
            throw new IllegalArgumentException("Setup listener must be not null!");
        }
        if (setupState != SETUP_RESULT_NOT_STARTED) {
            String state = setupStateToString(setupState);
            throw new IllegalStateException("Couldn't be set up. Current state: " + state);
        }
        this.notifyHandler = new Handler();        
        started = System.currentTimeMillis();
        new Thread(new Runnable() {
            public void run() {
                List<Appstore> stores2check = new ArrayList<Appstore>();
                if (options.availableStores != null) {
                    stores2check.addAll(options.availableStores);
                } else { // if appstores are not specified by user - lookup for all available stores
                    final List<Appstore> openStores = discoverOpenStores(context, null, options);
                    if (mDebugLog) Log.d(TAG, in() + " " + "startSetup() discovered openstores: " + openStores.toString());
                    stores2check.addAll(openStores);
                    if (options.verifyMode == Options.VERIFY_EVERYTHING && !options.storeKeys.containsKey(NAME_GOOGLE)) {
                        // don't work with GooglePlay if verifyMode is strict and no publicKey provided 
                    } else {
                        final String publicKey = options.verifyMode == Options.VERIFY_SKIP ? null 
                                : options.storeKeys.get(OpenIabHelper.NAME_GOOGLE);
                        stores2check.add(new GooglePlay(context, publicKey));
                    }
                    stores2check.add(new AmazonAppstore(context));
                    stores2check.add(new TStore(context, options.storeKeys.get(OpenIabHelper.NAME_TSTORE)));
                    if (getAllStoreSkus(NAME_SAMSUNG).size() > 0) {  
                        // SamsungApps shows lot of UI stuff during init 
                        // try it only if samsung SKUs are specified
                        stores2check.add(new SamsungApps((Activity) context, options));
                    }
                }
                
                for (Appstore store : stores2check) {
                    if (store instanceof SamsungApps) samsungInSetup = (SamsungApps) store;
                }
                
                IabResult result = new IabResult(BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE, "Billing isn't supported");
                
                if (options.checkInventory) {
                    
                    final List<Appstore> equippedStores = checkInventory(stores2check);
                    
                    if (equippedStores.size() > 0) {
                        mAppstore = selectBillingService(equippedStores);
                        if (mDebugLog) Log.d(TAG, in() + " " + "select equipped");
                    } 
                    if (mAppstore != null) {
                        result = new IabResult(BILLING_RESPONSE_RESULT_OK, "Successfully initialized with existing inventory: " + mAppstore.getAppstoreName());
                    } else {
                        // found no equipped stores. Select store based on store parameters 
                        mAppstore = selectBillingService(stores2check);
                        if (mDebugLog) Log.d(TAG, in() + " " + "select non-equipped");
                    }
                    if (mAppstore != null) {
                        result = new IabResult(BILLING_RESPONSE_RESULT_OK, "Successfully initialized: " + mAppstore.getAppstoreName());
                        mAppstoreBillingService = mAppstore.getInAppBillingService();
                    }
                    fireSetupFinished(listener, result);
                } else {                // no inventory check. Select store based on store parameters   
                    mAppstore = selectBillingService(stores2check);
                    if (mAppstore != null) {
                        mAppstoreBillingService = mAppstore.getInAppBillingService(); 
                        mAppstoreBillingService.startSetup(new OnIabSetupFinishedListener() {
                            public void onIabSetupFinished(IabResult result) {
                                fireSetupFinished(listener, result);
                            }
                        });
                    } else {
                        fireSetupFinished(listener, result);
                    }
                }
                for (Appstore store : stores2check) {
                    if (store != mAppstore && store.getInAppBillingService() != null) {
                        store.getInAppBillingService().dispose();
                        if (mDebugLog) Log.d(TAG, in() + " " + "startSetup() disposing " + store.getAppstoreName());
                    }
                }
            }
        }, "openiab-setup").start();
    }

    /** Check options are valid */
    public static void checkOptions(Options options) {
        if (options.verifyMode != Options.VERIFY_SKIP && options.storeKeys != null) { // check publicKeys. Must be not null and valid
            for (Entry<String, String> entry : options.storeKeys.entrySet()) {
                if (entry.getValue() == null) {
                    throw new IllegalArgumentException("Null publicKey for store: " + entry.getKey() + ", key: " + entry.getValue());
                }
                try {
                    Security.generatePublicKey(entry.getValue());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid publicKey for store: " + entry.getKey() + ", key: " + entry.getValue(), e);
                }
            }
        }
    }

    private static void checkSettings(Options options, Context context){
        checkOptions(options);
        checkSamsung(context);
    }

    private static void checkSamsung(Context context) {
        List<String> allStoreSkus = getAllStoreSkus(OpenIabHelper.NAME_SAMSUNG);
        if (!allStoreSkus.isEmpty()) { // it means that Samsung is among the candidates
            for (String sku : allStoreSkus) {
                SamsungApps.checkSku(sku);
            }
            if (!(context instanceof Activity)) {
                //
                // Unfortunately, SamsungApps requires to launch their own "Certification Activity"
                // in order to connect to billing service. So it's also needed for OpenIAB.
                //
                // Because of SKU for SamsungApps are specified,
                // intance of Activity needs to be passed to OpenIAB constructor to launch
                // Samsung Cerfitication Activity.
                // Activity also need to pass activityResult to OpenIABHelper.handleActivityResult()
                //
                //
                throw new IllegalArgumentException(
                                  "\n "
                                + "\nContext is not instance of Activity."
                                + "\nUnfortunately, SamsungApps requires to launch their own Certification Activity "
                                + "\nin order to connect to billing service. So it's also needed for OpenIAB."                                
                                + "\n "
                                + "\nBecause of SKU for SamsungApps are specified, instance of Activity needs to be passed "
                                + "\nto OpenIAB constructor to launch Samsung Cerfitication Activity."
                                + "\nActivity should call OpenIabHelper#handleActivityResult()."
                                + "\n ");
            }
        }
    }

    protected void fireSetupFinished(final IabHelper.OnIabSetupFinishedListener listener, final IabResult result) {
        if (setupState == SETUP_DISPOSED) return;
        if (mDebugLog) Log.d(TAG, in() + " " + "fireSetupFinished() === SETUP DONE === result: " + result
            + (mAppstore != null ? ", appstore: " + mAppstore.getAppstoreName() : ""));
        
        samsungInSetup = null;
        setupState = result.isSuccess() ? SETUP_RESULT_SUCCESSFUL : SETUP_RESULT_FAILED;
        notifyHandler.post(new Runnable() {
           public void run() { 
               listener.onIabSetupFinished(result);
           }
        });
    }

    /**
     * Discover all OpenStore services, checks them and build {@link #availableStores} list<br>.
     * Time is limited by 5 seconds  
     * 
     * @param appstores - discovered OpenStores will be added here. Must be not null
     * @param listener - called back when all OpenStores collected and analyzed
     */
    public static List<Appstore> discoverOpenStores(final Context context, final List<Appstore> dest, final Options options) {
        PackageManager packageManager = context.getPackageManager();
        final Intent intentAppstoreServices = new Intent(BIND_INTENT);
        List<ResolveInfo> infoList = packageManager.queryIntentServices(intentAppstoreServices, 0);
        final List<Appstore> result = dest != null ? dest : new ArrayList<Appstore>(infoList.size());

        final CountDownLatch storesToCheck = new CountDownLatch(infoList.size());
        for (ResolveInfo info : infoList) {
            String packageName = info.serviceInfo.packageName;
            String name = info.serviceInfo.name;
            Intent intentAppstore = new Intent(intentAppstoreServices);
            intentAppstore.setClassName(packageName, name);
            context.bindService(intentAppstore, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    if (mDebugLog) Log.d(TAG, "discoverOpenStores() appstoresService connected for component: " + name.flattenToShortString());
                    IOpenAppstore openAppstoreService = IOpenAppstore.Stub.asInterface(service);

                    try {
                        String appstoreName = openAppstoreService.getAppstoreName();
                        Intent billingIntent = openAppstoreService.getBillingServiceIntent();
                        if (appstoreName == null) { // no name - no service
                            Log.e(TAG, "discoverOpenStores() Appstore doesn't have name. Skipped. ComponentName: " + name);
                        } else if (billingIntent == null) { // don't handle stores without billing support
                            if (mDebugLog) Log.d(TAG, "discoverOpenStores(): billing is not supported by store: " + name);
                        } else if ((options.verifyMode == Options.VERIFY_EVERYTHING) && !options.storeKeys.containsKey(appstoreName)) { 
                            // don't connect to OpenStore if no key provided and verification is strict
                            Log.e(TAG, "discoverOpenStores() verification is required but publicKey is not provided: " + name);
                        } else {
                            String publicKey = options.storeKeys.get(appstoreName);
                            if (options.verifyMode == Options.VERIFY_SKIP) publicKey = null;
                            final OpenAppstore openAppstore = new OpenAppstore(context, appstoreName, openAppstoreService, billingIntent, publicKey, this);
                            openAppstore.componentName = name;
                            Log.d(TAG, "discoverOpenStores() add new OpenStore: " + openAppstore);
                            synchronized (result) {
                                if (result.contains(openAppstore) == false) {
                                    result.add(openAppstore);
                                }
                            }
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "discoverOpenStores() ComponentName: " + name, e);
                    }
                    storesToCheck.countDown();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    if (mDebugLog) Log.d(TAG, "onServiceDisconnected() appstoresService disconnected for component: " + name.flattenToShortString());
                    //Nothing to do here
                }
            }, Context.BIND_AUTO_CREATE);
        }
        try {
            storesToCheck.await(options.discoveryTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted: discovering OpenStores. ", e);
        }
        return result;
    }
    
    /**
     * Connects to Billing Service of each store. Request list of user purchases (inventory)
     * 
     * @see {@link OpenIabHelper#INVENTORY_CHECK_TIMEOUT_MS} to set timout value 
     * 
     * @param availableStores - list of stores to check
     * @return list of stores with non-empty inventory
     */
    protected List<Appstore> checkInventory(final List<Appstore> availableStores) {
        String packageName = context.getPackageName();
        // candidates:
        Map<String, Appstore> candidates = new HashMap<String, Appstore>();
        for (Appstore appstore : availableStores) {
            if (appstore.isBillingAvailable(packageName)) {
                candidates.put(appstore.getAppstoreName(), appstore);
            }
        }
        if (mDebugLog) Log.d(TAG, in() + " " + candidates.size() + " inventory candidates");
        final List<Appstore> equippedStores = Collections.synchronizedList(new ArrayList<Appstore>());
        final CountDownLatch storeRemains = new CountDownLatch(candidates.size());
        // for every appstore: connect to billing service and check inventory 
        for (Map.Entry<String, Appstore> entry : candidates.entrySet()) {
            final Appstore appstore = entry.getValue();
            final AppstoreInAppBillingService billingService = entry.getValue().getInAppBillingService();
            billingService.startSetup(new OnIabSetupFinishedListener() {
                public void onIabSetupFinished(IabResult result) {
                    if (mDebugLog) Log.d(TAG, in() + " " + "billing set " + appstore.getAppstoreName());
                    if(result.isFailure()) {
                        storeRemains.countDown();
                        return;
                    }
                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                Inventory inventory = billingService.queryInventory(false, null, null);
                                if (inventory.getAllPurchases().size() > 0) {
                                    equippedStores.add(appstore);
                                }
                                if (mDebugLog) Log.d(TAG, in() + " " + "inventoryCheck() in " + appstore.getAppstoreName() + " found: " + inventory.getAllPurchases().size() + " purchases");
                            } catch (IabException e) {
                                Log.e(TAG, "inventoryCheck() failed for " + appstore.getAppstoreName());
                            }
                            storeRemains.countDown();
                        }
                    }, "inv-check[" + appstore.getAppstoreName()+ "]").start();;
                }
            });
        }
        try {
            storeRemains.await(options.checkInventoryTimeoutMs, TimeUnit.MILLISECONDS);
            if (mDebugLog) Log.d(TAG, in() + " " + "inventory check done");
        } catch (InterruptedException e) {
            Log.e(TAG, "selectBillingService()  inventory check is failed. candidates: " + candidates.size() 
                    + ", inventory remains: " + storeRemains.getCount() , e);
        }
        return equippedStores;
    }
    
    /**
     * Lookup for requested service in store based on isPackageInstaller() & isBillingAvailable()
     * <p>
     * Scenario:
     * <li>
     * - look for installer: if exists and supports billing service - we done <li>  
     * - rest of stores who support billing considered as candidates<p><li>
     * 
     * - find candidate according to [prefferedStoreNames]. if found - we done<p><li>
     * 
     * - select candidate randomly from 3 groups based on published package version<li> 
     *   - published version == app.versionCode<li>
     *   - published version  > app.versionCode<li>
     *   - published version < app.versionCode
     * 
     */
    protected Appstore selectBillingService(final List<Appstore> availableStores) {
        String packageName = context.getPackageName();
        // candidates:
        Map<String, Appstore> candidates = new HashMap<String, Appstore>();
        //
        for (Appstore appstore : availableStores) {
            if (appstore.isBillingAvailable(packageName)) {
                candidates.put(appstore.getAppstoreName(), appstore);
            } else {
                continue; // for billing we cannot select store without billing
            }
            if (appstore.isPackageInstaller(packageName)) {
                return appstore;
            }
        }
        if (candidates.size() == 0) return null;
        
        // lookup for developer preffered stores
        for (int i = 0; i < options.prefferedStoreNames.length; i++) {
            Appstore candidate = candidates.get(options.prefferedStoreNames[i]);
            if (candidate != null) {
                return candidate;
            }
        }
        // nothing found. select something that matches package version
        int versionCode = Appstore.PACKAGE_VERSION_UNDEFINED; 
        try {
            versionCode = context.getPackageManager().getPackageInfo(packageName, 0).versionCode;
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Are we installed?", e);
        }
        List<Appstore> sameVersion = new ArrayList<Appstore>();
        List<Appstore> higherVersion = new ArrayList<Appstore>();
        for (Appstore candidate : candidates.values()) {
            final int storeVersion = candidate.getPackageVersion(packageName);
            if (storeVersion == versionCode) {
                 sameVersion.add(candidate);
            } else if (storeVersion > versionCode) {
                higherVersion.add(candidate);
            }
        }
        // use random if found stores with same version of package  
        if (sameVersion.size() > 0) {
            return sameVersion.get(new Random().nextInt(sameVersion.size()));
        } else if (higherVersion.size() > 0) {  // or one of higher version
            return higherVersion.get(new Random().nextInt(higherVersion.size()));
        } else {                                // ok, return no matter what
            return new ArrayList<Appstore>(candidates.values()).get(new Random().nextInt(candidates.size())); 
        }
    }
    
    public void dispose() {
        logDebug("Disposing.");
        if (mAppstoreBillingService != null) {
            mAppstoreBillingService.dispose();
        }
        setupState = SETUP_DISPOSED;
    }

    public boolean subscriptionsSupported() {
        // TODO: implement this
        return true;
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
        String storeSku = getStoreSku(mAppstore.getAppstoreName(), sku);
        mAppstoreBillingService.launchPurchaseFlow(act, storeSku, itemType, requestCode, listener, extraData);
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (mDebugLog) Log.d(TAG, in() + " " + "handleActivityResult() requestCode: " + requestCode+ " resultCode: " + resultCode+ " data: " + data);
        if (requestCode == options.samsungCertificationRequestCode && samsungInSetup != null) {
            return samsungInSetup.getInAppBillingService().handleActivityResult(requestCode, resultCode, data);
        }
        if (setupState != SETUP_RESULT_SUCCESSFUL) {
            if (mDebugLog) Log.d(TAG, "handleActivityResult() setup is not done. requestCode: " + requestCode+ " resultCode: " + resultCode+ " data: " + data);
            return false;
        }
        return mAppstoreBillingService.handleActivityResult(requestCode, resultCode, data);
    }

    /**
     * See {@link #queryInventory(boolean, List, List)} for details
     */
    public Inventory queryInventory(boolean querySkuDetails, List<String> moreSkus) throws IabException {
        return queryInventory(querySkuDetails, moreSkus, null);
    }

    /**
     * Queries the inventory. This will query all owned items from the server, as well as
     * information on additional skus, if specified. This method may block or take long to execute.
     * Do not call from a UI thread. For that, use the non-blocking version {@link #refreshInventoryAsync}.
     *
     * @param querySkuDetails if true, SKU details (price, description, etc) will be queried as well
     *                        as purchase information.
     * @param moreItemSkus    additional PRODUCT skus to query information on, regardless of ownership.
     *                        Ignored if null or if querySkuDetails is false.
     * @param moreSubsSkus    additional SUBSCRIPTIONS skus to query information on, regardless of ownership.
     *                        Ignored if null or if querySkuDetails is false.
     * @throws IabException if a problem occurs while refreshing the inventory.
     */
    public Inventory queryInventory(boolean querySkuDetails, List<String> moreItemSkus, List<String> moreSubsSkus) throws IabException {
        checkSetupDone("queryInventory");

        List<String> moreItemStoreSkus = null;
        if (moreItemSkus != null) {
            moreItemStoreSkus = new ArrayList<String>();
            for (String sku : moreItemSkus) {
                String storeSku = getStoreSku(mAppstore.getAppstoreName(), sku);
                moreItemStoreSkus.add(storeSku);
            }
        }
        List<String> moreSubsStoreSkus = null;
        if (moreSubsSkus != null) {
            moreSubsStoreSkus = new ArrayList<String>();
            for (String sku : moreSubsSkus) {
                String storeSku = getStoreSku(mAppstore.getAppstoreName(), sku);
                moreSubsStoreSkus.add(storeSku);
            }
        }
        return mAppstoreBillingService.queryInventory(querySkuDetails, moreItemStoreSkus, moreSubsStoreSkus);
    }

    /**
     * Queries the inventory. This will query all owned items from the server, as well as
     * information on additional skus, if specified. This method may block or take long to execute.
     * Do not call from a UI thread. For that, use the non-blocking version {@link #refreshInventoryAsync}.
     *
     * @param querySkuDetails if true, SKU details (price, description, etc) will be queried as well
     *                        as purchase information.
     * @param moreItemSkus    additional PRODUCT skus to query information on, regardless of ownership.
     *                        Ignored if null or if querySkuDetails is false.
     * @param moreSubsSkus    additional SUBSCRIPTIONS skus to query information on, regardless of ownership.
     *                        Ignored if null or if querySkuDetails is false.
     * @throws IabException if a problem occurs while refreshing the inventory.
     */
    public void queryInventoryAsync(final boolean querySkuDetails, final List<String> moreItemSkus, final List<String> moreSubsSkus, final IabHelper.QueryInventoryFinishedListener listener) {
        checkSetupDone("queryInventory");
        if (listener == null) {
             throw new IllegalArgumentException("Inventory listener must be not null");
        }
        flagStartAsync("refresh inventory");
        (new Thread(new Runnable() {
            public void run() {
                IabResult result = new IabResult(BILLING_RESPONSE_RESULT_OK, "Inventory refresh successful.");
                Inventory inv = null;
                try {
                    inv = queryInventory(querySkuDetails, moreItemSkus, moreSubsSkus);
                } catch (IabException ex) {
                    result = ex.getResult();
                }
                
                flagEndAsync();
                
                final IabResult result_f = result;
                final Inventory inv_f = inv;
                if (setupState != SETUP_DISPOSED) {
                    notifyHandler.post(new Runnable() {
                        public void run() {
                            listener.onQueryInventoryFinished(result_f, inv_f);
                        }
                    });
                }
            }
        })).start();
    }

    /**
     * For details see {@link #queryInventoryAsync(boolean, List, List, QueryInventoryFinishedListener)}
     */
    public void queryInventoryAsync(final boolean querySkuDetails, final List<String> moreSkus, final IabHelper.QueryInventoryFinishedListener listener) {
        checkSetupDone("queryInventoryAsync");
        if (listener == null) {
            throw new IllegalArgumentException("Inventory listener must be not null!");
        }
        queryInventoryAsync(querySkuDetails, moreSkus, null, listener);
    }

    /**
     * For details see {@link #queryInventoryAsync(boolean, List, List, QueryInventoryFinishedListener)}
     */
    public void queryInventoryAsync(IabHelper.QueryInventoryFinishedListener listener) {
        checkSetupDone("queryInventoryAsync");
        if (listener == null) {
            throw new IllegalArgumentException("Inventory listener must be not null!");
        }
        queryInventoryAsync(true, null, listener);
    }

    /**
     * For details see {@link #queryInventoryAsync(boolean, List, List, QueryInventoryFinishedListener)}
     */
    public void queryInventoryAsync(boolean querySkuDetails, IabHelper.QueryInventoryFinishedListener listener) {
        checkSetupDone("queryInventoryAsync");
        if (listener == null) {
            throw new IllegalArgumentException("Inventory listener must be not null!");
        }
        queryInventoryAsync(querySkuDetails, null, listener);
    }

    public void consume(Purchase itemInfo) throws IabException {
        checkSetupDone("consume");
        Purchase purchaseStoreSku = (Purchase) itemInfo.clone(); // TODO: use Purchase.getStoreSku()
        purchaseStoreSku.setSku(getStoreSku(mAppstore.getAppstoreName(), itemInfo.getSku()));
        mAppstoreBillingService.consume(purchaseStoreSku);
    }

    public void consumeAsync(Purchase purchase, IabHelper.OnConsumeFinishedListener listener) {
        checkSetupDone("consumeAsync");
        if (listener == null) {
            throw new IllegalArgumentException("Consume listener must be not null!");
        }
        List<Purchase> purchases = new ArrayList<Purchase>();
        purchases.add(purchase);
        consumeAsyncInternal(purchases, listener, null);
    }

    public void consumeAsync(List<Purchase> purchases, IabHelper.OnConsumeMultiFinishedListener listener) {
        checkSetupDone("consumeAsync");
        if (listener == null) {
            throw new IllegalArgumentException("Consume listener must be not null!");
        }
        consumeAsyncInternal(purchases, null, listener);
    }

    void consumeAsyncInternal(final List<Purchase> purchases,
                              final IabHelper.OnConsumeFinishedListener singleListener,
                              final IabHelper.OnConsumeMultiFinishedListener multiListener) {
        checkSetupDone("consume");
        flagStartAsync("consume");
        (new Thread(new Runnable() {
            public void run() {
                final List<IabResult> results = new ArrayList<IabResult>();
                for (Purchase purchase : purchases) {
                    try {
                        consume(purchase);
                        results.add(new IabResult(BILLING_RESPONSE_RESULT_OK, "Successful consume of sku " + purchase.getSku()));
                    } catch (IabException ex) {
                        results.add(ex.getResult());
                    }
                }

                flagEndAsync();
                if (setupState != SETUP_DISPOSED && singleListener != null) {
                    notifyHandler.post(new Runnable() {
                        public void run() {
                            singleListener.onConsumeFinished(purchases.get(0), results.get(0));
                        }
                    });
                }
                if (setupState != SETUP_DISPOSED && multiListener != null) {
                    notifyHandler.post(new Runnable() {
                        public void run() {
                            multiListener.onConsumeMultiFinished(purchases, results);
                        }
                    });
                }
            }
        })).start();
    }

    // Checks that setup was done; if not, throws an exception.
    void checkSetupDone(String operation) {
        String stateToString = setupStateToString(setupState);
        if (setupState != SETUP_RESULT_SUCCESSFUL) {
            logError("Illegal state for operation (" + operation + "): " + stateToString);
            throw new IllegalStateException(stateToString + " Can't perform operation: " + operation);
        }
    }

    void flagStartAsync(String operation) {
        // TODO: why can't be called consume and queryInventory at the same time?
//        if (mAsyncInProgress) {
//            throw new IllegalStateException("Can't start async operation (" +
//                    operation + ") because another async operation(" + mAsyncOperation + ") is in progress.");
//        }
        mAsyncOperation = operation;
        mAsyncInProgress = true;
        logDebug("Starting async operation: " + operation);
    }

    void flagEndAsync() {
        logDebug("Ending async operation: " + mAsyncOperation);
        mAsyncOperation = "";
        mAsyncInProgress = false;
    }

    void logDebug(String msg) {
        if (mDebugLog) Log.d(TAG, msg);
    }

    void logError(String msg) {
        Log.e(TAG, "In-app billing error: " + msg);
    }

    void logWarn(String msg) {
        if (mDebugLog) Log.w(TAG, "In-app billing warning: " + msg);
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

    public interface OnInitListener {
        public void onInitFinished();
    }

    public interface OnOpenIabHelperInitFinished {
        public void onOpenIabHelperInitFinished();
    }
    
    private static String in() {
        return "in: " + (System.currentTimeMillis() - started);
    }
    
    /**
     * All options of OpenIAB can be found here
     * 
     * TODO: consider to use cloned instance of Options in OpenIABHelper   
     */
    public static class Options {
        
        /** 
         * List of stores to be used for store elections. By default GooglePlay, Amazon, SamsungApps and 
         * all installed OpenStores are used.
         * <p>
         * To specify your own list, you need to instantiate Appstore object manually.
         * GooglePlay, Amazon and SamsungApps could be instantiated directly. OpenStore can be discovered 
         * using {@link OpenIabHelper#discoverOpenStores(Context, List, Options)}
         * <p>
         * If you put only your instance of Appstore in this list OpenIAB will use it
         * 
         * TODO: consider to use AppstoreFactory.get(storeName) -> Appstore instance
         */
        public List<Appstore> availableStores;
        
        /**
         * Wait specified amount of ms to find all OpenStores on device
         */
        public int discoveryTimeoutMs = DISCOVER_TIMEOUT_MS;
        /** 
         * Check user inventory in every store to select proper store
         * <p>
         * Will try to connect to each billingService and extract user's purchases.
         * If purchases have been found in the only store that store will be used for further purchases. 
         * If purchases have been found in multiple stores only such stores will be used for further elections    
         */
        public boolean checkInventory = true;
        
        /**
         * Wait specified amount of ms to check inventory in all stores
         */
        public int checkInventoryTimeoutMs = INVENTORY_CHECK_TIMEOUT_MS;
        
        /** 
         * OpenIAB could skip receipt verification by publicKey for GooglePlay and OpenStores 
         * <p>
         * Receipt could be verified in {@link OnIabPurchaseFinishedListener#onIabPurchaseFinished()}
         * using {@link Purchase#getOriginalJson()} and {@link Purchase#getSignature()}
         */
        public int verifyMode = VERIFY_EVERYTHING;
        /**
         * Verify signatures in any store. 
         * <p>
         * By default in Google's IabHelper. Throws exception if key is not available or invalid.
         * To prevent crashes OpenIAB wouldn't connect to OpenStore if no publicKey provided
         */
        public static final int VERIFY_EVERYTHING = 0;
        /**
         * Don't verify signatires. To perform verification on server-side
         */
        public static final int VERIFY_SKIP = 1;
        /**
         * Verify signatures only if publicKey is available. Otherwise skip verification. 
         * <p>
         * Developer is responsible for verify
         */
        public static final int VERIFY_ONLY_KNOWN = 2;
        
        /** 
         * storeKeys is map of [ appstore name -> publicKeyBase64 ] 
         * Put keys for all stores you support in this Map and pass it to instantiate {@link OpenIabHelper} 
         * <p>
         * <b>publicKey</b> key is used to verify receipt is created by genuine Appstore using 
         * provided signature. It can be found in Developer Console of particular store
         * <p>
         * <b>name</b> of particular store can be provided by local_store tool if you run it on device.
         * For Google Play OpenIAB uses {@link OpenIabHelper#NAME_GOOGLE}.
         * <p>
         * <p>Note:
         * AmazonApps and SamsungApps doesn't use RSA keys for receipt verification, so you don't need 
         * to specify it
         */
        public Map<String, String> storeKeys = new HashMap<String, String>();
        
        /**
         * Used as priority list if store that installed app is not found and there are 
         * multiple stores installed on device that supports billing.
         */
        public String[] prefferedStoreNames = new String[] {};
        
        /** Used for SamsungApps setup. Specify your own value if default one interfere your code.
         * <p>default value is {@link SamsungAppsBillingService#REQUEST_CODE_IS_ACCOUNT_CERTIFICATION} */
        public int samsungCertificationRequestCode = SamsungAppsBillingService.REQUEST_CODE_IS_ACCOUNT_CERTIFICATION;
    }

}
