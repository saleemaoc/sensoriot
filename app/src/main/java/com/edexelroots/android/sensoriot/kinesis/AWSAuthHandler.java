package com.edexelroots.android.sensoriot.kinesis;


import android.content.Context;

import com.amazonaws.mobile.auth.core.IdentityHandler;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.edexelroots.android.sensoriot.CredentialsReciever;
import com.edexelroots.android.sensoriot.Utils;
import com.edexelroots.android.sensoriot.vision.CredentialProviderRecognition;

public class AWSAuthHandler implements AuthenticationHandler {

    IdentityManager identityManager = null;
    CredentialsReciever mReciever = null;
    Context mContext = null;

    public AWSAuthHandler(Context c, CredentialsReciever cr, IdentityManager im) {
        this.identityManager = im;
        this.mReciever = cr;
        this.mContext = c;
    }

    @Override
    public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {
        final String idToken = userSession.getIdToken().getJWTToken();
        identityManager.getUserID(new IdentityHandler() {
            @Override
            public void onIdentityId(String identityId) {
                new CredentialProviderRecognition(mContext, mReciever).execute(idToken, identityId);
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
