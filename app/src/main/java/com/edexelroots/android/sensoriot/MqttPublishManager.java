package com.edexelroots.android.sensoriot;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Regions;

import java.util.UUID;

public class MqttPublishManager {

/*
    // Initialize the Amazon Cognito credentials provider
    CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
            getApplicationContext(),
            "ap-southeast-1:0d8c48a7-1c58-4a8a-ae47-596cffc679b5", // Identity pool ID
            Regions.AP_SOUTHEAST_1 // Region
    );
*/

    static final String LOG_TAG = MqttPublishManager.class.getCanonicalName();

    // --- Constants to modify per your configuration ---

    // Customer specific IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com,
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a6mohze0r9216.iot.ap-southeast-1.amazonaws.com";

    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "ap-southeast-1:0d8c48a7-1c58-4a8a-ae47-596cffc679b5";

    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.AP_SOUTHEAST_1;


    AWSIotMqttManager mqttManager;
    String clientId;

    CognitoCachingCredentialsProvider credentialsProvider;
    AWSIoTConnectionStatus mAwsIoTConnectionStatusCallback = null;

    Activity mContext = null;

    public MqttPublishManager(@NonNull Activity context, @NonNull AWSIoTConnectionStatus connectionStatusCallback) {
        mContext = context;
        this.mAwsIoTConnectionStatusCallback = connectionStatusCallback;

        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        clientId = UUID.randomUUID().toString();
        // tvClientId.setText(clientId);

        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                context, // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);

        // The following block uses a Cognito credentials provider for authentication with AWS IoT.
        // TODO - use a checkbox to enable/disable publishing to AWS IoT
/*        new Thread(new Runnable() {
            @Override
            public void run() {
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnConnect.setEnabled(true);
                    }
                });
            }
        }).start();*/
    }

    public void connectToAWS() {

        Utils.logE(LOG_TAG, "clientId = " + clientId);

        try {
            mqttManager.connect(credentialsProvider, mAwsIoTConnectionStatusCallback);
        } catch (final Exception e) {
            Utils.logE(LOG_TAG, "Connection error." + e);
            // tvStatus.setText("Error! " + e.getMessage());
        }
    }

    public void publishToAwsIoT(String msg) {

        final String topic = "aws/things/controllerBox/shadow/update";

        try {
            mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
        } catch (Exception e) {
            Utils.logE(LOG_TAG, "Publish error." + e);
        }

    }

    public void disconnectMqtt() {

        try {
            mqttManager.disconnect();
        } catch (Exception e) {
            Utils.logE(LOG_TAG, "Disconnect error." + e);
        }

    }


        /*        View.OnClickListener subscribeClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String topic = "aws/things/controllerBox/shadow/update";

                Utils.logE(LOG_TAG, "topic = " + topic);

                try {
                    mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                            new AWSIotMqttNewMessageCallback() {
                                @Override
                                public void onMessageArrived(final String topic, final byte[] data) {
                                    mContext.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                String message = new String(data, "UTF-8");
                                                Utils.logE(LOG_TAG, "Message arrived:");
                                                Utils.logE(LOG_TAG, "   Topic: " + topic);
                                                Utils.logE(LOG_TAG, " Message: " + message);

                                                // tvLastMessage.setText(message);

                                            } catch (UnsupportedEncodingException e) {
                                                Utils.logE(LOG_TAG, "Message encoding error." + e);
                                            }
                                        }
                                    });
                                }
                            });
                } catch (Exception e) {
                    Utils.logE(LOG_TAG, "Subscription error." + e);
                }
            }
        };*/
}
