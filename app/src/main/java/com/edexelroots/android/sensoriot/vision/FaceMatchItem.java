package com.edexelroots.android.sensoriot.vision;

import android.graphics.Bitmap;

/**
 * Helper class for providing sample name for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class FaceMatchItem {

//    public boolean blink = false;

    public String awsFaceId;

    public String name;
    public String subtitle;
    public String url;

    public Bitmap image;
    public float similarity = 0f;
    public int counter = 1;

    public FaceMatchItem(String awsFaceId, float similarity, String name, Bitmap img) {
        this.similarity = similarity;
        this.name = name;
        this.image = img;
        this.awsFaceId = awsFaceId;
    }

    @Override
    public String toString() {
        return name;
    }
}
