package org.onepf.life2.oms;

/**
 * User: Boris Minaev
 * Date: 21.04.13
 * Time: 21:34
 */

import java.util.HashMap;
import java.util.Map;

/**
 * Stores sku's for different stores
 * <p/>
 * Samples of use:
 * 1. new OpenItem("some_sku_name")
 * sku for Google Play is "some_sku_name", sku for other stores are undefined
 * 2. new OpenItem("'Google Play' : 'some_sku_name_google', 'Amazon AppStore' : 'some_sku_name_amazon'")
 * sku for Google Play is "some_sku_name_google", for Amazon App Store is "some_sku_name_amazon"
 * and undefined for others
 * 3. new OpenItem("'Google Play':'some_sku_name_google', 'Amazon AppStore' : '?'")
 * sku for Google Play is "some_sku_name_google", sku for other stores are undefined
 * <p/>
 * List of all supported stores names:
 * 1. Google Play
 * 2. Amazon AppStore
 * 3. Samsung Apps
 * 4. Yandex.Store
 * 5. SK T-Store
 */

public class OpenItem {
    private Map<AppstoreName, String> storeToSkuName;

    public OpenItem(String s) {
        s = s.replaceAll("\\s", "");
        storeToSkuName = new HashMap<>();
        if (s.contains(",") || s.contains(":")) {
            String[] allStores = s.split(",");
            for (String curStore : allStores) {
                String[] parse = curStore.split(":");
                if (parse.length != 2 || parse[0].length() < 3 || parse[1].length() < 3) {
                    throw new OpenItemParseException(curStore + " isn't valid sku description");
                }
                String storeName = parse[0].substring(1, parse[0].length() - 1);
                AppstoreName appstore = null;
                for (AppstoreName appstoreName : AppstoreName.values()) {
                    if (appstoreName.toString().equals(storeName)) {
                        appstore = appstoreName;
                    }
                }
                if (appstore == null) {
                    throw new OpenItemParseException(storeName + " isn't valid store name");
                }
                String sku = parse[1].substring(1, parse[1].length() - 1);
                setSkuForStore(appstore, sku);
            }
        } else {
            storeToSkuName.put(AppstoreName.APPSTORE_GOOGLE, s);
        }
    }

    public String getSkuName(AppstoreName appstore) {
        return storeToSkuName.get(appstore);
    }

    public void setSkuForStore(AppstoreName appstore, String sku) {
        if (sku.equals("?")) {
            return;
        }
        if (storeToSkuName.containsKey(appstore)) {
            throw new OpenItemParseException("Redefinition sku for " + appstore.toString());
        }
        storeToSkuName.put(appstore, sku);
    }
}
