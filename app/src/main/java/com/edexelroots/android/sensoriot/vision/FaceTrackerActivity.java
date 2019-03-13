/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.edexelroots.android.sensoriot.vision;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationChannel;
//import android.app.NotificationManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.ui.AuthUIConfiguration;
import com.amazonaws.mobile.auth.ui.SignInUI;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.android.AndroidCameraMediaSourceConfiguration;
import com.amazonaws.regions.Regions;
import com.edexelroots.android.sensoriot.CredentialsReciever;
import com.edexelroots.android.sensoriot.R;
import com.edexelroots.android.sensoriot.StreamManager;
import com.edexelroots.android.sensoriot.Utils;
import com.edexelroots.android.sensoriot.kinesis.AWSAuthHandler;
import com.edexelroots.android.sensoriot.kinesis.fragments.StreamingFragment;
import com.edexelroots.android.sensoriot.vision.api.FaceApiService;
import com.edexelroots.android.sensoriot.vision.api.FaceResponse;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.edexelroots.android.sensoriot.vision.camera.CameraSourcePreview;
import com.edexelroots.android.sensoriot.vision.camera.GraphicOverlay;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * Activity for the face tracker app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 */
public final class FaceTrackerActivity extends AppCompatActivity implements
        FaceMatchFragment.OnFaceFragmentListener,
        CredentialsReciever {
    private static final String TAG = "FaceTracker";

    private CameraSource mCameraSource = null;

    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    MyFaceDetector mFaceDetector;
    AndroidCameraMediaSourceConfiguration acmsc;

    FaceMatchFragment mFaceMatchFragment = null;

//    public static Regions REGION = Regions.AP_NORTHEAST_1;
    public static Regions REGION = Regions.AP_SOUTHEAST_1;

    boolean previewShown = false;
    private final String KEY_PREVIEW_SHOWN = "preview_shown";

    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);

        mPreview = findViewById(R.id.preview);
        mGraphicOverlay = findViewById(R.id.faceOverlay);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            showHidePreview(true);
//                startCameraSource();
//                fab.hide();
        });

        Bundle b = getIntent().getBundleExtra("config");
        if (b != null) {
            acmsc = b.getParcelable(StreamingFragment.KEY_MEDIA_SOURCE_CONFIGURATION);
        }

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }

        mFaceMatchFragment = (FaceMatchFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_facematch);

