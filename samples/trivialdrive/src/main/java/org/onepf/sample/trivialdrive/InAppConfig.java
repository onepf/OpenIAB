package org.onepf.sample.trivialdrive;

import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.SkuManager;

import java.util.HashMap;
import java.util.Map;

/**
 * In-app products configuration.
 * <p/>
 * Created by krozov on 01.09.14.
 */
public final class InAppConfig {
    //premium upgrade (non-consumable)
    public static final String SKU_PREMIUM = "sku_premium";
    //gas (consumable)
    public static final String SKU_GAS = "sku_gas";
    //subscription (infinite gas)
    public static final String SKU_INFINITE_GAS = "sku_infinite_gas";

    //Google Play
    public static final String GOOGLE_PLAY_KEY
            = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5F8fASyrDFdaXrkoW8kNtwH5JIkLnNuTD5uE1a37TbI5LDZR" +
            "VgvMIYAtZ9CAHAfLnJ6OEZt0lvLLJSKVuS47VqYVhGZciOkX8TEihONBRwis6i9A3JnKfyqm0iiT+P0CEktOLuFLROIo13" +
            "utCIO++6h7A7/WLfxNV+Jnxfs9OEHyyPS+MdHxa0wtZGeAGiaN65BymsBQo7J/ABt2DFyMJP1R/nJM45F8yu4D6wSkUNKz" +
            "s/QbPfvHJQzq56/B/hbx59EkzkInqC567hrlUlX4bU5IvOTF/B1G+UMuKg80m3I1IcQk4FD2D9oJ3E+8IXG/1UdejrOsmq" +
            "DAzE7LkMl8xwIDAQAB";

    //Yandex.Store
    public static final String YANDEX_PUBLIC_KEY
            = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAs4SI/obW+q3dhsY3g5B6UggPcL5olWK8WY3tnTa2k3i2U40j" +
            "QuHRNNs8SqzdJeuoBLsKjaEsdTT0SJtEucOMZrprXMch97QtuLB4Mgu3Gs7USL6dM7NCUSoYrgOgw1Koi+ab+ZvFJkVMb9" +
            "a2EjYzR3aP0k4xjKyG2gW1rIEMMepxHm22VFjEg6YxBy+ecwRrjqDJOAPJyH6uSl8vUT8AKuG+hcCuYbNvlMdEZJo6MXJ9" +
            "vPNf/qPHwMy5G+faEprL6zR+HaPfxEqN/d8rbrW0qnr8LpXJ+nPB3/irBiMSZSqA222GC7m12sNNmNnNNlI397F3fRQSTz" +
            "VSRZt14YdPzwIDAQAB";
    //Appland
    public static final String APPLAND_PUBLIC_KEY =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC5idC9c24V7a7qCJu7kdIyOZsk\n" +
                    "W0Rc7/q+K+ujEXsUaAdb5nwmlOJqpoJeCh5Fmq5A1NdF3BwkI8+GwTkH757NBZAS\n" +
                    "SdEuN0pLZmA6LopOiMIy0LoIWknM5eWMa3e41CxCEFoMv48gFIVxDNJ/KAQAX7+K\n" +
                    "ysYzIdlA3W3fBXXyGQIDAQAB";
    //SlideME
    public static final String SLIDEME_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwRiedByAoS2" +
            "DUJAkm6qqfhlhNnpNbONf8W4giGERMBAvRA7mRGKa7+vtgJiepvQ/CX0np5MBAMXcL9t9YFZ30lmp4COBdr5nilTyUdLWns" +
            "cnhYIxneJIG3rzkmnhXaDsiemOlrLC2PEJu6jcek8qurJmQ7gpP0va45MwiTHHto1lSjjvF8xYAZSrTlbIqLo1f98lxg9xs" +
            "zHI6sSXwDqDpJfS0JORtw3Rcc731QFR1rR2EOAEZo6Zdo0cD1uOQJgLkv8drU9BDMsR9ErBuGSbZQzn2FAc4Bkmq/gNGYd1" +
            "HmdFkofwVkqu/dTYWXOumKDIVqRsLQ213vuvC0lzcLaJxQIDAQAB";

