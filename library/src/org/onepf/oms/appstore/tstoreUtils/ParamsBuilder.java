/*******************************************************************************
 * Copyright 2013 One Platform Foundation
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 ******************************************************************************/

package org.onepf.oms.appstore.tstoreUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 05.04.13
 */

public final class ParamsBuilder {
    public static final String KEY_APPID = "appid";
    public static final String KEY_PID = "product_id";
    public static final String KEY_PNAME = "product_name";
    public static final String KEY_TID = "tid";
    public static final String KEY_BPINFO = "bpinfo";
    private ConcurrentHashMap<String, String> mParams = null;

    public ParamsBuilder() {
        mParams = new ConcurrentHashMap<String, String>();
    }

    public ParamsBuilder put(Map<String, String> source) {
        for (Map.Entry<String, String> entry : source.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }

        return this;
    }

    public ParamsBuilder put(String k, String v) {
        if (k != null && v != null) {
            if (mParams.contains(k)) {
                mParams.replace(k, v);
            } else {
                mParams.put(k, v);
            }
        }
        return this;
    }

    public ParamsBuilder remove(String k) {
        mParams.remove(k);
        return this;
    }

    public String build() {
        return toString();
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        for (ConcurrentHashMap.Entry<String, String> entry : mParams.entrySet()) {
            if (result.length() > 0)
                result.append("&");

            result.append(entry.getKey());
            result.append("=");
            result.append(entry.getValue());
        }

        return result.toString();
    }
}
