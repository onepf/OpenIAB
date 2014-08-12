package org.onepf.lifegame;

import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.SkuManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by krozov on 07.08.14.
 */
public final class Config {
    /**
     * Public key for Yandex.Store.
     */
    public static final String YANDEX_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAl+f8rFyrOHu9jOKn9Aj1Cg+y7T4BP24YuPWjTdZJ3sjY50qYYuBIGKQKiQ7t/8R+d+zui1+HYWieVqomy/dO/f5AThjgVl43a3WeJbCJzK+2ctXLfDbyiDpHYJKs01japLMPqXNfOYxL4NqR6anJWS2SKpCaKDyasFXZ5axDmH/kScuYM2MksCudRoxRTHriKgBPuRG6GLmbPBa9FCxcBq9g5WBhuztZl+JBdyBrY9seYD3dv0PtD5RMBzJuNv9mqaQe6vlYUITDuR7ZnJOE9HyeDvSZieKcDAgHq+SPUBNTHpsfC+ZuK8uXqtEBuHjz0wweLrfw3AOGy6jdhNKfdwIDAQAB";

    /**
     * Public key for Google Play.
     */
    public static final String GOOGLE_PLAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgjIHTYMXI0oCR0QRtxKwt4bEYBiMmXujSqAiyEq+3g6Hnb1+UkxaQR5C8hwxu6KA1wR/61kMP+/KzFjz1Zr1VGQquJ3bWSAnbnF1ypDVu9nLeyraDBPlPVLrQNtu0OvkbMJp6WX81z/Y8LaxvAGVSh/vSG84/IrTJ5y8iJq9JN/zGecdR4J7/zD+R/K8b8vYQBhJ7vsTBbowR8oJ3HDVQHFX0BBqTfTJp6uszG6JeleKfhrzyEsNBM8WXS9sAlDY6CGhm9ZFGLHLHRUQBTScgYX7t4Ne8gOKaVsUcsUCCpc5/MhS52e/TNbYBfV/fejcqXtpfvLXk0WNA0q9dj1wZwIDAQAB";

    /**
     * Public key for Appland store.
     */
    public static final String APPLAND_PUBLIC_KEY = "";

    /**
     * Public key for SlideMe store.
     */
    public static final String SLIDEME_PUBLIC_KEY = "";

    /**
     * SKU of purchase that add 50 addition changes.
     */
    //consumable
    public static final String SKU_CHANGES = BuildConfig.PACKAGE_NAME + ".changes";

    /**
     * SKU of purchase that unlock new figures.
     */
    //non-consumable
    public static final String SKU_FIGURES = BuildConfig.PACKAGE_NAME + ".figures";

    /**
     * SKU of purchase that unlock orange cells of one month.
     */
    //subscription
    public static final String SKU_ORANGE_CELLS = BuildConfig.PACKAGE_NAME
            + ".subscription.orange_cells";

    /**
     * Nokia Store specific SKU for {@link #SKU_CHANGES}
     */
    public static final String SKU_CHANGES_NOKIA_STORE = "";

    /**
     * Nokia Store specific SKU for {@link #SKU_FIGURES}
     */
    public static final String SKU_FIGURES_NOKIA_STORE = "";

    /**
     * Nokia Store specific SKU for {@link #SKU_ORANGE_CELLS}
     */
    public static final String SKU_ORANGE_CELLS_NOKIA_STORE = "";

    public static final Map<String, String> STORE_KEYS_MAP;

    static {
        STORE_KEYS_MAP = new HashMap<>();
        STORE_KEYS_MAP.put(OpenIabHelper.NAME_GOOGLE, Config.GOOGLE_PLAY_PUBLIC_KEY);
        STORE_KEYS_MAP.put(OpenIabHelper.NAME_YANDEX, Config.YANDEX_PUBLIC_KEY);
//        STORE_KEYS_MAP.put(OpenIabHelper.NAME_APPLAND, Config.APPLAND_PUBLIC_KEY);
//        STORE_KEYS_MAP.put(OpenIabHelper.NAME_SLIDEME, Config.SLIDEME_PUBLIC_KEY);

        //Only map SKUs for stores that using purchase with SKUs different from described in store console.
        SkuManager.getInstance()
                .mapSku(SKU_CHANGES, OpenIabHelper.NAME_NOKIA, SKU_CHANGES_NOKIA_STORE)
                .mapSku(SKU_FIGURES, OpenIabHelper.NAME_NOKIA, SKU_FIGURES_NOKIA_STORE)
                .mapSku(SKU_ORANGE_CELLS, OpenIabHelper.NAME_NOKIA, SKU_ORANGE_CELLS_NOKIA_STORE);
    }

    private Config() {
        //Can't create instance of class
    }
}
