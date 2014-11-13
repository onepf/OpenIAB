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
 * Utility class for collections and primitive arrays.
 *
 * @author Kirill Rozov
 */
public final class CollectionUtils {
    /**
     * Checks if a map is empty or null.
     *
     * @param map The map to test.
     * @return true if the map is empty or null.
     */
    public static boolean isEmpty(@Nullable Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Checks if a collection of Objects is empty or null.
     *
     * @param collection The collection to test.
     * @return true if the collection is empty or null.
     */
    public static boolean isEmpty(@Nullable Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Checks if an array of Objects is empty or null.
     *
     * @param array The array to test.
     * @return true if the array is empty or null.
     */
    public static boolean isEmpty(@Nullable Object[] array) {
        return array == null || array.length == 0;
    }

    private CollectionUtils() {
    }
}
