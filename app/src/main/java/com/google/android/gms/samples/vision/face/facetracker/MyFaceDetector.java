package com.google.android.gms.samples.vision.face.facetracker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;


public class MyFaceDetector extends Detector {
    private Detector<Face> mDelegate;

    FaceTrackerActivity fta;
    MyFaceDetector(Detector<Face> delegate, FaceTrackerActivity activity) {
        mDelegate = delegate;
        fta = activity;
    }

    public SparseArray<Face> detect(Frame frame) {
/*
        Frame outputFrame = new Frame.Builder()
                .setImageData(frame.getGrayscaleImageData(), frame.getMetadata().getWidth(),
                        frame.getMetadata().getHeight(), ImageFormat.NV21)
                .setId(frame.getMetadata().getId())
                .setTimestampMillis(frame.getMetadata().getTimestampMillis())
                .setRotation(frame.getMetadata().getRotation())
                .build();

*/
        SparseArray<Face> faces = mDelegate.detect(frame);

        int w = frame.getMetadata().getWidth();
        int h = frame.getMetadata().getHeight();

        final Bitmap bitmap;
        ByteBuffer byteBufferRaw = frame.getGrayscaleImageData();
        byte[] byteBuffer = byteBufferRaw.array();
        YuvImage yuvimage  = new YuvImage(byteBuffer, ImageFormat.NV21, w, h, null);



        if(faces.size() > 0) {
            Face face = faces.valueAt(0);
            int left = (int) face.getPosition().x;
            int top = (int) face.getPosition().y;
            int right = (int) face.getWidth() + left;
            int bottom = (int) face.getHeight() + top;

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                yuvimage.compressToJpeg(new Rect(left, top, right, bottom), 80, baos);
                byte[] jpegArray = baos.toByteArray();
                fta.runOnUiThread(() -> fta.faceToImageView(jpegArray, byteBufferRaw, face.getId()));
            }catch (Exception e) {
                e.printStackTrace();
            }


        }
        return faces;
    }

    public boolean isOperational() {
        return mDelegate.isOperational();
    }

    public boolean setFocus(int id) {
        return mDelegate.setFocus(id);
    }
}
