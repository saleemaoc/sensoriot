package com.edexelroots.android.sensoriot;

import android.app.Application;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.text.emoji.EmojiCompat;
import android.support.text.emoji.FontRequestEmojiCompatConfig;
import android.support.text.emoji.bundled.BundledEmojiCompatConfig;
import android.support.v4.provider.FontRequest;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.userpools.CognitoUserPoolsSignInProvider;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.regions.Regions;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SensorIoTApp extends Application {
    public static Regions KINESIS_VIDEO_REGION = Regions.AP_SOUTHEAST_1;
//    public static Regions KINESIS_VIDEO_REGION = Regions.AP_NORTHEAST_1;

    public static boolean isEmojiCompatInit = false;

    @Override
    public void onCreate() {
        super.onCreate();

/*        QueryBuilder queryBuilder = new QueryBuilder("Noto Sans")
                .withWidth(1)
                .withWeight(1)
                .withItalic(1)
                .withBestEffort(true);
        String query = queryBuilder.build();


        FontRequest fontRequest = new FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                query,
                R.array.com_google_android_gms_fonts_certs);
        EmojiCompat.Config config = new FontRequestEmojiCompatConfig(this, fontRequest);
*/

        EmojiCompat.Config config = new BundledEmojiCompatConfig(this);
        config.setReplaceAll(true)
                .setEmojiSpanIndicatorEnabled(true)
                .setEmojiSpanIndicatorColor(Color.GREEN);
        config.registerInitCallback(new EmojiCompat.InitCallback() {
            @Override
            public void onFailed(@Nullable Throwable throwable) {
                super.onFailed(throwable);
                throwable.printStackTrace();
            }

            @Override
            public void onInitialized() {
                super.onInitialized();
                isEmojiCompatInit = true;
                Utils.logE(getClass().getName(), "EmojiCompat initialized");
            }

        });
        EmojiCompat.init(config);

        // this sets the logging level
        // to actually enable logging you also need to run
        //    adb shell setprop log.tag.com.amazonaws.request DEBUG
        // see https://github.com/aws/aws-sdk-android/blob/master/Logging.html for image
        Logger.getLogger("org.apache.http").setLevel(Level.FINEST);
        Logger.getLogger("com.amazonaws").setLevel(Level.FINEST);

        if (IdentityManager.getDefaultIdentityManager() == null) {
            final IdentityManager identityManager = new IdentityManager(
                    getApplicationContext(),
                    new AWSConfiguration(this)
            );
            IdentityManager.setDefaultIdentityManager(identityManager);

            IdentityManager.getDefaultIdentityManager().addSignInProvider(CognitoUserPoolsSignInProvider.class);
        }

    }

    public static AWSCredentialsProvider getCredentialsProvider() {
        return IdentityManager.getDefaultIdentityManager().getCredentialsProvider();
    }

}
