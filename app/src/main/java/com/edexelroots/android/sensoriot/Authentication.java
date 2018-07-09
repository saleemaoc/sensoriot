package com.edexelroots.android.sensoriot;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.auth.core.IdentityHandler;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.ui.AuthUIConfiguration;
import com.amazonaws.mobile.auth.ui.SignInUI;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.AWSStartupHandler;
import com.amazonaws.mobile.client.AWSStartupResult;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;

import java.util.HashMap;
import java.util.Map;

public class Authentication extends AppCompatActivity {

    public static CognitoCachingCredentialsProvider credentialsProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        AWSMobileClient.getInstance().initialize(this, new AWSStartupHandler() {
            @Override
            public void onComplete(AWSStartupResult awsStartupResult) {

                final IdentityManager identityManager = IdentityManager.getDefaultIdentityManager();
                if(!identityManager.isUserSignedIn()) {
                    AuthUIConfiguration config =
                            new AuthUIConfiguration.Builder()
                                    .userPools(true)  // true? show the Email and Password UI
//                                .signInButton(FacebookButton.class) // Show Facebook button
//                                .signInButton(GoogleButton.class) // Show Google button
//                                .logoResId(R.drawable.mylogo) // Change the logo
                                    .backgroundColor(Color.BLUE) // Change the backgroundColor
                                    .isBackgroundColorFullScreen(true) // Full screen backgroundColor the backgroundColor full screenff
                                    .fontFamily("sans-serif-light") // Apply sans-serif-light as the global font
                                    .canCancel(true)
                                    .build();

                    SignInUI signInUI = (SignInUI) AWSMobileClient.getInstance().getClient(Authentication.this, SignInUI.class);
                    signInUI.login(Authentication.this, MainActivity.class).authUIConfiguration(config).execute();

                } else {
                    final CognitoUserPool userPool = new CognitoUserPool(Authentication.this, identityManager.getConfiguration());
                    userPool.getCurrentUser().getSessionInBackground(new AuthenticationHandler() {
                        @Override
                        public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {
                            String idToken = userSession.getIdToken().getJWTToken();
                            Utils.logE(getClass().getName(), "user session token: " + idToken);
                            new CredentialProvider().execute(idToken);
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
            }
        }).execute();

    }

    class CredentialProvider extends AsyncTask<String, Void, Void> {

        private Exception exception;

        protected Void doInBackground(String... args) {
            try {
                credentialsProvider = new CognitoCachingCredentialsProvider(Authentication.this, MqttPublishManager.COGNITO_POOL_ID, MqttPublishManager.MY_REGION);
                AWSSessionCredentials sessionCredentials = credentialsProvider.getCredentials();
                Map<String, String> logins = new HashMap<>();
                Utils.logE(getClass().getName(), "args[0]: " + args[0]);
                logins.put("cognito-idp.ap-southeast-1.amazonaws.com/ap-southeast-1_EnF2okBJS", args[0]);

                credentialsProvider.setLogins(logins);
                credentialsProvider.refresh();
                Utils.logE(getClass().getName(), "doInBackground! " + credentialsProvider.getCredentials().getAWSAccessKeyId());

            } catch (Exception e) {
                this.exception = e;
            } finally {
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            startActivity(new Intent(Authentication.this, MainActivity.class));
            finish();
        }
    }
}

