package org.onepf.trivialdrivegame;

import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.SkuManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for purchase.
 * <p/>
 * Created by krozov on 07.08.14.
 */
public final class Config {

    // SKUs for our products: the premium upgrade (non-consumable) and gas (consumable)
    public static final String SKU_PREMIUM = BuildConfig.PACKAGE_NAME + ".sku_premium";

    public static final String SKU_GAS = BuildConfig.PACKAGE_NAME + ".sku_gas";

    // SKU for our subscription (infinite gas)
    public static final String SKU_INFINITE_GAS = BuildConfig.PACKAGE_NAME + ".sku_infinite_gas";

    /**
     * Google play public key.
     */
    public static final String GOOGLE_PLAY_KEY = "";

    /**
     * Yandex.Store public key.
     */
    public static final String YANDEX_PUBLIC_KEY = "";

    /**
     * Appland store public key.
     */
    public static final String APPLAND_PUBLIC_KEY = "";

    /**
     * SlideMe store public key.
     */
    public static final String SLIDEME_PUBLIC_KEY = "";

    public static final Map<String, String> STORE_KEYS_MAP;

    private static final String SKU_GAS_NOKIA_STORE = "1290250";

    private static final String SKU_INFINITE_GAS_NOKIA_STORE = "1290302";

    private static final String SKU_PREMIUM_NOKIA_STORE = "1290315";

    static {
        STORE_KEYS_MAP = new HashMap<>();
        STORE_KEYS_MAP.put(OpenIabHelper.NAME_GOOGLE, Config.GOOGLE_PLAY_KEY);
        STORE_KEYS_MAP.put(OpenIabHelper.NAME_YANDEX, Config.YANDEX_PUBLIC_KEY);
        STORE_KEYS_MAP.put(OpenIabHelper.NAME_APPLAND, Config.APPLAND_PUBLIC_KEY);
        STORE_KEYS_MAP.put(OpenIabHelper.NAME_SLIDEME, Config.SLIDEME_PUBLIC_KEY);
//      STORE_KEYS_MAP.put(OpenIabHelper.NAME_AMAZON, "Unavailable. Amazon doesn't support RSA verification. So this mapping is not needed"); //
//      STORE_KEYS_MAP.put(OpenIabHelper.NAME_SAMSUNG,"Unavailable. SamsungApps doesn't support RSA verification. So this mapping is not needed"); //

        final Map<String, String> premiumStoreSkus = new HashMap<>(6);
        premiumStoreSkus.put(OpenIabHelper.NAME_AMAZON, SKU_PREMIUM);
//        premiumStoreSkus.put(OpenIabHelper.NAME_SAMSUNG, "100000100696/000001003746");
        premiumStoreSkus.put(OpenIabHelper.NAME_YANDEX, SKU_PREMIUM);
        premiumStoreSkus.put(OpenIabHelper.NAME_APPLAND, SKU_PREMIUM);
        premiumStoreSkus.put(OpenIabHelper.NAME_NOKIA, SKU_PREMIUM_NOKIA_STORE);
        premiumStoreSkus.put(OpenIabHelper.NAME_SLIDEME, SKU_PREMIUM);
        premiumStoreSkus.put(OpenIabHelper.NAME_GOOGLE, SKU_PREMIUM);

        final Map<String, String> gasStoreSkus = new HashMap<>(6);
        gasStoreSkus.put(OpenIabHelper.NAME_AMAZON, SKU_GAS);
//        gasStoreSkus.put(OpenIabHelper.NAME_SAMSUNG, "100000100696/000001003744");
        gasStoreSkus.put(OpenIabHelper.NAME_YANDEX, SKU_GAS);
        gasStoreSkus.put(OpenIabHelper.NAME_NOKIA, SKU_GAS_NOKIA_STORE);
        gasStoreSkus.put(OpenIabHelper.NAME_SLIDEME, SKU_GAS);
        gasStoreSkus.put(OpenIabHelper.NAME_GOOGLE, SKU_GAS);
        gasStoreSkus.put(OpenIabHelper.NAME_APPLAND, SKU_GAS);

        final Map<String, String> infiniteGasStoreSkus = new HashMap<>(5);
        infiniteGasStoreSkus.put(OpenIabHelper.NAME_AMAZON, SKU_INFINITE_GAS);
//        infiniteGasStoreSkus.put(OpenIabHelper.NAME_SAMSUNG, "100000100696/000001003747");
        infiniteGasStoreSkus.put(OpenIabHelper.NAME_YANDEX, SKU_INFINITE_GAS);
        infiniteGasStoreSkus.put(OpenIabHelper.NAME_NOKIA, SKU_INFINITE_GAS_NOKIA_STORE);
        infiniteGasStoreSkus.put(OpenIabHelper.NAME_SLIDEME, SKU_INFINITE_GAS);
        infiniteGasStoreSkus.put(OpenIabHelper.NAME_GOOGLE, SKU_INFINITE_GAS);
        infiniteGasStoreSkus.put(OpenIabHelper.NAME_APPLAND, SKU_INFINITE_GAS);

        SkuManager.getInstance()
                .mapSku(Config.SKU_GAS, gasStoreSkus)
                .mapSku(Config.SKU_PREMIUM, premiumStoreSkus)
                .mapSku(Config.SKU_INFINITE_GAS, infiniteGasStoreSkus);
    }

    private Config() {
    }
}
