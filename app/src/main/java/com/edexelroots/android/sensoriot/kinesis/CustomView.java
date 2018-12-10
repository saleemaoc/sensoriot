package com.edexelroots.android.sensoriot.kinesis;

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

import com.edexelroots.android.sensoriot.Utils;

public class CustomView extends SurfaceView {

    private final Paint paint;
    private final SurfaceHolder mHolder;
    private final Context context;

    public CustomView(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        this.context = context;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas2) {
        super.onDraw(canvas2);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            invalidate();
            if (mHolder.getSurface().isValid()) {
                final Canvas canvas = mHolder.lockCanvas();
                Utils.logE(getClass().getName(), "touchRecieved by camera");
                if (canvas != null) {
                    Utils.logE(getClass().getName(), "touchRecieved CANVAS STILL Not Null");
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
//                canvas.drawColor(Color.TRANSPARENT);
                    canvas.drawCircle(100, 100, 100, paint);
                    mHolder.unlockCanvasAndPost(canvas);
                    new Handler().postDelayed(() -> {
                        Canvas canvas1 = mHolder.lockCanvas();
                        if(canvas1 !=null){
                            canvas1.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                            mHolder.unlockCanvasAndPost(canvas1);
                        }
                    }, 1000);

                }
//                mHolder.unlockCanvasAndPost(canvas);


            }

        }


        return false;
    }
}
