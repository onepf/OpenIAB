package org.onepf.trivialdrive;

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
    public static final String SKU_PREMIUM = "sku_premium";

    public static final String SKU_GAS = "sku_gas";

    // SKU for our subscription (infinite gas)
    public static final String SKU_INFINITE_GAS = "sku_infinite_gas";

    /**
     * Google play public key.
     */
    public static final String GOOGLE_PLAY_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA8A4rv" +
            "1uXF5mqJGrtGkQ5PQGpyNIgcZhvRD3yNLC5T+NlIlvMlkuGUmgZnXHfPdORZT/s5QXa2ytjffOy" +
            "DVgXpHrZ0J9bRoR+hePP4o0ANzdEY/ehkt0EsifB2Kjhok+kTNpikplwuFtIJnIyFyukcesPAXks" +
            "u2LTQAEzYwlMeJ8W4ToDHw6U5gEXLZcMKiDVTFA0pb89wVfb76Uerv9c6lrydKZiTn/gxg8J1yrz7v" +
            "NzX7IzoWPO0+pXLnkcgqtEHePF2DIW1D29GkNJOt6xH3IvyS4ZI+1xs3wuSg8vWq3fQP/XIVHZQOqd" +
            "5pmJY0tdgzboHuqq3ebtNrBI6Ky0SwIDAQAB";

    /**
     * Yandex.Store public key.
     */
    public static final String YANDEX_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAs4" +
            "NKNVt1lC97e5qr5qIK31WKh470ihgFdRSiV/8kdKtdk2gsLD70AFPFZ0py/OOyZflD" +
            "jTOya809mU0lsWOxrrGZBRFqQKbvCPh9ZIMVZc79Uz0UZfjBy/n2h4bc0Z5VeBIsnDNh4" +
            "DCD/XlHYwLIf6En+uPkKZwD3lG2JW4q4Hmuc3HYbuagv+hMexEG/umjbHTRq5rJ+rJ2LyY" +
            "Qs5Kdi/UZ5JKjsk9CuYrzMi9TqOqc9fDG19mfqqr4lfzvKneGIG11c3d1yUNX/MmSE43QYPPW" +
            "NNKgGLha1AbS7RvtbWzEviiEZ0wjQkRSu4QAXhUurzK75eWDBN2KiJK9mlI1lQIDAQAB";

    /**
     * Appland store public key.
     */
    public static final String APPLAND_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC" +
            "5idC9c24V7a7qCJu7k" +
            "dIyOZskW0Rc7/q+K+ujEXsUaAdb5nwmlOJqpoJeCh5Fmq5A1NdF3BwkI8+GwTkH757NBZASSdEuN0" +
            "pLZmA6LopOiMIy0LoIWknM5eWMa3e41CxCEFoMv48gFIVxDNJ/KAQAX7+KysYzIdlA3W3fBXXyGQIDAQAB";

    /**
     * SlideMe store public key.
     */
    public static final String SLIDEME_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBC" +
            "gKCAQEAq6rFm2wb9sm" +
            "bcowrfZHYw71ISHYxF/tG9Jn9c+nRzFCVDSXjvedBxKllw16/GEx9DQ32Ut8azVAznB2wBDNUsS" +
            "M8nzNhHeCSDvEX2/Ozq1dEq3V3DF4jBEKDAkIOMzIBRWN8fpA5MU/9m8QD9xkJDfP7Mw/6zEMidk" +
            "2CEE8EZRTlpQ8ULVgBlFISd8Mt9w8ZFyeTyJTZhF2Z9+RZN8woU+cSXiVRmiA0+v2R8Pf+YNJb9fd" +
            "V5yvM8r9K1MEdRaXisJyMOnjL7H2mZWigWLm7uGoUGuIg9HHi09COBMm3dzAe9yLZoPSG75SvYDs" +
            "AZ6ms8IYxF6FAniNqfMOuMFV8zwIDAQAB";

    public static final Map<String, String> STORE_KEYS_MAP;

    static {
        STORE_KEYS_MAP = new HashMap<String, String>();
        STORE_KEYS_MAP.put(OpenIabHelper.NAME_GOOGLE, Config.GOOGLE_PLAY_KEY);
        STORE_KEYS_MAP.put(OpenIabHelper.NAME_YANDEX, Config.YANDEX_PUBLIC_KEY);
        STORE_KEYS_MAP.put(OpenIabHelper.NAME_APPLAND, Config.APPLAND_PUBLIC_KEY);
        STORE_KEYS_MAP.put(OpenIabHelper.NAME_SLIDEME, Config.SLIDEME_PUBLIC_KEY);
//      STORE_KEYS_MAP.put(OpenIabHelper.NAME_AMAZON, "Unavailable. Amazon doesn't support RSA verification. So this mapping is not needed"); //
//      STORE_KEYS_MAP.put(OpenIabHelper.NAME_SAMSUNG,"Unavailable. SamsungApps doesn't support RSA verification. So this mapping is not needed"); //

        final Map<String, String> premiumStoreSkus = new HashMap<String, String>(6);
        premiumStoreSkus.put(OpenIabHelper.NAME_AMAZON, "org.onepf.trivialdrive.amazon.premium");
        premiumStoreSkus.put(OpenIabHelper.NAME_SAMSUNG, "100000100696/000001003746");
        premiumStoreSkus.put(OpenIabHelper.NAME_YANDEX, "org.onepf.trivialdrive.premium");
        premiumStoreSkus.put(OpenIabHelper.NAME_APPLAND, "org.onepf.trivialdrive.premium");
        premiumStoreSkus.put(OpenIabHelper.NAME_NOKIA, "1023608");
        premiumStoreSkus.put(OpenIabHelper.NAME_SLIDEME, "slideme_sku_premium");

        final Map<String, String> gasStoreSkus = new HashMap<String, String>(6);
        gasStoreSkus.put(OpenIabHelper.NAME_AMAZON, "org.onepf.trivialdrive.amazon.gas");
        gasStoreSkus.put(OpenIabHelper.NAME_SAMSUNG, "100000100696/000001003744");
        gasStoreSkus.put(OpenIabHelper.NAME_YANDEX, "org.onepf.trivialdrive.gas");
        gasStoreSkus.put(OpenIabHelper.NAME_APPLAND, "org.onepf.trivialdrive.gas");
        gasStoreSkus.put(OpenIabHelper.NAME_NOKIA, "1023609");
        gasStoreSkus.put(OpenIabHelper.NAME_SLIDEME, "slideme_sku_gas");

        final Map<String, String> infiniteGasStoreSkus = new HashMap<String, String>(5);
        infiniteGasStoreSkus.put(OpenIabHelper.NAME_AMAZON, "org.onepf.trivialdrive.amazon.infinite_gas");
        infiniteGasStoreSkus.put(OpenIabHelper.NAME_SAMSUNG, "100000100696/000001003747");
        infiniteGasStoreSkus.put(OpenIabHelper.NAME_YANDEX, "org.onepf.trivialdrive.infinite_gas");
        infiniteGasStoreSkus.put(OpenIabHelper.NAME_NOKIA, "1023610");
        infiniteGasStoreSkus.put(OpenIabHelper.NAME_SLIDEME, "slideme_sku_inifinite_gas");

        SkuManager.getInstance()
                .mapSku(Config.SKU_GAS, gasStoreSkus)
                .mapSku(Config.SKU_PREMIUM, premiumStoreSkus)
                .mapSku(Config.SKU_INFINITE_GAS, infiniteGasStoreSkus);
    }

    private Config() {
    }
}
