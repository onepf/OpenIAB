package org.onepf.lifegame;

/**
 * Created by krozov on 07.08.14.
 */
public interface PurchaseConfig {
    /**
     * Public key for Yandex store.
     */
    String YANDEX_PUBLIC_KEY = "";

    /**
     * Public key for Google Play.
     */
    String GOOGLE_PUBLIC_KEY = "";

    //consumable
    String SKU_CHANGES = BuildConfig.PACKAGE_NAME + ".changes";

    //non-consumable
    String SKU_FIGURES = BuildConfig.PACKAGE_NAME + ".figures";

    //subscription
    String SKU_ORANGE_CELLS = BuildConfig.PACKAGE_NAME + ".orange_cells";
}
