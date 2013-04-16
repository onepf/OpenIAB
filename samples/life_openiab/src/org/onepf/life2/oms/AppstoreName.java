package org.onepf.life2.oms;

/**
 * User: Boris Minaev
 * Date: 16.04.13
 * Time: 17:41
 */
public enum AppstoreName {
    APPSTORE_GOOGLE("Google Play"),
    APPSTORE_AMAZON("Amazon AppStore"),
    APPSTORE_SAMSUNG("Samsung Apps"),
    APPSTORE_YANDEX("Yandex.Store"),
    APPSTORE_TSTORE("SK T-Store");

    private String name;

    private AppstoreName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
