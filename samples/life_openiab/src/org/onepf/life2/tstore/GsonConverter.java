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

package org.onepf.life2.tstore;

import com.google.gson.Gson;

import java.lang.Override;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 05.04.13
 */
public class GsonConverter implements Converter {
    private final Gson mGson = new Gson();

    @Override
    public String toJson(CommandRequest r) {
        return mGson.toJson(r);
    }

    @Override
    public Response fromJson(String json) {
        return mGson.fromJson(json, Response.class);
    }

    @Override
    public VerifyReceipt fromJson2VerifyReceipt(String json) {
        return mGson.fromJson(json, VerifyReceipt.class);
    }

}
