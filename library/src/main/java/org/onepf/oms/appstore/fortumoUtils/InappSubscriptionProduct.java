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

/**
 * @author akarimova@onepf.org
 * @since 17.02.14
 */
public class InappSubscriptionProduct extends InappBaseProduct {
    public static final String ONE_MONTH = "oneMonth";
    public static final String ONE_YEAR = "oneYear";

    private String period;

    public InappSubscriptionProduct(@NotNull InappBaseProduct otherProduct, String period) {
        super(otherProduct);
        this.period = period;
    }


    public String getPeriod() {
        return period;
    }

    public void setPeriod(@NotNull String period) {
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

    @NotNull
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
