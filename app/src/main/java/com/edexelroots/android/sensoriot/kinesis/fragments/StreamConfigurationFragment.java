package com.edexelroots.android.sensoriot.kinesis.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.amazonaws.kinesisvideo.client.KinesisVideoClient;
import com.amazonaws.kinesisvideo.client.mediasource.CameraMediaSourceConfiguration;
import com.amazonaws.kinesisvideo.common.exception.KinesisVideoException;
import com.amazonaws.kinesisvideo.producer.StreamInfo;
import com.amazonaws.mobileconnectors.kinesisvideo.client.KinesisVideoAndroidClientFactory;
import com.amazonaws.mobileconnectors.kinesisvideo.data.MimeType;
import com.amazonaws.mobileconnectors.kinesisvideo.mediasource.android.AndroidCameraMediaSourceConfiguration;
import com.edexelroots.android.sensoriot.SensorIoTApp;
import com.edexelroots.android.sensoriot.MainActivity;
import com.edexelroots.android.sensoriot.R;
import com.edexelroots.android.sensoriot.Utils;
import com.edexelroots.android.sensoriot.kinesis.KinesisActivity;
import com.edexelroots.android.sensoriot.kinesis.ui.adapter.ToStrings;
import com.edexelroots.android.sensoriot.kinesis.ui.widget.StringSpinnerWidget;

import static com.amazonaws.mobileconnectors.kinesisvideo.util.CameraUtils.getCameras;
import static com.amazonaws.mobileconnectors.kinesisvideo.util.CameraUtils.getSupportedResolutions;
import static com.amazonaws.mobileconnectors.kinesisvideo.util.VideoEncoderUtils.getSupportedMimeTypes;

public class StreamConfigurationFragment extends Fragment {
    private static final String TAG = StreamConfigurationFragment.class.getSimpleName();
//    private static final Size RESOLUTION_320x240 = new Size(320, 240);
    public static final Size RESOLUTION_320x240 = new Size(240, 320);
    private static final int FRAMERATE_20 = 20;
    private static final int BITRATE_384_KBPS = 384 * 1024;
    private static final int RETENTION_PERIOD_48_HOURS = 2 * 24;

    private Button mStartStreamingButton;
    private EditText mStreamName;
    private KinesisVideoClient mKinesisVideoClient;

    private StringSpinnerWidget<CameraMediaSourceConfiguration> mCamerasDropdown;
    private StringSpinnerWidget<Size> mResolutionDropdown;
    private StringSpinnerWidget<MimeType> mMimeTypeDropdown;

    private KinesisActivity mActivity;

