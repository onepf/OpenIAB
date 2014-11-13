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

package org.onepf.oms.appstore.nokiaUtils;

import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;

public class NokiaResult extends IabResult {

    public static final int RESULT_NO_SIM = 9;

    public NokiaResult(final int response, final String message) {

        super(
                response == RESULT_NO_SIM ? IabHelper.BILLING_RESPONSE_RESULT_ERROR : response,
                response == RESULT_NO_SIM ? "No sim. " + message : message);
    }
}
