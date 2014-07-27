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
 * @author Kirill Rozov
 * @since 25.07.14
 */
public final class Logger {
    private static final String TAG = "OpenIabLibrary";

    //Is debug enabled?
    private static boolean debuggable;

    private static long started = System.currentTimeMillis();

    private Logger() {
    }

    public static void init() {
        started = System.currentTimeMillis();
    }

    public static void d(Object... values) {
        if (debuggable) {
            Log.d(TAG, TextUtils.join("", values));
        }
    }

    public static void i(Object... values) {
        if (debuggable) {
            Log.i(TAG, TextUtils.join("", values));
        }
    }

    public static void i(String msg) {
        if (debuggable) {
            Log.i(TAG, msg);
        }
    }

    public static void d(String msg) {
        if (debuggable) {
            Log.d(TAG, msg);
        }
    }

    public static void dWithTimeFromUp(String msg) {
        if (debuggable) {
            Log.d(TAG, in() + ' ' + msg);
        }
    }

    public static void dWithTimeFromUp(Object... msgs) {
        if (debuggable) {
            Log.d(TAG, in() + ' ' + TextUtils.join("", msgs));
        }
    }

    public static void e(Object... msgs) {
        if (debuggable) {
            Log.e(TAG, TextUtils.join("", msgs));
        }
    }

    public static void e(String msg, Throwable e) {
        if (debuggable) {
            Log.e(TAG, msg, e);
        }
    }

    public static void e(String msg) {
        if (debuggable) {
            Log.e(TAG, msg);
        }
    }

    public static void e(Throwable e, Object... msgs) {
        if (debuggable) {
            Log.e(TAG, TextUtils.join("", msgs), e);
        }
    }

    public static void w(String msg) {
        if (debuggable) {
            Log.w(TAG, msg);
        }
    }

    public static void w(String msg, Throwable e) {
        if (debuggable) {
            Log.w(TAG, msg, e);
        }
    }

    public static void v(Object... msgs) {
        if (debuggable) {
            Log.v(TAG, TextUtils.join("", msgs));
        }
    }

    public static void w(Object... values) {
        if (debuggable) {
            Log.w(TAG, TextUtils.join("", values));
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