    public static StreamConfigurationFragment newInstance(KinesisActivity navActivity) {
        StreamConfigurationFragment s = new StreamConfigurationFragment();
        s.mActivity = navActivity;
        return s;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        if (ContextCompat.checkSelfPermission(this.getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this.getActivity(), new String[]{Manifest.permission.CAMERA},9393 );
        }

        getActivity().setTitle(getActivity().getString(R.string.title_fragment_stream));

        final View view = inflater.inflate(R.layout.fragment_stream_configuration, container, false);

        try {
            mKinesisVideoClient = KinesisVideoAndroidClientFactory.createKinesisVideoClient(
                    getActivity(),
                    SensorIoTApp.KINESIS_VIDEO_REGION,
                    SensorIoTApp.getCredentialsProvider());
        } catch (KinesisVideoException e) {
            Log.e(TAG, "Failed to create Kinesis Video client", e);
        } catch (Exception e) {
            Toast.makeText(getContext(), "An error occurred! " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Utils.logE(getClass().getName(), e.getMessage());
        }

        mCamerasDropdown = new StringSpinnerWidget<>(
                getActivity(),
                view,
                R.id.cameras_spinner,
                ToStrings.CAMERA_DESCRIPTION,
                getCameras(mKinesisVideoClient));

        mCamerasDropdown.setItemSelectedListener(
                new StringSpinnerWidget.ItemSelectedListener<CameraMediaSourceConfiguration>() {
                    @Override
                    public void itemSelected(final CameraMediaSourceConfiguration mediaSource) {
                        mResolutionDropdown = new StringSpinnerWidget<>(
                                getActivity(),
                                view,
                                R.id.resolutions_spinner,
                                getSupportedResolutions(getActivity(), mediaSource.getCameraId()));
                        select640orBelow();
                    }
                });

        mMimeTypeDropdown = new StringSpinnerWidget<>(
                getActivity(),
                view,
                R.id.codecs_spinner,
                getSupportedMimeTypes());

        return view;
    }

    private void select640orBelow() {
        Size tmpSize = new Size(0, 0);
        int indexToSelect = 0;
        for (int i = 0; i < mResolutionDropdown.getCount(); i++) {
            final Size resolution = mResolutionDropdown.getItem(i);
            if (resolution.getWidth() <= RESOLUTION_320x240.getWidth()
                    && tmpSize.getWidth() <= resolution.getWidth()
                    && resolution.getHeight() <= RESOLUTION_320x240.getHeight()
                    && tmpSize.getHeight() <=resolution.getHeight()) {

                tmpSize = resolution;
                indexToSelect = i;
            }
        }

        mResolutionDropdown.selectItem(indexToSelect);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mStartStreamingButton = (Button) view.findViewById(R.id.start_streaming);
        mStartStreamingButton.setOnClickListener(startStreamingActivityWhenClicked());
        mStreamName = (EditText) view.findViewById(R.id.stream_name);
        mStreamName.setText(StreamingFragment.KEY_STREAM_NAME);
    }

    private View.OnClickListener startStreamingActivityWhenClicked() {
        return new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                startStreaming();
            }
        };
    }

    private void startStreaming() {
        final Bundle extras = new Bundle();

        Utils.logE(getClass().getName(), "0 Camera Orientation: " + mCamerasDropdown.getSelectedItem().getCameraOrientation());

        AndroidCameraMediaSourceConfiguration acmsc = getCurrentConfiguration();
        extras.putParcelable(StreamingFragment.KEY_MEDIA_SOURCE_CONFIGURATION, acmsc);
        Utils.logE(getClass().getName(), "1 Camera Orientation: " + acmsc.getCameraOrientation());

        extras.putString(StreamingFragment.KEY_STREAM_NAME, mStreamName.getText().toString());

        mActivity.startStreamingFragment(extras);
    }

    private AndroidCameraMediaSourceConfiguration getCurrentConfiguration() {
        Utils.logE(getClass().getName(), "Camera Orientation: " + mCamerasDropdown.getSelectedItem().getCameraOrientation());
        CameraMediaSourceConfiguration.Builder builder = AndroidCameraMediaSourceConfiguration.builder()
                .withCameraId(mCamerasDropdown.getSelectedItem().getCameraId())
                .withEncodingMimeType(mMimeTypeDropdown.getSelectedItem().getMimeType())
                .withHorizontalResolution(mResolutionDropdown.getSelectedItem().getWidth())
                .withVerticalResolution(mResolutionDropdown.getSelectedItem().getHeight())
                .withCameraFacing(mCamerasDropdown.getSelectedItem().getCameraFacing())
                .withIsEncoderHardwareAccelerated(mCamerasDropdown.getSelectedItem().isEndcoderHardwareAccelerated())
                .withFrameRate(FRAMERATE_20)
                .withRetentionPeriodInHours(RETENTION_PERIOD_48_HOURS)
                .withEncodingBitRate(BITRATE_384_KBPS)
                .withCameraOrientation(-mCamerasDropdown.getSelectedItem().getCameraOrientation())
                .withNalAdaptationFlags(StreamInfo.NalAdaptationFlags.NAL_ADAPTATION_ANNEXB_CPD_AND_FRAME_NALS)
                .withIsAbsoluteTimecode(false);
        return new AndroidCameraMediaSourceConfiguration(builder);
    }
}