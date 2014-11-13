/**
 * Copyright 2014 Skubit
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.onepf.oms.appstore.skubitUtils;

public class BillingResponseCodes {

    public static final int RESULT_BILLING_UNAVAILABLE = 3;// - this billing API
    // version is not
    // supported for the
    // type requested

    public static final int RESULT_DEVELOPER_ERROR = 5;// - invalid arguments
    // provided to the API

    public static final int RESULT_ERROR = 6;// - Fatal error during the API
    // action

    public static final int RESULT_ITEM_ALREADY_OWNED = 7;// - Failure to
    // purchase since
    // item is
    // already owned

    public static final int RESULT_ITEM_NOT_OWNED = 8;// - Failure to consume
    // since item is not
    // owned

    public static final int RESULT_ITEM_UNAVAILABLE = 4;// - requested SKU is
    // not available for
    // purchase

    public static final int RESULT_NOT_PROMO_ELIGIBLE = 10;

    public static final int RESULT_OK = 0;// - success

    public static final int RESULT_PROMO_ELIGIBLE = 9;

    public static final int RESULT_SERVICE_UNAVAILABLE = 2;

    public static final int RESULT_USER_CANCELED = 1;// - user pressed back or
    // canceled a dialog

    public static final int RESULT_INSUFFICIENT_FUNDS = 100;

}
