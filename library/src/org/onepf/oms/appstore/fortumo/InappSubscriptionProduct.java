package org.onepf.oms.appstore.fortumo;


/**
 * Created by akarimova on 17.02.14.
 */
public class InappSubscriptionProduct extends InappBaseProduct {
    public static final String ONE_MONTH = "oneMonth";
    public static final String ONE_YEAR = "oneYear";

    private String period;

    public InappSubscriptionProduct(InappBaseProduct otherProduct, String period) {
        super(otherProduct);
        this.period = period;
    }


    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        if (!period.equals(ONE_MONTH) && !period.equals(ONE_YEAR)) {
            throw new IllegalStateException("Wrong period value!");
        }
        this.period = period;
    }
}