    public static Map<String, String> STORE_KEYS_MAP;

    public static void init() {
        STORE_KEYS_MAP = new HashMap<>();
        STORE_KEYS_MAP.put(OpenIabHelper.NAME_GOOGLE, InAppConfig.GOOGLE_PLAY_KEY);
        STORE_KEYS_MAP.put(OpenIabHelper.NAME_YANDEX, InAppConfig.YANDEX_PUBLIC_KEY);
        STORE_KEYS_MAP.put(OpenIabHelper.NAME_APPLAND, InAppConfig.APPLAND_PUBLIC_KEY);
        STORE_KEYS_MAP.put(OpenIabHelper.NAME_SLIDEME, InAppConfig.SLIDEME_PUBLIC_KEY);
//        STORE_KEYS_MAP.put(OpenIabHelper.NAME_AMAZON,
//                "Unavailable. Amazon doesn't support RSA verification. So this mapping is not needed");
//        STORE_KEYS_MAP.put(OpenIabHelper.NAME_SAMSUNG,
//                "Unavailable. SamsungApps doesn't support RSA verification. So this mapping is not needed");

        SkuManager.getInstance()
                //Yandex.Store
                .mapSku(SKU_GAS, OpenIabHelper.NAME_YANDEX, "org.onepf.sample.trivialdrive.sku_gas")
                .mapSku(SKU_PREMIUM, OpenIabHelper.NAME_YANDEX, "org.onepf.sample.trivialdrive.sku_premium")
                .mapSku(SKU_INFINITE_GAS, OpenIabHelper.NAME_YANDEX, "org.onepf.sample.trivialdrive.sku_infinite_gas")
                        //Nokia store
                .mapSku(SKU_GAS, OpenIabHelper.NAME_NOKIA, "1290250")
                .mapSku(SKU_PREMIUM, OpenIabHelper.NAME_NOKIA, "1290315")
                .mapSku(SKU_INFINITE_GAS, OpenIabHelper.NAME_NOKIA, "1290302")
                        //Amazon
                .mapSku(SKU_GAS, OpenIabHelper.NAME_AMAZON, "org.onepf.sample.trivialdrive.sku_gas")
                .mapSku(SKU_PREMIUM, OpenIabHelper.NAME_AMAZON, "org.onepf.sample.trivialdrive.sku_premium")
                .mapSku(SKU_INFINITE_GAS, OpenIabHelper.NAME_AMAZON, "org.onepf.sample.trivialdrive.subscrption.sku_infinite_gas")
                        //Appland
                .mapSku(SKU_GAS, OpenIabHelper.NAME_APPLAND, "appland.sku_gas")
                .mapSku(SKU_PREMIUM, OpenIabHelper.NAME_APPLAND, "appland.sku_premium")
                .mapSku(SKU_INFINITE_GAS, OpenIabHelper.NAME_APPLAND, "appland.sku_infinite_gas") //todo check is it supported
                        //SlideME
                .mapSku(SKU_GAS, OpenIabHelper.NAME_SLIDEME, "slideme.sku_gas")
                .mapSku(SKU_PREMIUM, OpenIabHelper.NAME_SLIDEME, "slideme.sku_premium")
                .mapSku(SKU_INFINITE_GAS, OpenIabHelper.NAME_SLIDEME, "slideme.sku_infinite_gas")
                        //Samsung
                .mapSku(SKU_GAS, OpenIabHelper.NAME_SAMSUNG, "000001003746/org.onepf.trivialdrivegame.sku_gas")
                .mapSku(SKU_PREMIUM, OpenIabHelper.NAME_SAMSUNG, "000001003747/org.onepf.trivialdrivegame.sku_premium")
                .mapSku(SKU_INFINITE_GAS, OpenIabHelper.NAME_SAMSUNG, "000001003744/org.onepf.trivialdrivegame.subscrption.sku_infinite_gas");
    }

    private InAppConfig() {
    }
}
