package com.edexelroots.android.sensoriot.kinesis.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Trace;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.amazonaws.kinesisvideo.client.KinesisVideoClient;
import com.amazonaws.kinesisvideo.common.exception.KinesisVideoException;
import com.amazonaws.mobileconnectors.kinesisvideo.client.KinesisVideoAndroidClientFactory;
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.android.AndroidCameraMediaSource;
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.android.AndroidCameraMediaSourceConfiguration;
import com.edexelroots.android.sensoriot.Constants;
import com.edexelroots.android.sensoriot.R;
import com.edexelroots.android.sensoriot.SensorIoTApp;
import com.edexelroots.android.sensoriot.StreamManager;
import com.edexelroots.android.sensoriot.Utils;
import com.edexelroots.android.sensoriot.kinesis.CustomView;
import com.edexelroots.android.sensoriot.kinesis.KDSConsumer;
import com.edexelroots.android.sensoriot.kinesis.KinesisActivity;

import java.util.ArrayList;
import java.util.List;


public class StreamingFragment extends Fragment implements TextureView.SurfaceTextureListener, SurfaceHolder.Callback {
    public static final String KEY_MEDIA_SOURCE_CONFIGURATION = "mediaSourceConfiguration";
    public static final String KEY_STREAM_NAME = "facekinesis";
//    public static final String KEY_STREAM_NAME = "liverekprototype"; // tom

    private static final String TAG = StreamingFragment.class.getSimpleName();

    private Button mStartStreamingButton;
    private KinesisVideoClient mKinesisVideoClient;
    private String mStreamName;
    private AndroidCameraMediaSourceConfiguration mConfiguration;
    private AndroidCameraMediaSource mCameraMediaSource;

    private KinesisActivity mActivity;

    public static StreamingFragment newInstance(KinesisActivity ma) {
        StreamingFragment s = new StreamingFragment();
        s.mActivity = ma;
        return s;
    }

    SurfaceView mSurfaceView;
    TextureView mTextureView;
    Surface surface;
    SurfaceHolder mSurfaceHolder;
    CameraWorker cw;

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        getArguments().setClassLoader(AndroidCameraMediaSourceConfiguration.class.getClassLoader());
        mStreamName = getArguments().getString(KEY_STREAM_NAME);
        mConfiguration = getArguments().getParcelable(KEY_MEDIA_SOURCE_CONFIGURATION);

        final View view = inflater.inflate(R.layout.fragment_streaming, container, false);
        mSurfaceView = new CustomView(getContext());
        mSurfaceView.setVisibility(View.INVISIBLE);
        ((LinearLayout) view.findViewById(R.id.texture_container)).addView(mSurfaceView);
/*
        int orientation = getResources().getConfiguration().orientation;
        if(orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(getContext(), "Landscape",Toast.LENGTH_SHORT ).show();
        } else {
//            view.findViewById(R.id.texture_container).setRotation(90);
//            mSurfaceView.setRotation(90);
            Toast.makeText(getContext(), "Portrait",Toast.LENGTH_SHORT ).show();
        }
*/
        mTextureView = view.findViewById(R.id.texture);
        mTextureView.setSurfaceTextureListener(this);
        mSurfaceHolder = mSurfaceView.getHolder();
        cw = new CameraWorker();
//        mSurfaceHolder.addCallback(this);
//        mSurfaceHolder.addCallback(cw);

