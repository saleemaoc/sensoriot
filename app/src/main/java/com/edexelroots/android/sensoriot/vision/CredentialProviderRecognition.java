package com.edexelroots.android.sensoriot.vision;


import android.content.Context;
import android.os.AsyncTask;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.edexelroots.android.sensoriot.CredentialsReciever;
import com.edexelroots.android.sensoriot.iot.MqttPublishManager;
import com.edexelroots.android.sensoriot.kinesis.KinesisActivity;

import java.util.HashMap;
import java.util.Map;

public class CredentialProviderRecognition extends AsyncTask<String, Void, Void> {

    private Exception exception;
    CognitoCachingCredentialsProvider credentialsProvider = null;
    CredentialsReciever mReciever = null;
    Context mContext = null;

    public CredentialProviderRecognition(Context c, CredentialsReciever cr) {
        this.mContext = c;
        this.mReciever = cr;
    }

    protected Void doInBackground(String... args) {

        try {
            credentialsProvider = new CognitoCachingCredentialsProvider(mContext, MqttPublishManager.COGNITO_POOL_ID, FaceTrackerActivity.REGION);


            String identityId = args[1];
            AttachPrincipalPolicyRequest policyRequest = new AttachPrincipalPolicyRequest();//.withPrincipal(identityId).withPolicyName(policyName);
            policyRequest.setPrincipal(identityId);
            policyRequest.setPolicyName(MqttPublishManager.policyName);
            AWSIotClient mIotAndroidClient = new AWSIotClient(credentialsProvider);
            mIotAndroidClient.setRegion(Region.getRegion(MqttPublishManager.MY_REGION));
            mIotAndroidClient.attachPrincipalPolicy(policyRequest);


            Map<String, String> logins = new HashMap<>();
//            Utils.logE(getClass().getName(), "jwt token; " + args[0]);
            logins.put("cognito-idp.ap-southeast-1.amazonaws.com/" + MqttPublishManager.USER_POOL_ID, args[0]);
//            logins.put("cognito-idp.ap-northeast-1.amazonaws.com/" + MqttPublishManager.USER_POOL_ID, args[0]);

            credentialsProvider.setLogins(logins);
            credentialsProvider.refresh();
        } catch (Exception e) {
            this.exception = e;
            e.printStackTrace();
        } finally {
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        mReciever.onCredentialsRecieved(credentialsProvider);
    }
}

