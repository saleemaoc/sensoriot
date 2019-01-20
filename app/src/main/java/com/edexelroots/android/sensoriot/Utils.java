package com.edexelroots.android.sensoriot;

import android.content.Context;
import android.content.res.Configuration;
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

    public static boolean isPortraitMode(Context mContext) {
        int orientation = mContext.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return false;
        }
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return true;
        }

        return false;
    }
}
