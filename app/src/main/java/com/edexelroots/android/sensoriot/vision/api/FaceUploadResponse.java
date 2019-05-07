package com.edexelroots.android.sensoriot.vision.api;

import com.google.gson.annotations.SerializedName;

public class FaceUploadResponse {

/*
    { "result": false, "message":"string"}}
    or
    { "result": true, "face":{"name":"string","title":"string","url":"string"}}
*/
    @SerializedName("result")
    public boolean result;

    @SerializedName("message")
    public String message;

    @SerializedName("face")
    FaceResponse face;

}
