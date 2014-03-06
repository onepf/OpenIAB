package org.onepf.oms.appstore.fortumo;


import android.text.TextUtils;

/**
 * @author akarimova@onepf.org
 * @since 17.02.14
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

    @Override
    public void validateItem() {
        final StringBuilder validateInfo = getValidateInfo();
        if (TextUtils.isEmpty(period) || !(period.equals(ONE_MONTH) || period.equals(ONE_YEAR))) {
            if (validateInfo.length() > 0) {
                validateInfo.append(", ");
            }
            validateInfo.append("period is not valid");
        }
        if (validateInfo.length() > 0) {
            throw new IllegalStateException("subscription product is not valid: " + validateInfo);
        }
    }

    @Override
    public String toString() {
        return "InappSubscriptionProduct{" +
                "published=" + published +
                ", productId='" + productId + '\'' +
                ", baseTitle='" + baseTitle + '\'' +
                ", localeToTitleMap=" + localeToTitleMap +
                ", baseDescription='" + baseDescription + '\'' +
                ", localeToDescriptionMap=" + localeToDescriptionMap +
                ", autoFill=" + autoFill +
                ", basePrice=" + basePrice +
                ", localeToPrice=" + localeToPrice +
                ", period='" + period + '\'' +
                '}';

    }
}
