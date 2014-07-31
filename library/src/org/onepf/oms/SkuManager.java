/*
 * Copyright 2012-2014 One Platform Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onepf.oms;

import org.jetbrains.annotations.Nullable;
import org.onepf.oms.util.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for manage stores SKUs.
 * Obtain instance of class by call {@link SkuManager#getInstance()}.
 *
 * Created by krozov on 7/27/14.
 */
public class SkuManager {

    /**
     * NOTE: used as sync object in related methods<br>
     * <p/>
     * storeName -> [ ... {app_sku1 -> store_sku1}, ... ]
     */
    private final Map<String, Map<String, String>> sku2storeSkuMappings
            = new ConcurrentHashMap<String, Map<String, String>>();

    /**
     * storeName -> [ ... {store_sku1 -> app_sku1}, ... ]
     */
    private final Map<String, Map<String, String>> storeSku2skuMappings
            = new ConcurrentHashMap<String, Map<String, String>>();

    /**
     * Map sku and storeSku for particular store.
     * <p/>
     * The best approach is to use SKU that unique in universe like <code>com.companyname.application.item</code>.
     * Such SKU fit most of stores so it doesn't need to be mapped.
     * <p/>
     * If best approach is not applicable use application inner SKU in code (usually it is SKU for Google Play)
     * and map SKU from other stores using this method. OpenIAB will map SKU in both directions,
     * so you can use only your inner SKU
     *
     * @param sku       - application inner SKU
     * @param storeSku  - shouldn't duplicate already mapped values
     * @param storeName - @see {@link IOpenAppstore#getAppstoreName()}
     *                  or {@link org.onepf.oms.OpenIabHelper#NAME_AMAZON}
     *                  {@link org.onepf.oms.OpenIabHelper#NAME_GOOGLE}
     *                  {@link org.onepf.oms.OpenIabHelper#NAME_TSTORE}
     */
    public void mapSku(String sku, String storeName, String storeSku) {
        Map<String, String> skuMap = sku2storeSkuMappings.get(storeName);
        if (skuMap == null) {
            skuMap = new HashMap<String, String>();
            sku2storeSkuMappings.put(storeName, skuMap);
        } else if (skuMap.get(sku) != null) {
            throw new IllegalArgumentException("Already specified SKU. sku: "
                    + sku + " -> storeSku: " + skuMap.get(sku));
        }

        Map<String, String> storeSkuMap = storeSku2skuMappings.get(storeName);
        if (storeSkuMap == null) {
            storeSkuMap = new HashMap<String, String>();
            storeSku2skuMappings.put(storeName, storeSkuMap);
        } else if (storeSkuMap.get(storeSku) != null) {
            throw new IllegalArgumentException("Ambiguous SKU mapping. You try to map sku: "
                    + sku + " -> storeSku: " + storeSku
                    + ", that is already mapped to sku: " + storeSkuMap.get(storeSku));
        }

        skuMap.put(sku, storeSku);
        storeSkuMap.put(storeSku, sku);
    }

    /**
     * Return previously mapped store SKU for specified inner SKU
     *
     * @param appstoreName Name of app store.
     * @param sku          Inner SKU
     * @return SKU used in store for specified inner SKU
     * @see #mapSku(String, String, String)
     */
    public String getStoreSku(final String appstoreName, String sku) {
        Map<String, String> storeSku = sku2storeSkuMappings.get(appstoreName);
        if (storeSku != null && storeSku.containsKey(sku)) {
            if (Logger.isLoggable()) {
                Logger.d("getStoreSku() using mapping for sku: ", sku, " -> ", storeSku.get(sku));
            }
            return storeSku.get(sku);
        }
        return sku;
    }

    /**
     * Return mapped application inner SKU using store name and store SKU.
     *
     * @see #mapSku(String, String, String)
     */
    public String getSku(final String appstoreName, String storeSku) {
        Map<String, String> skuMap = storeSku2skuMappings.get(appstoreName);
        if (skuMap != null && skuMap.containsKey(storeSku)) {
            if (Logger.isLoggable()) {
                Logger.d("getSku() restore sku from storeSku: ", storeSku, " -> ", skuMap.get(storeSku));
            }
            return skuMap.get(storeSku);
        }
        return storeSku;
    }

    /**
     * @param appstoreName for example {@link OpenIabHelper#NAME_AMAZON}
     * @return list of skus those have mappings for specified appstore
     */
    @Nullable
    public List<String> getAllStoreSkus(final String appstoreName) {
        Map<String, String> skuMap = sku2storeSkuMappings.get(appstoreName);
        return skuMap == null ? null : new ArrayList<String>(skuMap.values());
    }

    public static SkuManager getInstance() {
        return InstanceHolder.SKU_MANAGER;
    }

    private SkuManager() {
    }

    private static final class InstanceHolder {
        static final SkuManager SKU_MANAGER = new SkuManager();
    }
}
