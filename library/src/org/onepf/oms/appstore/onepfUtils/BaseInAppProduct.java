package org.onepf.oms.appstore.onepfUtils;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Locale;

/**
 * Created by akarimova on 14.02.14.
 */

//todo add check
public class BaseInappProduct {
    //    //available types
//    public static final int TYPE_ITEM = 1;
//    public static final int TYPE_SUBS = 2;
    public static final String PUBLISHED = "published";
    public static final String UNPUBLISHED = "unpublished";

    //publish state
    private boolean published;
    //product id
    private String productId;
    //title
    private String baseTitle;
    private final HashMap<String, String> localeToTitleMap = new HashMap<String, String>();
    //description
    private String baseDescription;
    private final HashMap<String, String> localeToDescriptionMap = new HashMap<String, String>();
    //price
    private boolean autoFill;
    private float basePrice;
    private final HashMap<String, Float> localeToPrice = new HashMap<String, Float>();

    public BaseInappProduct() {
    }

    public BaseInappProduct(BaseInappProduct otherProduct) {
        this.published = otherProduct.published;
        this.productId = otherProduct.productId;
        this.baseTitle = otherProduct.baseTitle;
        this.baseDescription = otherProduct.baseDescription;
        this.basePrice = otherProduct.basePrice;
        this.localeToTitleMap.putAll(otherProduct.localeToTitleMap);
        this.localeToDescriptionMap.putAll(otherProduct.localeToDescriptionMap);
        this.localeToPrice.putAll(otherProduct.localeToPrice);
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(String published) {
        if (!published.equals(PUBLISHED) && !published.equals(UNPUBLISHED)) {
            throw new IllegalArgumentException("Wrong \"publish-state\" attr value " + published);
        }
        this.published = published.equals(PUBLISHED);
    }

    public String getBaseTitle() {
        return baseTitle;
    }

    public void setBaseTitle(String baseTitle) {
        this.baseTitle = baseTitle;
    }

    public String getBaseDescription() {
        return baseDescription;
    }

    public void setBaseDescription(String baseDescription) {
        this.baseDescription = baseDescription;
    }

    public void addTitleLocalization(String locale, String title) {
        localeToTitleMap.put(locale, title);
    }

    public String getTitleByLocale(String locale) {
        String mapValue = localeToTitleMap.get(locale);
        if (!TextUtils.isEmpty(mapValue)) {
            return mapValue;
        } else {
            return baseTitle;
        }
    }

    public String getTitle() {
        return getTitleByLocale(Locale.getDefault().toString());
    }

    public void addDescriptionLocalization(String locale, String description) {
        localeToDescriptionMap.put(locale, description);
    }

    public String getDescriptionByLocale(String locale) {
        String mapValue = localeToDescriptionMap.get(locale);
        if (!TextUtils.isEmpty(mapValue)) {
            return mapValue;
        } else {
            return baseDescription;
        }
    }

    public String getDescription() {
        return getDescriptionByLocale(Locale.getDefault().toString());
    }

    public void addCountryPrice(String countryCode, float price) {
        localeToPrice.put(countryCode, price);
    }

    public float getPriceByCountryCode(String countryCode) {
        Float mapValue = localeToPrice.get(countryCode);
        if (mapValue != null) {
            return mapValue;
        } else {
            return basePrice;
        }
    }

    public float getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(float basePrice) {
        this.basePrice = basePrice;
    }

    public boolean isAutoFill() {
        return autoFill;
    }

    public void setAutoFill(boolean autoFill) {
        this.autoFill = autoFill;
    }

    public void validateItem() {
        //todo
    }
}
