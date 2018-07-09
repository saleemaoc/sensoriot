package com.edexelroots.android.sensoriot;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;

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

    // Cognito pool ID
    public static final String COGNITO_POOL_ID = "ap-southeast-1:089b5c41-6644-44e3-a2db-5d9ae54703aa";

    // Region of AWS IoT
    public static final Regions MY_REGION = Regions.AP_SOUTHEAST_1;


    AWSIotMqttManager mqttManager;
    String clientId;

    public CognitoCachingCredentialsProvider credentialsProvider;
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

        if(Authentication.credentialsProvider != null) {
            credentialsProvider.setLogins(Authentication.credentialsProvider.getLogins());
        } else {
            Utils.logE(getClass().getName(), " >> credentialProvider is null");
        }

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);
    }

    public void connectToAWS() {

        Utils.logE(LOG_TAG, "clientId = " + clientId);

        try {
            mqttManager.connect(credentialsProvider, mAwsIoTConnectionStatusCallback);
        } catch (final Exception e) {
            Utils.logE(LOG_TAG, "Connection error." + e);
            mAwsIoTConnectionStatusCallback.reportStatus("Error! " + e.getMessage());
        }
    }

    public void publishToAwsIoT(String msg) {

        final String topic = "aws/things/controllerBox/shadow/update";

        try {
            mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
        } catch (Exception e) {
            Utils.logE(LOG_TAG, "Publish error." + e);
            mAwsIoTConnectionStatusCallback.reportStatus("Publish error!");
        }
    }

    public void disconnectMqtt() {

        try {
            mqttManager.disconnect();
        } catch (Exception e) {
            Utils.logE(LOG_TAG, "Disconnect error." + e);
        }
    }
}
