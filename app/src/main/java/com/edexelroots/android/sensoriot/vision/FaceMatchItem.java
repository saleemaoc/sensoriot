package com.edexelroots.android.sensoriot.vision;

import android.graphics.Bitmap;

/**
 * Helper class for providing sample name for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class FaceMatchItem{

    public float similarity = 0f;
    public final long id;
    public String name;
    public final Bitmap image;

    public FaceMatchItem(long id, float similarity, String name, Bitmap img) {
        this.id = id;
        this.similarity = similarity;
        this.name = name;
        this.image = img;
    }

    @Override
    public String toString() {
        return name;
    }
}
