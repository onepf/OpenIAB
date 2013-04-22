package org.onepf.life2.oms;

/**
 * User: Boris Minaev
 * Date: 21.04.13
 * Time: 21:34
 */

import java.util.EnumMap;
import java.util.Map;

public class OpenSku {
    private Map<AppstoreName, String> storeToSku;

    public static class Sku {
        AppstoreName appstore;
        String sku;

        public Sku(AppstoreName appstore, String sku) {
            this.appstore = appstore;
            this.sku = sku;
        }
    }

    public OpenSku(Sku... skus) {
        storeToSku = new EnumMap<AppstoreName, String>(AppstoreName.class);
        for (Sku sku : skus) {
            storeToSku.put(sku.appstore, sku.sku);
        }
    }

    public String getSku(AppstoreName appstore) {
        return storeToSku.get(appstore);
    }
}
