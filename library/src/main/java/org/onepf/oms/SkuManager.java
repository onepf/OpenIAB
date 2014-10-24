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

import android.text.TextUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.onepf.oms.appstore.NokiaStore;
import org.onepf.oms.appstore.SamsungApps;
import org.onepf.oms.util.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper to manage SKUs.
 * <p/>
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
     * Maps a store-specific SKU to an internal base SKU.
     * The best approach is to use SKU like <code>com.companyname.application.item</code>.
     * Such SKU fits most of stores, so it doesn't need to be mapped.
     * If this approach is not applicable, use an internal application SKU in the code (usually it is a SKU for Google Play)
     * and map SKU from other stores using this method. OpenIAB will map SKU in both directions,
     * so you can use only your internal SKU
     *
     * @param sku       - The internal SKU
     * @param storeSku  - The store-specific SKU. Shouldn't duplicate already mapped values
     * @param storeName - @see {@link IOpenAppstore#getAppstoreName()}
     *                  or {@link org.onepf.oms.OpenIabHelper#NAME_AMAZON}
     *                  {@link org.onepf.oms.OpenIabHelper#NAME_GOOGLE}
     * @return Instance of {@link org.onepf.oms.SkuManager}.
     * @throws org.onepf.oms.SkuMappingException If mapping can't be done.
     * @see #mapSku(String, java.util.Map)
     */
    @NotNull
    public SkuManager mapSku(String sku, String storeName, @NotNull String storeSku) {
        checkSkuMappingParams(sku, storeName, storeSku);

        Map<String, String> skuMap = sku2storeSkuMappings.get(storeName);
        if (skuMap == null) {
            skuMap = new HashMap<String, String>();
            sku2storeSkuMappings.put(storeName, skuMap);
        } else if (skuMap.containsKey(sku)) {
            throw new SkuMappingException("Already specified SKU. sku: "
                    + sku + " -> storeSku: " + skuMap.get(sku));
        }

        Map<String, String> storeSkuMap = storeSku2skuMappings.get(storeName);
        if (storeSkuMap == null) {
            storeSkuMap = new HashMap<String, String>();
            storeSku2skuMappings.put(storeName, storeSkuMap);
        } else if (storeSkuMap.get(storeSku) != null) {
            throw new SkuMappingException("Ambiguous SKU mapping. You try to map sku: "
                    + sku + " -> storeSku: " + storeSku
                    + ", that is already mapped to sku: " + storeSkuMap.get(storeSku));
        }

        skuMap.put(sku, storeSku);
        storeSkuMap.put(storeSku, sku);
        return this;
    }

    private static void checkSkuMappingParams(String storeName, @NotNull String storeSku) {
        if (TextUtils.isEmpty(storeName)) {
            throw SkuMappingException.newInstance(SkuMappingException.REASON_STORE_NAME);
        }

        if (TextUtils.isEmpty(storeSku)) {
            throw SkuMappingException.newInstance(SkuMappingException.REASON_STORE_SKU);
        }

        if (OpenIabHelper.NAME_SAMSUNG.equals(storeName)) {
            SamsungApps.checkSku(storeSku);
        }

        if (OpenIabHelper.NAME_NOKIA.equals(storeName)) {
            NokiaStore.checkSku(storeSku);
        }
    }

    private static void checkSkuMappingParams(String sku, String storeName, @NotNull String storeSku) {
        if (TextUtils.isEmpty(sku)) {
            throw SkuMappingException.newInstance(SkuMappingException.REASON_SKU);
        }
        checkSkuMappingParams(storeName, storeSku);
    }

    /**
     * Maps a base internal SKU to a store-specific SKU.
     * The best approach is to use SKU like <code>com.companyname.application.item</code>.
     * Such SKU fits most of stores so it doesn't need to be mapped.
     * If this approach is not applicable, use application internal SKU in the code (usually it is a SKU for Google Play)
     * and map SKU from other stores using this method. OpenIAB will map SKU in both directions,
     * so you can use only your internal SKU
     *
     * @param sku       - The application internal SKU.
     * @param storeSkus - The map of "store name -> sku id in store"
     * @return Instance of {@link org.onepf.oms.SkuManager}.
     * @throws org.onepf.oms.SkuMappingException If mapping can't done.
     * @see org.onepf.oms.SkuManager#mapSku(String, String, String)
     */
    @NotNull
    public SkuManager mapSku(String sku, @Nullable Map<String, String> storeSkus) {
        if (storeSkus == null) {
            throw new SkuMappingException("Store skus map can't be null.");
        }

        for (Map.Entry<String, String> entry : storeSkus.entrySet()) {
            mapSku(sku, entry.getKey(), entry.getValue());
        }
        return this;
    }

    /**
     * Returns a store-specific SKU by the base internal SKU.
     *
     * @param appstoreName The name of an app store.
     * @param sku          The internal SKU.
     * @return store-specific SKU by a base internal one.
     * @throws java.lang.IllegalArgumentException When appstoreName or sku param is empty or null value.
     * @see #mapSku(String, String, String)
     */
    @NotNull
    public String getStoreSku(@NotNull String appstoreName, @NotNull String sku) {
        if (TextUtils.isEmpty(appstoreName)) {
            throw SkuMappingException.newInstance(SkuMappingException.REASON_STORE_NAME);
        }
        if (TextUtils.isEmpty(sku)) {
            throw SkuMappingException.newInstance(SkuMappingException.REASON_SKU);
        }

        Map<String, String> storeSku = sku2storeSkuMappings.get(appstoreName);
        if (storeSku != null && storeSku.containsKey(sku)) {
            final String s = storeSku.get(sku);
            Logger.d("getStoreSku() using mapping for sku: ", sku, " -> ", s);
            return s;
        }
        return sku;
    }

    /**
     * Returns a base internal SKU by a store-specific SKU.
     *
     * @throws java.lang.IllegalArgumentException If the store name or a store SKU is empty or null.
     * @see #mapSku(String, String, String)
     */
    @NotNull
    public String getSku(@NotNull String appstoreName, @NotNull String storeSku) {
        checkSkuMappingParams(appstoreName, storeSku);

        Map<String, String> skuMap = storeSku2skuMappings.get(appstoreName);
        if (skuMap != null && skuMap.containsKey(storeSku)) {
            final String s = skuMap.get(storeSku);
            Logger.d("getSku() restore sku from storeSku: ", storeSku, " -> ", s);
            return s;
        }
        return storeSku;
    }

    /**
     * Returns a list of SKU for a store.
     *
     * @param appstoreName The app store name.
     * @return list of SKU that mapped to the store. Null if the store has no mapped SKUs.
     * @throws java.lang.IllegalArgumentException If the store name is null or empty.
     * @see #mapSku(String, String, String)
     */
    @Nullable
    public List<String> getAllStoreSkus(@NotNull final String appstoreName) {
        if (TextUtils.isEmpty(appstoreName)) {
            throw SkuMappingException.newInstance(SkuMappingException.REASON_STORE_NAME);
        }

        Map<String, String> skuMap = sku2storeSkuMappings.get(appstoreName);
        return skuMap == null ? null :
                Collections.unmodifiableList(new ArrayList<String>(skuMap.values()));
    }

    /**
     * @return the current instance of {@link org.onepf.oms.SkuManager}.
     */
    @NotNull
    public static SkuManager getInstance() {
        return InstanceHolder.SKU_MANAGER;
    }

    private SkuManager() {
    }

    private static final class InstanceHolder {
        static final SkuManager SKU_MANAGER = new SkuManager();
    }
}
