package com.edexelroots.android.sensoriot.iot;


import android.content.Context;
import android.os.AsyncTask;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;

import java.util.HashMap;
import java.util.Map;

public class CredentialProviderIoT extends AsyncTask<String, Void, Void> {

    private Exception exception;
    CognitoCachingCredentialsProvider credentialsProvider = null;
    MqttPublishManager mqttPublishManager = null;
    Context mContext = null;

    public CredentialProviderIoT(MqttPublishManager mpm, Context c) {
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
//            mIotAndroidClient.setRegion(Region.getRegion(Regions.AP_SOUTHEAST_1));
            mIotAndroidClient.setRegion(Region.getRegion(MqttPublishManager.MY_REGION));
            mIotAndroidClient.attachPrincipalPolicy(policyRequest);

            Map<String, String> logins = new HashMap<>();
//            Utils.logE(getClass().getName(), "jwt token; " + args[0]);
//            logins.put("cognito-idp.ap-southeast-1.amazonaws.com/" + MqttPublishManager.USER_POOL_ID, args[0]);
            logins.put("cognito-idp.ap-northeast-1.amazonaws.com/" + MqttPublishManager.USER_POOL_ID, args[0]);

            credentialsProvider.setLogins(logins);
            credentialsProvider.refresh();

            mqttPublishManager.setCredentialsProvider(credentialsProvider);

        } catch (Exception e) {
            this.exception = e;
            e.printStackTrace();
        } finally {
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        mqttPublishManager.connectToAWS(credentialsProvider);
    }
}

