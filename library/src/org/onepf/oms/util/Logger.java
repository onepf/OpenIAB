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

package org.onepf.oms.util;

import android.text.TextUtils;
import android.util.Log;

/**
 * Utility for log events.
 *
 * @author Kirill Rozov
 * @since 25.07.14
 */
public final class Logger {
    public static final String LOG_TAG = "OpenIabLibrary";

    private static boolean debuggable;

    private static long started = System.currentTimeMillis();

    private Logger() {
    }

    public static void init() {
        started = System.currentTimeMillis();
    }

    public static void d(Object... values) {
        if (debuggable && Log.isLoggable(LOG_TAG, Log.DEBUG)) {
            Log.d(LOG_TAG, TextUtils.join("", values));
        }
    }

    public static void i(Object... values) {
        if (debuggable && Log.isLoggable(LOG_TAG, Log.INFO)) {
            Log.i(LOG_TAG, TextUtils.join("", values));
        }
    }

    public static void i(String msg) {
        if (debuggable && Log.isLoggable(LOG_TAG, Log.INFO)) {
            Log.i(LOG_TAG, msg);
        }
    }

    public static void d(String msg) {
        if (debuggable && Log.isLoggable(LOG_TAG, Log.DEBUG)) {
            Log.d(LOG_TAG, msg);
        }
    }

    public static void dWithTimeFromUp(String msg) {
        if (debuggable && Log.isLoggable(LOG_TAG, Log.DEBUG)) {
            Log.d(LOG_TAG, in() + ' ' + msg);
        }
    }

    public static void dWithTimeFromUp(Object... msgs) {
        if (debuggable && Log.isLoggable(LOG_TAG, Log.DEBUG)) {
            Log.d(LOG_TAG, in() + ' ' + TextUtils.join("", msgs));
        }
    }

    public static void e(Object... msgs) {
        if (debuggable && Log.isLoggable(LOG_TAG, Log.ERROR)) {
            Log.e(LOG_TAG, TextUtils.join("", msgs));
        }
    }

    public static void e(String msg, Throwable e) {
        if (debuggable && Log.isLoggable(LOG_TAG, Log.ERROR)) {
            Log.e(LOG_TAG, msg, e);
        }
    }

    public static void e(String msg) {
        if (debuggable && Log.isLoggable(LOG_TAG, Log.ERROR)) {
            Log.e(LOG_TAG, msg);
        }
    }

    public static void e(Throwable e, Object... msgs) {
        if (debuggable && Log.isLoggable(LOG_TAG, Log.ERROR)) {
            Log.e(LOG_TAG, TextUtils.join("", msgs), e);
        }
    }

    public static void w(String msg) {
        if (debuggable && Log.isLoggable(LOG_TAG, Log.WARN)) {
            Log.w(LOG_TAG, msg);
        }
    }

    public static void w(String msg, Throwable e) {
        if (debuggable && Log.isLoggable(LOG_TAG, Log.WARN)) {
            Log.w(LOG_TAG, msg, e);
        }
    }

    public static void v(Object... msgs) {
        if (debuggable && Log.isLoggable(LOG_TAG, Log.VERBOSE)) {
            Log.v(LOG_TAG, TextUtils.join("", msgs));
        }
    }

    public static void w(Object... values) {
        if (debuggable && Log.isLoggable(LOG_TAG, Log.VERBOSE)) {
            Log.w(LOG_TAG, TextUtils.join("", values));
        }
    }

    public static boolean isDebuggable() {
        return debuggable;
    }

    public static void setDebuggable(boolean debuggable) {
        Logger.debuggable = debuggable;
    }

    private static String in() {
        return "in: " + (System.currentTimeMillis() - started);
    }
}
