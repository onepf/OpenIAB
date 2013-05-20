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

package org.onepf.life2.oms.appstore.tstoreUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Ruslan Sayfutdinov
 * Date: 05.04.13
 */
public class CommandRequest {
    public final String method;
    public final Parameter param;

    private CommandRequest(final String method, final Parameter param) {
        this.method = method;
        this.param = param;
    }

    public static CommandRequest makeRequest(final String method,
                                             final Parameter param) {
        return new CommandRequest(method, param);
    }

    public static class Parameter {
        public final String appid;
        public final List<String> product_id = new ArrayList<String>();
        public final String action;

        private Parameter(final String appid, String... pids) {
            this.appid = appid;
            this.action = null;
            if (pids != null) {
                for (String arg : pids) {
                    this.product_id.add(arg);
                }
            }

        }

        private Parameter(final String appid, final String action,
                          String... pids) {
            this.appid = appid;
            this.action = action;
            if (pids != null) {
                for (String arg : pids) {
                    this.product_id.add(arg);
                }
            }
        }

        public static Parameter makeParam(final String appid, String... pids) {
            return new Parameter(appid, pids);
        }

        public static Parameter makeParam(final String appid,
                                          final String action, String pids[]) {
            return new Parameter(appid, action, pids);
        }
    }
}
