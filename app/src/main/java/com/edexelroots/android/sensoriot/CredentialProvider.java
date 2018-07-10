package com.edexelroots.android.sensoriot;


import android.content.Context;
import android.os.AsyncTask;

import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;

import java.util.HashMap;
import java.util.Map;

public class CredentialProvider extends AsyncTask<String, Void, Void> {

    private Exception exception;
    CognitoCachingCredentialsProvider credentialsProvider = null;
    MqttPublishManager mqttPublishManager = null;
    Context mContext = null;

    public CredentialProvider(MqttPublishManager mpm, Context c) {
        this.mContext = c;
        this.mqttPublishManager = mpm;
    }

    protected Void doInBackground(String... args) {

        try {
            credentialsProvider = new CognitoCachingCredentialsProvider(mContext, MqttPublishManager.COGNITO_POOL_ID, MqttPublishManager.MY_REGION);

            String identityId = args[1];
            AttachPrincipalPolicyRequest policyRequest = new AttachPrincipalPolicyRequest();//.withPrincipal(identityId).withPolicyName(policyName);
            policyRequest.setPrincipal(identityId);
            policyRequest.setPolicyName(MqttPublishManager.policyName);
            AWSIotClient mIotAndroidClient = new AWSIotClient(credentialsProvider);
            mIotAndroidClient.setRegion(Region.getRegion(Regions.AP_SOUTHEAST_1));
            mIotAndroidClient.attachPrincipalPolicy(policyRequest);

            // AWSSessionCredentials sessionCredentials = credentialsProvider.getCredentials();
            Map<String, String> logins = new HashMap<>();
            logins.put("cognito-idp.ap-southeast-1.amazonaws.com/" + MqttPublishManager.userPoolId, args[0]);

            credentialsProvider.setLogins(logins);
            credentialsProvider.refresh();

            mqttPublishManager.setCredentialsProvider(credentialsProvider);

        } catch (Exception e) {
            this.exception = e;
        } finally {
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        mqttPublishManager.connectToAWS();
    }
}