package org.onepf.oms;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;

/**
 * Created by krozov on 01.09.14.
 */
public class SkuMappingException extends IllegalArgumentException {

    public SkuMappingException() {
        super("Error while map sku.");
    }

    public SkuMappingException(String detailMessage) {
        super(detailMessage);
    }

    @NotNull
    public static SkuMappingException newInstance(@MagicConstant(
            intValues = {REASON_SKU, REASON_STORE_NAME, REASON_STORE_SKU}) int reason) {
        switch (reason) {
            case REASON_SKU:
                return new SkuMappingException("Sku can't be null or empty value.");

            case REASON_STORE_NAME:
                return new SkuMappingException("Store name can't be null or empty value.");

            case REASON_STORE_SKU:
                return new SkuMappingException("Store sku can't be null or empty value.");

            default:
                return new SkuMappingException();
        }
    }

    public static final int REASON_SKU = 1;
    public static final int REASON_STORE_NAME = 2;
    public static final int REASON_STORE_SKU = 3;
}
