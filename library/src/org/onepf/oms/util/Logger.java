package org.onepf.oms.util;

import android.text.TextUtils;
import android.util.Log;

public class Logger {
    private static final String TAG = "OpenIabLibrary";

    //Is debug enabled?
    private static boolean debuggable = false;

    private static long started = System.currentTimeMillis();

    private Logger() {

    }

    public static void init() {

    }

    public static void d(Object... values) {
        if (debuggable) {
            Log.d(TAG, TextUtils.join("", values));
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
