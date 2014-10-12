/*
 * Copyright 2012-2014 One Platform Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onepf.oms.appstore.fortumoUtils;

import android.text.TextUtils;

import org.jetbrains.annotations.NotNull;

import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;

/**
 * @author akarimova@onepf.org
 * @since 14.02.14
 */
public class InappBaseProduct {
    public static final String PUBLISHED = "published";
    public static final String UNPUBLISHED = "unpublished";
    //publish state
    boolean published;
    //product id
    String productId;
    //title
    String baseTitle;
    final HashMap<String, String> localeToTitleMap = new HashMap<String, String>();
    //description
    String baseDescription;
    final HashMap<String, String> localeToDescriptionMap = new HashMap<String, String>();
    //price
    boolean autoFill;
    float basePrice;
    final HashMap<String, Float> localeToPrice = new HashMap<String, Float>();

    public InappBaseProduct() {
    }

    public InappBaseProduct(@NotNull InappBaseProduct otherProduct) {
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

    public void setPublished(@NotNull String published) {
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

    public String getPriceDetails() {
        Locale defaultLocale = Locale.getDefault();
        Float mapValue = localeToPrice.get(defaultLocale.getCountry());
        float price = mapValue != null ? mapValue : basePrice;
        String symbol = mapValue != null ? Currency.getInstance(defaultLocale).getSymbol() : Currency.getInstance(Locale.US).getSymbol();
        return String.format("%.2f %s", price, symbol);
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
        //todo add own string builder with dividers
        StringBuilder builder = getValidateInfo();
        if (builder.length() > 0) {
            throw new IllegalStateException("in-app product is not valid: " + builder.toString());
        }
    }

    @NotNull
    protected StringBuilder getValidateInfo() {
        StringBuilder builder = new StringBuilder();
        if (TextUtils.isEmpty(productId)) {
            builder.append("product id is empty");
        }
        if (TextUtils.isEmpty(baseTitle)) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("base title is empty");
        }
        if (TextUtils.isEmpty(baseDescription)) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("base description is empty");
        }
        if (basePrice == 0) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("base price is not defined");
        }
        return builder;
    }

    @Override
    public String toString() {
        return "InappBaseProduct{" +
                "published=" + published +
                ", productId='" + productId + '\'' +
                ", baseTitle='" + baseTitle + '\'' +
                ", localeToTitleMap=" + localeToTitleMap +
                ", baseDescription='" + baseDescription + '\'' +
                ", localeToDescriptionMap=" + localeToDescriptionMap +
                ", autoFill=" + autoFill +
                ", basePrice=" + basePrice +
                ", localeToPrice=" + localeToPrice +
                '}';
    }
}
