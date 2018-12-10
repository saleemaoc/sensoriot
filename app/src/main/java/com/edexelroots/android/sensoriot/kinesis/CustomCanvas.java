package com.edexelroots.android.sensoriot.kinesis;

import android.graphics.Canvas;
import android.graphics.SurfaceTexture;

public class CustomCanvas extends Canvas {


    public void drawFace(float boxLeft, float boxTop, float boxWidth, float boxHeight, int previewWidth, int previewHeight) {
        // top, left, bottom and right params come from aws api
        // width and height params too
        // calculate actual top, left, bottom and right for the box
        // by using t * h
        int top = (int) (boxTop * previewHeight);
        int left = (int) (boxLeft * previewWidth);
        boxWidth = boxWidth * previewWidth; // convert from ratio to pixels
        boxHeight = boxHeight * previewHeight; // convert from ratio to pixels

        int bottom = (int) (top + boxHeight);
        int right = (int) (left + boxWidth);
    }



}
