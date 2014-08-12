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

package org.onepf.oms.util;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * Different utils for arrays, collections and maps.
 *
 * @author Kirill Rozov
 */
public final class CollectionUtils {
    /**
     * Verify does map null or empty.
     *
     * @param map Map to verify.
     * @return Does map null or empty.
     */
    public static boolean isEmpty(@Nullable Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Verify does collection null or empty.
     *
     * @param collection Collection to verify.
     * @return Does collection null or empty.
     */
    public static boolean isEmpty(@Nullable Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Verify does array null or empty.
     *
     * @param array Array to verify.
     * @return Does array null or empty.
     */
    public static boolean isEmpty(@Nullable Object[] array) {
        return array == null || array.length == 0;
    }

    private CollectionUtils() {
    }
}
