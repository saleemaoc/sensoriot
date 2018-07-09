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

    }

    public void connectToAWS() {
        Utils.logE(LOG_TAG, "clientId = " + clientId);
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);
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

    protected void setupSession() {
        final IdentityManager identityManager = IdentityManager.getDefaultIdentityManager();
        final CognitoUserPool userPool = new CognitoUserPool(mContext, identityManager.getConfiguration());
        userPool.getCurrentUser().getSessionInBackground(new AuthenticationHandler() {
            @Override
            public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {
                final String idToken = userSession.getIdToken().getJWTToken();
                identityManager.getUserID(new IdentityHandler() {
                    @Override
                    public void onIdentityId(String identityId) {
                        Utils.logE(getClass().getName(), "identity id: " + identityId);
                        // ap-southeast-1:908fba04-ed69-4766-95f2-49b60bb1c329
                        new CredentialProvider().execute(idToken, identityId);
                    }

                    @Override
                    public void handleError(Exception exception) {

                    }
                });
                Utils.logE(getClass().getName(), "user jwt token: " + userSession.getIdToken().getJWTToken());
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
        });
    }
    public class CredentialProvider extends AsyncTask<String, Void, Void> {

        private Exception exception;

        protected Void doInBackground(String... args) {
            try {
                credentialsProvider = new CognitoCachingCredentialsProvider(mContext, MqttPublishManager.COGNITO_POOL_ID, MqttPublishManager.MY_REGION);

                String identityId = args[1];
                String policyName = "policy1";

                AttachPrincipalPolicyRequest policyRequest = new AttachPrincipalPolicyRequest();//.withPrincipal(identityId).withPolicyName(policyName);
                policyRequest.setPrincipal(identityId);
                policyRequest.setPolicyName(policyName);
                AWSIotClient mIotAndroidClient = new AWSIotClient(credentialsProvider);
                mIotAndroidClient.setRegion(Region.getRegion(Regions.AP_SOUTHEAST_1));
                mIotAndroidClient.attachPrincipalPolicy(policyRequest);

                AWSSessionCredentials sessionCredentials = credentialsProvider.getCredentials();
                Map<String, String> logins = new HashMap<>();
                Utils.logE(getClass().getName(), "args[0]: " + args[0]);
                logins.put("cognito-idp.ap-southeast-1.amazonaws.com/ap-southeast-1_EnF2okBJS", args[0]);

                credentialsProvider.setLogins(logins);
                credentialsProvider.refresh();
                Utils.logE(getClass().getName(), "doInBackground! " + credentialsProvider.getCredentials().getAWSAccessKeyId());

                if(credentialsProvider != null) {
                    credentialsProvider.setLogins(credentialsProvider.getLogins());
                } else {
                    Utils.logE(getClass().getName(), " >> credentialProvider is null");
                }

            } catch (Exception e) {
                this.exception = e;
            } finally {
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // TODO - anything to do
            connectToAWS();
        }
    }
}
