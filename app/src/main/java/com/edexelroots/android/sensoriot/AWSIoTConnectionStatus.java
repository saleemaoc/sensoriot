package com.edexelroots.android.sensoriot;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.widget.TextView;

import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;

public class AWSIoTConnectionStatus implements AWSIotMqttClientStatusCallback {

    private final String LOG_TAG = AWSIotMqttClientStatus.class.getCanonicalName();

    TextView mStatusReporter = null;
    Activity mActivity = null;

    public AWSIoTConnectionStatus(@NonNull Activity activity, @NonNull TextView reporter) {
        this.mActivity = activity;
        this.mStatusReporter = reporter;
    }

    @Override
    public void onStatusChanged(final AWSIotMqttClientStatus status, final Throwable throwable) {
        Utils.logE(LOG_TAG, "Status = " + String.valueOf(status));
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (status == AWSIotMqttClientStatus.Connecting) {
                    mStatusReporter.setText("Connecting...");

                } else if (status == AWSIotMqttClientStatus.Connected) {
                    mStatusReporter.setText("Connected!");

                } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                    if (throwable != null) {
                        Utils.logE(LOG_TAG, "Connection error." + throwable);
                    }
                    mStatusReporter.setText("Reconnecting");
                } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                    if (throwable != null) {
                        Utils.logE(LOG_TAG, "Connection error." + throwable);
                        throwable.printStackTrace();
                    }
                    mStatusReporter.setText("Disconnected");
                } else {
                    mStatusReporter.setText("Not Connected!");
                }
            }
        });
    }

    public void reportStatus(String msg) {
        mStatusReporter.setText(msg);
    }

    public void clearStatus() {
        mStatusReporter.setText("");
    }
}
