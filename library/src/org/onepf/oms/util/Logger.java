package org.onepf.oms.util;

import android.util.Log;

public class Logger {
    private static final String TAG = "OpenIabLibrary";

    //Is debug enabled?
    private static boolean debuggable = false;

    public static void d(String msg){
        if (debuggable){
            Log.d(TAG, msg);
        }
    }

    public static void e(String msg){
        if (debuggable){
            Log.e(TAG, msg);
        }
    }

    public static void e(String msg, Throwable e){
        if (debuggable){
            Log.e(TAG, msg, e);
        }
    }

    public static void w(String msg){
        if (debuggable){
            Log.w(TAG, msg);
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

    private Logger(){

    }
}
