package com.edexelroots.android.sensoriot.kinesis;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Size;
import android.widget.Toast;

import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.ui.AuthUIConfiguration;
import com.amazonaws.mobile.auth.ui.SignInUI;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.AWSStartupHandler;
import com.amazonaws.mobile.client.AWSStartupResult;
import com.edexelroots.android.sensoriot.R;
import com.edexelroots.android.sensoriot.Utils;
import com.edexelroots.android.sensoriot.iot.AWSIoTConnectionStatus;
import com.edexelroots.android.sensoriot.iot.MqttPublishManager;
import com.edexelroots.android.sensoriot.kinesis.fragments.Camera2BasicFragment;
import com.edexelroots.android.sensoriot.kinesis.fragments.StreamConfigurationFragment;
import com.edexelroots.android.sensoriot.kinesis.fragments.StreamingFragment;
import com.edexelroots.android.sensoriot.vision.FaceTrackerActivity;

import org.apache.http.client.CredentialsProvider;

public class KinesisActivity extends AppCompatActivity {

    public static CredentialsProvider mCredentialProvider = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kinesis);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(null == savedInstanceState) {
            signInAWSCognito();
            startConfigFragment();
        }
    }

    MqttPublishManager mPublishManager = null;
    AWSIoTConnectionStatus mConnectionStatus = null;

    protected void signInAWSCognito() {
        AWSMobileClient.getInstance().initialize(this, new AWSStartupHandler() {
            @Override
            public void onComplete(AWSStartupResult awsStartupResult) {

                final IdentityManager identityManager = IdentityManager.getDefaultIdentityManager();
                if(!identityManager.isUserSignedIn()) {
                    AuthUIConfiguration config =
                            new AuthUIConfiguration.Builder()
                                    .userPools(true)  // true? show the Email and Password UI
                                    .backgroundColor(Color.BLUE) // Change the backgroundColor
                                    .isBackgroundColorFullScreen(true) // Full screen backgroundColor the backgroundColor full screenff
                                    .fontFamily("sans-serif-light") // Apply sans-serif-light as the global font
                                    .canCancel(true)
                                    .build();

                    SignInUI signInUI = (SignInUI) AWSMobileClient.getInstance().getClient(KinesisActivity.this, SignInUI.class);
                    signInUI.login(KinesisActivity.this, KinesisActivity.class).authUIConfiguration(config).execute();
                }
                mConnectionStatus = new AWSIoTConnectionStatus(KinesisActivity.this, findViewById(R.id.tv_connection_status));
                mPublishManager =  new MqttPublishManager(KinesisActivity.this, mConnectionStatus);
                mPublishManager.setupSession();
            }
        }).execute();
    }
    public void startFragment(Fragment fragment, String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if(null == tag) {
            fragmentManager.beginTransaction().replace(R.id.content_simple, fragment).commit();
        } else {
            fragmentManager.beginTransaction().replace(R.id.content_simple, fragment, tag).commit();
        }
    }

    private final String STREAMING_FRAGMENT_TAG = "preview_fragment";

    public void indicateFace(String label, float rTop, float rLeft, float rHeight, float rWidth) {
        try {
            Fragment f = getSupportFragmentManager().findFragmentByTag(STREAMING_FRAGMENT_TAG);
            Toast.makeText(this, label, Toast.LENGTH_SHORT).show();

            assert f != null && f instanceof Camera2BasicFragment;
            Size previewSize = ((Camera2BasicFragment) f).getPreviewSize();
            Utils.logE(getClass().getName(), "Preview: " + previewSize.getWidth() + ", " + previewSize.getHeight());
            int w = previewSize.getHeight();
            int h = previewSize.getWidth();
            float top = h * rTop;
            float height = h * rHeight;
            float left = w * rLeft;
            float width = w * rWidth;

            ((Camera2BasicFragment) f).drawRectangle(label, left, top, width, height);
        }catch (NullPointerException npe){
            npe.printStackTrace();
        }
    }

    public void startCamera2Fragment(Bundle extras) {
        try {
            Camera2BasicFragment f = Camera2BasicFragment.newInstance(this);
            f.setArguments(extras);
            startFragment(f, STREAMING_FRAGMENT_TAG);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Could not start streaming", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void startFaceDetectionActivity(Bundle extras) {
        Intent i = new Intent(this, FaceTrackerActivity.class);
        i.putExtra("config", extras);
        startActivity(i);
    }

    public  void startStreamingFragment(Bundle extras) {
        try {
            StreamingFragment f = StreamingFragment.newInstance(this);
            f.setArguments(extras);
            startFragment(f, null);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Could not start streaming", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void startConfigFragment() {
        Utils.logE(getClass().getName(), "Start Config Fragment");
        try {
            StreamConfigurationFragment f = StreamConfigurationFragment.newInstance(this);
            startFragment(f, null);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Could not config", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