        return view;
    }

    private void adjustAspectRatio(int videoWidth, int videoHeight) {
/*
        int viewWidth = mSurfaceView.getWidth();
        int viewHeight = mSurfaceView.getHeight();
        double aspectRatio = (double) videoHeight / videoWidth;

        int newWidth, newHeight;
        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            // limited by narrow width; restrict height
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            // limited by short height; restrict width
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }
        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;
        Log.v(TAG, "video=" + videoWidth + "x" + videoHeight +
                " view=" + viewWidth + "x" + viewHeight +
                " newView=" + newWidth + "x" + newHeight +
                " off=" + xoff + "," + yoff);

        android.graphics.Matrix txform = new android.graphics.Matrix();
        mSurfaceView.getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        txform.postRotate(45);          // just for fun
        txform.postTranslate(xoff, yoff);
        mSurfaceView.setTransform(txform);
*/
    }

    private void createClientAndStartStreaming(final Surface surface) {

        try {
            mKinesisVideoClient = KinesisVideoAndroidClientFactory.createKinesisVideoClient(
                    getActivity(),
                    SensorIoTApp.KINESIS_VIDEO_REGION,
                    SensorIoTApp.getCredentialsProvider());

            mCameraMediaSource = (AndroidCameraMediaSource) mKinesisVideoClient
                    .createMediaSource(mStreamName, mConfiguration);

//             adjustAspectRatio(200,200);

/*
            float[] mtx = new float[16];
            previewTexture.getTransformMatrix(mtx);

            Matrix.rotateM(mtx, 0, 90, 0, 0, 1);
            Matrix.translateM(mtx, 0, 0, -1, 0);
*/

            mCameraMediaSource.setPreviewSurfaces(surface);


            if (sm == null) {
                sm = new StreamManager(getActivity(),
                        Constants.Rekognition.streamProcessorName,
                        Constants.Rekognition.kinesisVideoStreamArn,
                        Constants.Rekognition.kinesisDataStreamArn,
                        Constants.Rekognition.roleArn,
                        Constants.Rekognition.collectionId,
                        Constants.Rekognition.matchThreshold);
            }

            resumeStreaming();

        } catch (final KinesisVideoException e) {
            Log.e(TAG, "unable to start streaming\n" + e.getMessage());
            // throw new RuntimeException("unable to start streaming", e)
            Toast.makeText(getContext(), "Unable to start streaming!!\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mStartStreamingButton = (Button) view.findViewById(R.id.start_streaming);
        mStartStreamingButton.setOnClickListener(stopStreamingWhenClicked());
         // cw.open();
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeStreaming();
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseStreaming();
    }

    private View.OnClickListener stopStreamingWhenClicked() {
        return view -> {
            pauseStreaming();
            mActivity.startConfigFragment();
        };
    }

    private void resumeStreaming() {
        try {
            if (mCameraMediaSource == null) {
                return;
            }
            mCameraMediaSource.start();
            Toast.makeText(getActivity(), "resumed streaming", Toast.LENGTH_SHORT).show();
            mStartStreamingButton.setText(getActivity().getText(R.string.stop_streaming));
//            startRekStreamProcessor();

        } catch (final KinesisVideoException e) {
            Log.e(TAG, "unable to resume streaming", e);
            Toast.makeText(getActivity(), "failed to resume streaming", Toast.LENGTH_SHORT).show();
        }
    }

    private void pauseStreaming() {
        try {
            if (mCameraMediaSource == null) {
                return;
            }
            mCameraMediaSource.stop();
            Toast.makeText(getActivity(), "stopped streaming", Toast.LENGTH_SHORT).show();
            mStartStreamingButton.setText(getActivity().getText(R.string.start_streaming));
        } catch (final KinesisVideoException e) {
            Log.e(TAG, "unable to pause streaming", e);
            Toast.makeText(getActivity(), "failed to pause streaming", Toast.LENGTH_SHORT).show();
        }
    }

    /*    private void startExecutor() {
        Utils.logE(getClass().getName(), "Start executor!!");
        RekognitionInput ri = RekognitionInput.builder()
                .faceCollectionId(Constants.Rekognition.collectionId)
                .iamRoleArn(Constants.Rekognition.roleArn)
                .kinesisVideoStreamArn(Constants.Rekognition.kinesisVideoStreamArn)
                .kinesisDataStreamArn(Constants.Rekognition.kinesisDataStreamArn)
                .matchThreshold(Constants.Rekognition.matchThreshold)
                .streamingProcessorName(Constants.Rekognition.streamProcessorName)
                .build();

        kvrie = KinesisVideoRekognitionIntegrationExample.builder()
                .region(Regions.AP_NORTHEAST_1)
                .credentialsProvider(SensorIoTApp.getCredentialsProvider())
                .kdsStreamName(Constants.Rekognition.kinesisDataStreamArn)
                .kvsStreamName(Constants.Rekognition.kinesisVideoStreamArn)
                .rekognitionInput(ri)
                .build();

        try {
            kvrie.execute(15L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    @Override
    public void onDestroy() {
        try {
            surfaceExists = false;
            KDSConsumer.isExecuting = false;
//            stopRekStreamProcessor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // cw.close();
        super.onDestroy();
    }


    StreamManager sm = null;

    private void startRekStreamProcessor() {
        Utils.logE(getClass().getName(), "Start Rekog processor!!");
        try {
            new Thread(() -> {
                sm.createStreamProcessor();
                sm.listStreamProcessors();
                sm.describeStreamProcessor();
                sm.startStreamProcessor();
            }).start();

            // start pulling data from the stream
            KDSConsumer kdsc = new KDSConsumer(getActivity());
            kdsc.execute();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }


    private void stopRekStreamProcessor() {
        if (sm == null) {
            Utils.logE(TAG, "StreamManager object is null..");
            return;
        }
        new Thread(() -> {
            sm.stopStreamProcessor();
            sm.deleteStreamProcessor();
        }).start();

    }


    ////
    // TextureView.SurfaceTextureListener methods
    ////

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        surfaceTexture.setDefaultBufferSize(1280, 720);
        createClientAndStartStreaming(new Surface(surfaceTexture));
//        new Thread(() -> drawRectangle(surface, 100, 200)).start();

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        try {
            if (mCameraMediaSource != null)
                mCameraMediaSource.stop();
            if (mKinesisVideoClient != null)
                mKinesisVideoClient.stopAllMediaSources();
            KinesisVideoAndroidClientFactory.freeKinesisVideoClient();
        } catch (final KinesisVideoException e) {
            Log.e(TAG, "failed to release kinesis video client", e);
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    private void drawRectangle(int x, int y) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);

        Canvas canvas = mSurfaceHolder.lockCanvas(null);
        try {
            Trace.beginSection("drawRectangle");
            Trace.beginSection("drawColor");
            canvas.drawColor(Color.RED, PorterDuff.Mode.CLEAR);
            Trace.endSection(); // drawColor

            int width = canvas.getWidth();
            int height = canvas.getHeight();
//            int radius;
            if (width < height) {
                // portrait
            } else {
                // landscape
            }

            canvas.drawRect(x, y, x + 200, y + 200, paint);
            Trace.endSection(); // drawRectangle
        } finally {
            mSurfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    boolean surfaceExists = false;

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        surfaceExists = true;
        createClientAndStartStreaming(surfaceHolder.getSurface());
//        new MyThread().execute();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        surfaceExists = false;
        try {
            if (mCameraMediaSource != null)
                mCameraMediaSource.stop();
            if (mKinesisVideoClient != null)
                mKinesisVideoClient.stopAllMediaSources();
            KinesisVideoAndroidClientFactory.freeKinesisVideoClient();
        } catch (final KinesisVideoException e) {
            Log.e(TAG, "failed to release kinesis video client", e);
        }
    }

    class MyThread extends AsyncTask {

        @Override
        protected Object doInBackground(Object... params) {

            while (surfaceExists) {
                Canvas rCanvas = mSurfaceHolder.lockCanvas();

                // reset the canvas to blank at the start
                rCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                // translate to the desired position
                rCanvas.translate(0, 100);

                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.FILL);

                int x = 0, y = 0;
                rCanvas.drawRect(x, y, x + 80, y + 80, paint);


                // draw the rect

                mSurfaceHolder.unlockCanvasAndPost(rCanvas);
            }
            return null;
        }

    }

    public class CameraWorker implements Handler.Callback, SurfaceHolder.Callback {
        static final String TAG = "CamTest";
        static final int MY_PERMISSIONS_REQUEST_CAMERA = 1242;
        private static final int MSG_CAMERA_OPENED = 1;
        private static final int MSG_SURFACE_READY = 2;
        private final Handler mHandler = new Handler(this);
        CameraManager mCameraManager;
        String[] mCameraIDsList;
        CameraDevice.StateCallback mCameraStateCB;
        CameraDevice mCameraDevice;
        CameraCaptureSession mCaptureSession;
        boolean mSurfaceCreated = true;
        boolean mIsCameraConfigured = false;
        private Surface mCameraSurface = null;

        public CameraWorker() {
            this.mCameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
            try {
                mCameraIDsList = this.mCameraManager.getCameraIdList();
                for (String id : mCameraIDsList) {
                    Log.v(TAG, "CameraID: " + id);
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

            mCameraStateCB = new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    Toast.makeText(getActivity().getApplicationContext(), "onOpened", Toast.LENGTH_SHORT).show();

                    mCameraDevice = camera;
                    mHandler.sendEmptyMessage(MSG_CAMERA_OPENED);
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    Toast.makeText(getActivity().getApplicationContext(), "onDisconnected", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    Toast.makeText(getActivity().getApplicationContext(), "onError", Toast.LENGTH_SHORT).show();
                }
            };
        }

        protected void open() {
            //requesting permission
            int permissionCheck = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.CAMERA)) {

                } else {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
                    Toast.makeText(getActivity().getApplicationContext(), "request permission", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getActivity().getApplicationContext(), "PERMISSION_ALREADY_GRANTED", Toast.LENGTH_SHORT).show();
                try {
                    mCameraManager.openCamera(mCameraIDsList[1], mCameraStateCB, new Handler());
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        protected void close() {
            try {
                if (mCaptureSession != null) {
                    mCaptureSession.stopRepeating();
                    mCaptureSession.close();
                    mCaptureSession = null;
                }

                mIsCameraConfigured = false;
            } catch (final CameraAccessException e) {
                // Doesn't matter, cloising device anyway
                e.printStackTrace();
            } catch (final IllegalStateException e2) {
                // Doesn't matter, cloising device anyway
                e2.printStackTrace();
            } finally {
                if (mCameraDevice != null) {
                    mCameraDevice.close();
                    mCameraDevice = null;
                    mCaptureSession = null;
                }
            }
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CAMERA_OPENED:
                case MSG_SURFACE_READY:
                    // if both surface is created and camera device is opened
                    // - ready to set up preview and other things
                    if (mSurfaceCreated && (mCameraDevice != null)
                            && !mIsCameraConfigured) {
                        configureCamera();
                    }
                    break;
            }

            return true;
        }

        private void configureCamera() {
            // prepare list of surfaces to be used in capture requests
            List<Surface> sfl = new ArrayList<>();
            sfl.add(mCameraSurface); // surface for viewfinder preview

            // configure camera with all the surfaces to be ever used
            try {
                mCameraDevice.createCaptureSession(sfl, new CaptureSessionListener(), null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

            mIsCameraConfigured = true;
        }



/*        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            switch (requestCode) {
                case MY_PERMISSIONS_REQUEST_CAMERA:
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                        try {
                            mCameraManager.openCamera(mCameraIDsList[1], mCameraStateCB, new Handler());
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    break;
            }
        }
        */

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mCameraSurface = holder.getSurface();
            mSurfaceHolder = holder;
//            drawRectangle(200, 200);

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mCameraSurface = holder.getSurface();
            mSurfaceCreated = true;
            mHandler.sendEmptyMessage(MSG_SURFACE_READY);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mSurfaceCreated = false;
        }

        private class CaptureSessionListener extends
                CameraCaptureSession.StateCallback {
            @Override
            public void onConfigureFailed(final CameraCaptureSession session) {
                Log.d(TAG, "CaptureSessionConfigure failed");
            }

            @Override
            public void onConfigured(final CameraCaptureSession session) {
                Log.d(TAG, "CaptureSessionConfigure onConfigured");
                mCaptureSession = session;

                try {
                    CaptureRequest.Builder previewRequestBuilder = mCameraDevice
                            .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    previewRequestBuilder.addTarget(mCameraSurface);
                    mCaptureSession.setRepeatingRequest(previewRequestBuilder.build(),
                            null, null);
                } catch (CameraAccessException e) {
                    Log.d(TAG, "setting up preview failed");
                    e.printStackTrace();
                }
            }
        }
    }
}