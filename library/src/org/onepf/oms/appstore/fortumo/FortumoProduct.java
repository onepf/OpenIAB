package org.onepf.oms.appstore.fortumo;

import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.SkuDetails;
import org.onepf.oms.appstore.onepfUtils.BaseInappProduct;

import java.util.Locale;

/**
 * Created by akarimova on 17.02.14.
 */
public class FortumoProduct extends BaseInappProduct {
    private boolean consumable;
    private String serviceId;
    private String inAppSecret;

    public FortumoProduct(BaseInappProduct otherProduct, boolean consumable, String serviceId, String inAppSecret) {
        super(otherProduct);
        this.consumable = consumable;
        this.serviceId = serviceId;
        this.inAppSecret = inAppSecret;
    }

    public boolean isConsumable() {
        return consumable;
    }

    public void setConsumable(boolean consumable) {
        this.consumable = consumable;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getInAppSecret() {
        return inAppSecret;
    }

    public void setInAppSecret(String inAppSecret) {
        this.inAppSecret = inAppSecret;
    }

    public SkuDetails toSkuDetails(String price) {
        return new SkuDetails(OpenIabHelper.ITEM_TYPE_INAPP, getProductId(), getTitle(), price, getDescription());
    }
}
