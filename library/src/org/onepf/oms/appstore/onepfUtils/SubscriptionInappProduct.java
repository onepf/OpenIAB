package org.onepf.oms.appstore.onepfUtils;

/**
 * Created by akarimova on 17.02.14.
 */
public class SubscriptionInappProduct extends BaseInappProduct {
    public static final String ONE_MONTH = "oneMonth";
    public static final String ONE_YEAR = "oneYear";

    private String period;

    public SubscriptionInappProduct(BaseInappProduct otherProduct, String period) {
        super(otherProduct);
        this.period = period;
    }


    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }
}
