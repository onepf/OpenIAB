package org.onepf.oms.appstore.nokiaUtils;

import org.onepf.oms.SkuMappingException;

/**
 * Created by krozov on 03.09.14.
 */
public class NokiaSkuFormatException extends SkuMappingException {
    public NokiaSkuFormatException() {
        super("Nokia Store SKU can contain only digits.");
    }
}
