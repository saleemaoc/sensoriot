package com.edexelroots.android.sensoriot.vision.api;

import com.google.gson.annotations.SerializedName;

public class FaceUploadRequest {

//    body: {"photo":"base_64_encoded_jpg_or_png"}

    @SerializedName("photo")
    public String photo;

}