/*
        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            Utils.logE(getClass().getName(), bundle.getString("test"));
        }
*/

        if (null == icicle) {
            Utils.logE(getClass().getName(), "null == icicle");
            signInAWSCognito();
            fab.setEnabled(false);
            fab.hide();
            showHidePreview(false);
            // clear faces list
            mFaceMatchFragment.clear();
        } else {
            Utils.logE(getClass().getName(), "null != icicle");
            showHidePreview(icicle.getBoolean(KEY_PREVIEW_SHOWN) && Utils.isConnected(this));
            fab.setEnabled(true);
            hideProgress();
        }
    }


    boolean isAWSInitialized = false;
    protected void signInAWSCognito() {
        isAWSInitialized = false;
//        Utils.logE(getClass().getName(), "signInAWSCognito");
        AWSMobileClient.getInstance().initialize(this, awsStartupResult -> {

            final IdentityManager identityManager = IdentityManager.getDefaultIdentityManager();

            if (!identityManager.isUserSignedIn()) {
                AuthUIConfiguration config =
                        new AuthUIConfiguration.Builder()
                                .userPools(true)  // true? show the Email and Password UI
                                .backgroundColor(Color.BLUE) // Change the backgroundColor
//                                .isBackgroundColorFullScreen(true) // Full screen backgroundColor the backgroundColor full screenff
                                .fontFamily("sans-serif-light") // Apply sans-serif-light as the global font
                                .canCancel(false)
                                .build();

                SignInUI signInUI = (SignInUI) AWSMobileClient.getInstance().getClient(FaceTrackerActivity.this, SignInUI.class);
                signInUI.login(FaceTrackerActivity.this, FaceTrackerActivity.class).authUIConfiguration(config).execute();
            }
            setupSession();
        }).execute();
    }

    public void setupSession() {
//        Utils.logE(getClass().getName(), "setup session");
        if (!Utils.isConnected(this)) {
            // we don't have connectivity
            Snackbar.make(mFaceMatchFragment.getView(), "No internet connectivity!", Snackbar.LENGTH_INDEFINITE).show();
            hideProgress();
            return;
        }
        final IdentityManager identityManager = IdentityManager.getDefaultIdentityManager();
        CognitoCachingCredentialsProvider cccp = identityManager.getUnderlyingProvider();
//        if(cccp == null) {
            Snackbar.make(findViewById(R.id.fab), "Please wait...", Snackbar.LENGTH_SHORT).show();
            final CognitoUserPool userPool = new CognitoUserPool(this, identityManager.getConfiguration());
            userPool.getCurrentUser().getSessionInBackground(new AWSAuthHandler(this, this, identityManager));
/*        } else {
            Utils.logE(getClass().getName(), "we already have cccp");
            onCredentialsRecieved(cccp);
        }*/
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = view -> ActivityCompat.requestPermissions(thisActivity, permissions, RC_HANDLE_CAMERA_PERM);

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    GraphicFaceTrackerFactory mFaceTracker = new GraphicFaceTrackerFactory();

    private void createCameraSource() {

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setTrackingEnabled(true)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        mFaceDetector = new MyFaceDetector(detector, this);
        mFaceDetector.setProcessor(new MultiProcessor.Builder<>(mFaceTracker)
                .build());

        if (!mFaceDetector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
            return;
        }
        int cameraFacing = CameraSource.CAMERA_FACING_BACK;
        int hr = 720, vr = 720;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float ratio = dm.heightPixels / (float) dm.widthPixels;
        if (!Utils.isPortraitMode(this)) {
            ratio = dm.widthPixels / (float) dm.heightPixels;
        }
        vr = (int) (hr * ratio);
        float half = dm.heightPixels / 2f;
        if (vr > half) {
            float prevVr = vr;
            vr = (int) half;
            float div = prevVr / (float) vr;
            hr = (int) (hr / div);
        }
/*
        Utils.logE(getClass().getName(), dm.widthPixels + " : " + dm.heightPixels + "; Ratio: " + ratio);
        Utils.logE(getClass().getName(), hr + " : " + vr);

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mPreview.getLayoutParams();
        lp.leftMargin = lp.rightMargin = (lp.leftMargin + lp.rightMargin) / 2;
        mPreview.setLayoutParams(lp);
*/
        if (acmsc != null) {
            cameraFacing = acmsc.getCameraFacing();
            hr = acmsc.getHorizontalResolution();
            vr = acmsc.getHorizontalResolution();
        }
        mCameraSource = new CameraSource.Builder(context, mFaceDetector)
                .setRequestedPreviewSize(hr, vr)
                .setFacing(cameraFacing)
                .setRequestedFps(30)
                .setAutoFocusEnabled(true)
                .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if(isAWSInitialized) {
            showHidePreview(true);
        }
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
        showHidePreview(false);
    }


    private Animator.AnimatorListener mAnimatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {

        }

        @Override
        public void onAnimationEnd(Animator animator) {
            startCameraSource();
        }

        @Override
        public void onAnimationCancel(Animator animator) {

        }

        @Override
        public void onAnimationRepeat(Animator animator) {

        }
    };

    @Override
    public void onSaveInstanceState(Bundle state, PersistableBundle outPersistentState) {
        state.putBoolean(KEY_PREVIEW_SHOWN, previewShown);
        super.onSaveInstanceState(state, outPersistentState);
    }

    private void animate(View view, float weight, boolean previewShown) {
        int ms = 0;
        Utils.ViewWeightAnimationWrapper animationWrapper = new Utils.ViewWeightAnimationWrapper(view);
        ObjectAnimator anim = ObjectAnimator.ofFloat(animationWrapper,
                "weight",
                animationWrapper.getWeight(),
                weight);
        anim.setInterpolator(new DecelerateInterpolator());
        if (previewShown) {
            ms = 100;
            anim.addListener(mAnimatorListener);
        }
        anim.setDuration(ms);
        anim.start();
    }

    public void showHidePreview(boolean show) {
        previewShown = show;
        if (show) {
            float ratio = getResources().getDisplayMetrics().heightPixels / (float) getResources().getDisplayMetrics().widthPixels;
            if (Utils.isPortraitMode(this)) {
                ratio = getResources().getDisplayMetrics().widthPixels / (float) getResources().getDisplayMetrics().heightPixels;
            }
            animate(mPreview, ratio, show);
            animate(mFaceMatchFragment.getView(), 1 - ratio, show);
        } else if(mFaceMatchFragment != null) {
            animate(mPreview, 0f, show);
            animate(mFaceMatchFragment.getView(), 1f, show);
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }

        if (mFaceDetector != null) {
            mFaceDetector.release();
        }

    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = (dialog, id) -> finish();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Face Tracker")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private int currentFaceId = -1;

    public void faceToImageViewLandscape(byte[] bytes, int faceId) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);

        byte[] byteArray = byteArrayOutputStream.toByteArray();
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
/*
        FaceDetector detector = new FaceDetector.Builder(this)
                .setTrackingEnabled(true)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();
        Frame frame = new Frame.Builder()
                .setBitmap(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length))
                .build();
        SparseArray faces = detector.detect(frame);
*/
//        if (faces.size() > 0) {
        addFaceToList(faceId, buffer, bitmap);
