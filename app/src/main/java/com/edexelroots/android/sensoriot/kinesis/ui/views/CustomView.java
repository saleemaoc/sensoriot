package com.edexelroots.android.sensoriot.kinesis.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import com.edexelroots.android.sensoriot.Utils;

public class CustomView extends SurfaceView {

    private final Paint paint, paint2;

    private final SurfaceHolder mHolder;
    private final Context context;

    public CustomView(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        this.context = context;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);

        paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint2.setColor(Color.GREEN);
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setStrokeWidth(6f);
    }

    @Override
    protected void onDraw(Canvas canvas2) {
        super.onDraw(canvas2);
    }
/*

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            invalidate();

        }

        return false;
    }
*/

    public void indicateFace(String faceName, float cx, float cy, float wd, float ht) {
        Utils.logE(getClass().getName(), "CustomView: " + cx + ", " + cy + ", " + wd + ", " + ht);
        if (mHolder.getSurface().isValid()) {
            final Canvas canvas = mHolder.lockCanvas();
            if (canvas != null) {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                cx = getWidth() - cx;
                float bottom = cy + ht;
                float right = cx - wd;

                canvas.drawRect(cx, cy, right, bottom, paint);

                /*
                // top left and bottom right of the overlay
                canvas.drawCircle(0,0,20, paint);
                canvas.drawCircle(getWidth(),getHeight(),20, paint);*/
                /*
                // top-left and bottom-right of the texture view
                canvas.drawCircle(tv.getLeft(),tv.getTop(),20, paint2);
                canvas.drawCircle(tv.getWidth(), tv.getHeight(),20, paint2);
                */

                /*
                canvas.drawCircle(cx,cy,10, paint2);
                canvas.drawCircle(right,bottom,10, paint);
                */

                mHolder.unlockCanvasAndPost(canvas);
                new Handler().postDelayed(() -> {
                    Canvas canvas1 = mHolder.lockCanvas();
                    if(canvas1 != null) {
                        canvas1.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        mHolder.unlockCanvasAndPost(canvas1);
                    }
                }, 1000);

            }
        }
    }
}

/*
{
"DetectedFace": {
    "BoundingBox": {
        "Height": 0.11217949,
        "Left": 0.46634614,
        "Top": 0.42628205,
        "Width": 0.084134616
    },
    "Confidence": 99.95089,
    "Landmarks": [
        {
            "Type": "eyeLeft",
            "X": 0.49466753,
            "Y": 0.4744765
        },
        {
            "Type": "eyeRight",
            "X": 0.52489024,
            "Y": 0.47281438
        },
        {
            "Type": "nose",
            "X": 0.5127326,
            "Y": 0.49460843
        },
        {
            "Type": "mouthLeft",
            "X": 0.4986741,
            "Y": 0.51601464
        },
        {
            "Type": "mouthRight",
            "X": 0.5227541,
            "Y": 0.5153943
        }
    ],
    "Pose": {
        "Pitch": 4.0858536,
        "Roll": -3.3027194,
        "Yaw": 9.092213
    },
    "Quality": {
        "Brightness": 95.21252,
        "Sharpness": 9.962683
    }
}
*/
