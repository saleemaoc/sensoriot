package com.edexelroots.android.sensoriot;

import android.content.Context;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

public class Utils {

    public static final boolean IS_RELEASE = true;

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

    public static boolean isConnected(Context c) {
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }


    public static class ViewWeightAnimationWrapper {
        private View view;

        public ViewWeightAnimationWrapper(View view) {
            if (view.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                this.view = view;
            } else {
                throw new IllegalArgumentException("The view should have LinearLayout as parent");
            }
        }

        public void setWeight(float weight) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
            params.weight = weight;
            view.getParent().requestLayout();
        }

        public float getWeight() {
            return ((LinearLayout.LayoutParams) view.getLayoutParams()).weight;
        }
    }
}
