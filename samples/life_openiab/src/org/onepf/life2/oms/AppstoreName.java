package org.onepf.life2.oms;

/**
 * User: Boris Minaev
 * Date: 16.04.13
 * Time: 17:41
 */
public enum AppstoreName {
    APPSTORE_GOOGLE("google"),
    APPSTORE_AMAZON("amazon"),
    APPSTORE_SAMSUNG("samsung"),
    APPSTORE_YANDEX("yandex"),
    APPSTORE_TSTORE("tstore");

    private String name;

    private AppstoreName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
