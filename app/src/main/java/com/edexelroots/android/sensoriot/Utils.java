package com.edexelroots.android.sensoriot;

import android.util.Log;

public class Utils {

    public static final boolean IS_RELEASE = false;

    public static void logE(String tag, String msg) {
        if (IS_RELEASE) {
            return;
        }
        Log.e(tag, msg);
    }

    public static void logD(String tag, String msg) {
        if (IS_RELEASE) {
            return;
        }

        Log.d(tag, msg);
    }
}
