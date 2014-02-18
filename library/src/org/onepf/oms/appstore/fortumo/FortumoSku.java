package org.onepf.oms.appstore.fortumo;

/**
 * Created by akarimova on 17.02.14.
 */
public class FortumoSku {
    private String id;
    private String serviceId;
    private String serviceInAppSecret;
    private boolean consumable;

    public FortumoSku(String id, boolean consumable, String serviceId, String serviceInAppSecret) {
        this.id = id;
        this.consumable = consumable;
        this.serviceId = serviceId;
        this.serviceInAppSecret = serviceInAppSecret;
    }

    public String getId() {
        return id;
    }

    public boolean isConsumable() {
        return consumable;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getServiceInAppSecret() {
        return serviceInAppSecret;
    }
}
