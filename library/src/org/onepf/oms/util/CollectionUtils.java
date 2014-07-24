package org.onepf.oms.util;

import java.util.Collection;
import java.util.Map;

public final class CollectionUtils {
    public static boolean isEmpty(Map<?, ?> list) {
        return list == null || list.isEmpty();
    }

    public static boolean isEmpty(Collection<?> list) {
        return list == null || list.isEmpty();
    }

    public static <E> boolean isEmpty(E[] array) {
        return array == null || array.length == 0;
    }

    private CollectionUtils(){

    }
}