//        }
    }

    public void faceToImageViewPortrait(Bitmap bitmap, int faceId) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        bitmap = rotateBitmap(bitmap, 90);

        byte[] byteArray = byteArrayOutputStream.toByteArray();
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
/*
        FaceDetector detector = new FaceDetector.Builder(this)
                .setTrackingEnabled(true)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();
        Frame frame = new Frame.Builder()
                .setBitmap(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length))
                .build();
        SparseArray faces = detector.detect(frame);
*/
//        if(faces.size() > 0) {
        addFaceToList(faceId, buffer, bitmap);
//        }
    }

    private void addFaceToList(int faceId, ByteBuffer buffer, Bitmap bitmap) {
        if (faceId != currentFaceId) {
            currentFaceId = faceId;
            if (mFaceMatchFragment != null) {
                FaceMatchItem fmi = new FaceMatchItem("", 0, "", bitmap);
                new Thread(() -> {
                    boolean faceMatched = new StreamManager(this).startFaceSearchRequest(buffer, fmi);
                    if (!faceMatched) {
                        // we couldn't recognize this face
                        currentFaceId = -1;
                    } else {
                        runOnUiThread(() -> {
                            boolean isNewFace = mFaceMatchFragment.addNewFace(fmi);
                            if(isNewFace) {
                                getFaceDetails(fmi);
                            }
                        });
                    }
                }).start();
            }
        }
    }

    FaceApiService mFaceApi = new FaceApiService();

    private void getFaceDetails(FaceMatchItem fmi) {
        mFaceApi.getFace(fmi.awsFaceId, new Callback<FaceResponse>() {
            @Override
            public void onResponse(Call<FaceResponse> call, Response<FaceResponse> response) {
                if (response.isSuccessful()) {
                    FaceResponse faceResponse = response.body();
                    if(faceResponse.errorMessage.equalsIgnoreCase("Unknown FaceID")) {
                        Utils.logE(getClass().getName(), "Unknow FaceID");
                        runOnUiThread(() -> mFaceMatchFragment.removeFace(fmi));
                        return;
                    }
                    // Utils.logE(getClass().getName(), faceResponse.name + ", " + faceResponse.title + ", " + fmi.awsFaceId);
                    fmi.name = faceResponse.name;
                    fmi.subtitle = faceResponse.title;
                    fmi.url = faceResponse.url;

                    runOnUiThread(() -> {
                        mFaceMatchFragment.notifyDataSetChanged();
                        showNotification(fmi.name, fmi.url);
                        vibrate(500);
                    });
                }
            }

            @Override
            public void onFailure(Call<FaceResponse> call, Throwable t) {

            }
        });
    }

    @Override
    public void OnFaceItemClicked(FaceMatchItem item) {
        if (TextUtils.isEmpty(item.url)) {
            Toast.makeText(this, item.name, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent browse = new Intent(Intent.ACTION_VIEW, Uri.parse(item.url));
        startActivity(browse);
    }

    @Override
    public void onCredentialsRecieved(CognitoCachingCredentialsProvider credentialsProvider) {
        isAWSInitialized = true;
        hideProgress();
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setEnabled(true);
        fab.show();
    }

    private void hideProgress() {
        findViewById(R.id.progress_bar).setVisibility(View.GONE);
    }


    public void showNotification(String name, String url) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = 1;
        String channelId = "channel-01";
        String channelName = "Rekognition Channel";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            @SuppressLint("WrongConstant") NotificationChannel mChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(name)
                .setContentText(url);

//        Intent notificationIntent = new Intent(this, FaceTrackerActivity.class);
        Intent notificationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
/*
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT|
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
*/
//        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

//        notificationIntent.putExtra("test", "Test");

        stackBuilder.addNextIntent(notificationIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_ONE_SHOT);
        mBuilder.setContentIntent(resultPendingIntent);

        notificationManager.notify(notificationId, mBuilder.build());
    }

    private void vibrate(int millis) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            v.vibrate(millis);
        }
    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {

        GraphicFaceTracker mTracker = null;

        @Override
        public Tracker<Face> create(Face face) {
            return mTracker = new GraphicFaceTracker(mGraphicOverlay);
        }

        public void done() {
            if (mTracker != null) {
                mTracker.onDone();
            }
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }

    }
}
