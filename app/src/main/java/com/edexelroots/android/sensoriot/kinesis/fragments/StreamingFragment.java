package com.edexelroots.android.sensoriot.kinesis.fragments;

import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.edexelroots.android.sensoriot.kinesis.KDSWorker;
import com.edexelroots.android.sensoriot.kinesis.KinesisActivity;


public class StreamingFragment extends Fragment implements TextureView.SurfaceTextureListener {
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

    TextureView mTextureView;

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        getArguments().setClassLoader(AndroidCameraMediaSourceConfiguration.class.getClassLoader());
        mStreamName = getArguments().getString(KEY_STREAM_NAME);
        mConfiguration = getArguments().getParcelable(KEY_MEDIA_SOURCE_CONFIGURATION);

        final View view = inflater.inflate(R.layout.fragment_streaming, container, false);
        mTextureView = (TextureView) view.findViewById(R.id.texture);
/*
        int orientation = getResources().getConfiguration().orientation;
        if(orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(getContext(), "Landscape",Toast.LENGTH_SHORT ).show();
        } else {
//            view.findViewById(R.id.texture_container).setRotation(90);
//            mTextureView.setRotation(90);
            Toast.makeText(getContext(), "Portrait",Toast.LENGTH_SHORT ).show();
        }
*/
        mTextureView.setSurfaceTextureListener(this);
        return view;
    }

    private void adjustAspectRatio(int videoWidth, int videoHeight) {
        int viewWidth = mTextureView.getWidth();
        int viewHeight = mTextureView.getHeight();
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
        mTextureView.getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        txform.postRotate(45);          // just for fun
        txform.postTranslate(xoff, yoff);
        mTextureView.setTransform(txform);
    }

    private void createClientAndStartStreaming(final SurfaceTexture previewTexture) {

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

            Surface surface = new Surface(previewTexture);
            mCameraMediaSource.setPreviewSurfaces(surface);

            if(sm == null) {
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
        return new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                pauseStreaming();
                mActivity.startConfigFragment();
            }
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
            startRekStreamProcessor();

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
             stopRekStreamProcessor();
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

    private void stopExecutor() {
        Utils.logE(getClass().getName(), "Stop executor!!");
    }

    StreamManager sm = null;
    private void startRekStreamProcessor() {
        Utils.logE(getClass().getName(), "Start Rekog processor!!");
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    sm.createStreamProcessor();
                    sm.listStreamProcessors();
                    sm.describeStreamProcessor();
                    sm.startStreamProcessor();
                }
            }).start();
            KDSWorker.execute();
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    private void stopRekStreamProcessor() {
        if(sm != null) {
            try {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sm.stopStreamProcessor();
                        sm.deleteStreamProcessor();
                    }
                }).start();
                KDSWorker.deleteResources();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }





    ////
    // TextureView.SurfaceTextureListener methods
    ////

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        surfaceTexture.setDefaultBufferSize(1280, 720);
        createClientAndStartStreaming(surfaceTexture);
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
}
