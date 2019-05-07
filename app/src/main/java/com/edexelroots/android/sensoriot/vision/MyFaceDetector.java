package com.edexelroots.android.sensoriot.vision;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.SparseArray;

import com.edexelroots.android.sensoriot.Utils;
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

    private int extensionOffset = 0;

    public SparseArray<Face> detect(Frame frame) {
        if (Utils.isPortraitMode(fta)) {
            return getFacesPortrait(frame);
        }
        return getFacesLandscape(frame);
    }

    public SparseArray<Face> getFacesLandscape(Frame frame) {
        SparseArray<Face> faces = mDelegate.detect(frame);
        int w = frame.getMetadata().getWidth();
        int h = frame.getMetadata().getHeight();

        ByteBuffer byteBufferRaw = frame.getGrayscaleImageData();
        byte[] byteBuffer = byteBufferRaw.array();
        YuvImage yuvimage = new YuvImage(byteBuffer, ImageFormat.NV21, w, h, null);


        if (faces.size() > 0) {
            Face face = faces.valueAt(0);
            int x = (int) face.getPosition().x;
            int y = (int) face.getPosition().y;
            int width = (int) face.getWidth();
            int height = (int) face.getHeight();

            int left = x - extensionOffset;
            int top = y - extensionOffset;
            int right = left + width + extensionOffset;
            int bottom = top + height + extensionOffset;

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Rect r = new Rect(left, top, right, bottom);
                if (!r.isEmpty()) {
                    yuvimage.compressToJpeg(r, 90, baos);
                    byte[] jpegArray = baos.toByteArray();
                    fta.runOnUiThread(() -> fta.faceToImageViewLandscape(jpegArray, face.getId()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return faces;
    }

    private SparseArray<Face> getFacesPortrait(Frame frame) {
        SparseArray<Face> faces2 = mDelegate.detect(frame);
        int w = frame.getMetadata().getWidth();
        int h = frame.getMetadata().getHeight();

        ByteBuffer byteBufferRaw = frame.getGrayscaleImageData();
        byte[] byteBuffer = byteBufferRaw.array();
        YuvImage yuvimage = new YuvImage(byteBuffer, ImageFormat.NV21, w, h, null);
        int right = frame.getMetadata().getWidth();
        int bottom = frame.getMetadata().getHeight();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Rect r2 = new Rect(0, 0, right, bottom);
            yuvimage.compressToJpeg(r2, 90, baos);
            byte[] bytes = baos.toByteArray();

            //create a rotated bitmap
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            bitmap = rotated;

            Frame frame2 = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<Face> faces = mDelegate.detect(frame2);

            if (faces.size() > 0) {
                Face face = faces.valueAt(0);
                int left = (int) face.getPosition().x - extensionOffset;
                int top = (int) face.getPosition().y - extensionOffset;
                int width = (int) face.getWidth() + extensionOffset;
                int height = (int) face.getHeight() + extensionOffset;

                top = top > 0 ? top : 0;

                try {
                    Bitmap clipped = Bitmap.createBitmap(bitmap, left, top, width, height);
                    bitmap.recycle();
                    bitmap = rotateBitmap(-90, clipped);

                    Bitmap finalBitmap = bitmap;
                    fta.runOnUiThread(() -> fta.faceToImageViewPortrait(finalBitmap, face.getId()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return faces2;
    }

    private Bitmap rotateBitmap(int angle, Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle();
        return rotated;
    }

    public boolean isOperational() {
        return mDelegate.isOperational();
    }

    public boolean setFocus(int id) {
        return mDelegate.setFocus(id);
    }
}
