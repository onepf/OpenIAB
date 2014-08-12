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

    private static final String SKU_GAS_NOKIA_STORE = "";

    private static final String SKU_INFINITE_GAS_NOKIA_STORE = "";

    private static final String SKU_PREMIUM_NOKIA_STORE = "";

    static {
        STORE_KEYS_MAP = new HashMap<>();
        STORE_KEYS_MAP.put(OpenIabHelper.NAME_GOOGLE, Config.GOOGLE_PLAY_KEY);
        STORE_KEYS_MAP.put(OpenIabHelper.NAME_YANDEX, Config.YANDEX_PUBLIC_KEY);
        STORE_KEYS_MAP.put(OpenIabHelper.NAME_APPLAND, Config.APPLAND_PUBLIC_KEY);
        STORE_KEYS_MAP.put(OpenIabHelper.NAME_SLIDEME, Config.SLIDEME_PUBLIC_KEY);

        //Only map SKUs for stores where SKU that using in app different from described in store console.
        SkuManager.getInstance()
                .mapSku(Config.SKU_GAS, OpenIabHelper.NAME_NOKIA, SKU_GAS_NOKIA_STORE)
                .mapSku(Config.SKU_PREMIUM, OpenIabHelper.NAME_NOKIA, SKU_PREMIUM_NOKIA_STORE)
                .mapSku(Config.SKU_INFINITE_GAS, OpenIabHelper.NAME_NOKIA, SKU_INFINITE_GAS_NOKIA_STORE);
    }

    private Config() {
    }
}
