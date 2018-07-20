package com.edexelroots.android.sensoriot;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.auth.core.IdentityHandler;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MqttPublishManager {

    static final String LOG_TAG = MqttPublishManager.class.getCanonicalName();

    // Customer specific IoT endpoint -- use aws iot describe-endpoint
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a2j4ibv1tgowau.iot.ap-southeast-1.amazonaws.com"; // tom
    public static final String COGNITO_POOL_ID = "ap-southeast-1:8267e1a6-d198-4c7c-acff-f05f284c4181"; // tom
    public static String USER_POOL_ID = "ap-southeast-1_PVcTZSD5R"; // tom
    public static String policyName = "iot_device"; //tom

    // Region of AWS IoT
    public static final Regions MY_REGION = Regions.AP_SOUTHEAST_1;


    AWSIotMqttManager mqttManager;
    String clientId;

    private CognitoCachingCredentialsProvider credentialsProvider;
    AWSIoTConnectionStatus mAwsIoTConnectionStatusCallback = null;

    Activity mContext = null;

    public MqttPublishManager(@NonNull Activity context, @NonNull AWSIoTConnectionStatus connectionStatusCallback) {
        mContext = context;
        this.mAwsIoTConnectionStatusCallback = connectionStatusCallback;

        // MQTT client IDs are required to be unique per AWS IoT account.
        clientId = UUID.randomUUID().toString();
    }

    public void setCredentialsProvider(CognitoCachingCredentialsProvider cp) {
        this.credentialsProvider = cp;
    }

    public void connectToAWS(CognitoCachingCredentialsProvider cp) {
        Utils.logE(LOG_TAG, "clientId = " + clientId);
        Utils.logE(LOG_TAG, "Credential provider = " + cp.getIdentityId());
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);
        try {
            mqttManager.connect(cp, mAwsIoTConnectionStatusCallback);
        } catch (final Exception e) {
            Utils.logE(LOG_TAG, "Connection error." + e);
            mAwsIoTConnectionStatusCallback.reportStatus("Error! " + e.getMessage());
        }
    }

    public void publishToAwsIoT(String msg) {
        final String topic = "$aws/things/controllerBox/shadow/update";
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

    protected void setupSession() {
        final IdentityManager identityManager = IdentityManager.getDefaultIdentityManager();
        final CognitoUserPool userPool = new CognitoUserPool(mContext, identityManager.getConfiguration());
        userPool.getCurrentUser().getSessionInBackground(new AWSAuthHandler(identityManager));
    }


    class AWSAuthHandler implements AuthenticationHandler {

        IdentityManager identityManager = null;

        public AWSAuthHandler(IdentityManager im) {
            this.identityManager = im;
        }

        @Override
        public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {
            final String idToken = userSession.getIdToken().getJWTToken();
            identityManager.getUserID(new IdentityHandler() {
                @Override
                public void onIdentityId(String identityId) {
                    new CredentialProvider(MqttPublishManager.this, mContext).execute(idToken, identityId);
                }

                @Override
                public void handleError(Exception exception) {
                    Utils.logE(getClass().getName(), exception.getMessage());
                    exception.printStackTrace();
                }
            });
        }

        @Override
        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String userId) {

        }

        @Override
        public void getMFACode(MultiFactorAuthenticationContinuation continuation) {

        }

        @Override
        public void authenticationChallenge(ChallengeContinuation continuation) {

        }

        @Override
        public void onFailure(Exception exception) {

        }
    }


}
