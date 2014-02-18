package org.onepf.oms.appstore.fortumo;

import org.onepf.oms.appstore.onepfUtils.BaseInappProduct;

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
}
