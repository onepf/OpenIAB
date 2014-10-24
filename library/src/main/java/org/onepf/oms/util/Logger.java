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

import org.jetbrains.annotations.NotNull;

/**
 * Simple wrapper for {@link android.util.Log}.
 * <br/>
 * To enable logging, call {@link org.onepf.oms.util.Logger#setLoggable(boolean)} or use <code>adb shell setprop log.tag.YOUR_TAG<code>.
 *
 * @author Kirill Rozov
 * @since 25.07.14
 */
public final class Logger {

    private Logger() {
    }

    /**
     * Default log tag. To change the tag value, use {@link org.onepf.oms.util.Logger#setLogTag(String)}.
     */
    public static final String LOG_TAG = "OpenIAB";

    @NotNull
    private static String logTag = LOG_TAG;

    private static boolean loggable;

    /**
     * Checks whether logging is enabled.
     *
     * @return true if logging is enabled.
     */
    public static boolean isLoggable() {
        return loggable;
    }

    /**
     * Enables/disables logging.
     */
    public static void setLoggable(boolean loggable) {
        Logger.loggable = loggable;
    }

    @Deprecated
    public static void init() {
    }

    /**
     * Sets the log tag.
     *
     * @param logTag The new tag value.
     */
    public static void setLogTag(final String logTag) {
        Logger.logTag = TextUtils.isEmpty(logTag) ? LOG_TAG : logTag;
    }

    public static void d(@NotNull Object... values) {
        if (loggable || Log.isLoggable(logTag, Log.DEBUG)) {
            Log.d(logTag, TextUtils.join("", values));
        }
    }

    public static void i(@NotNull Object... values) {
        if (loggable || Log.isLoggable(logTag, Log.INFO)) {
            Log.i(logTag, TextUtils.join("", values));
        }
    }

    public static void i(String msg) {
        if (loggable || Log.isLoggable(logTag, Log.INFO)) {
            Log.i(logTag, msg);
        }
    }

    public static void d(String msg) {
        if (loggable || Log.isLoggable(logTag, Log.DEBUG)) {
            Log.d(logTag, msg);
        }
    }

    @Deprecated
    public static void dWithTimeFromUp(String msg) {
        d(msg);
    }

    @Deprecated
    public static void dWithTimeFromUp(Object... msgs) {
        d(msgs);
    }

    public static void e(@NotNull Object... msgs) {
        if (loggable || Log.isLoggable(logTag, Log.ERROR)) {
            Log.e(logTag, TextUtils.join("", msgs));
        }
    }

    public static void e(String msg, Throwable e) {
        if (loggable || Log.isLoggable(logTag, Log.ERROR)) {
            Log.e(logTag, msg, e);
        }
    }


    public static void e(String msg) {
        if (loggable || Log.isLoggable(logTag, Log.ERROR)) {
            Log.e(logTag, msg);
        }
    }

    public static void e(Throwable e, @NotNull Object... msgs) {
        if (loggable || Log.isLoggable(logTag, Log.ERROR)) {
            Log.e(logTag, TextUtils.join("", msgs), e);
        }
    }

    public static void w(String msg) {
        if (loggable || Log.isLoggable(logTag, Log.WARN)) {
            Log.w(logTag, msg);
        }
    }

    public static void w(String msg, Throwable e) {
        if (loggable || Log.isLoggable(logTag, Log.WARN)) {
            Log.w(logTag, msg, e);
        }
    }

    public static void v(@NotNull Object... msgs) {
        if (loggable || Log.isLoggable(logTag, Log.VERBOSE)) {
            Log.v(logTag, TextUtils.join("", msgs));
        }
    }

    public static void w(@NotNull Object... values) {
        if (loggable || Log.isLoggable(logTag, Log.VERBOSE)) {
            Log.w(logTag, TextUtils.join("", values));
        }
    }
}
