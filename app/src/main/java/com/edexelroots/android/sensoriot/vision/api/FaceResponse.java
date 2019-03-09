package com.edexelroots.android.sensoriot.vision.api;

import com.google.gson.annotations.SerializedName;

public class FaceResponse {
    @SerializedName("name")
    public String name;

    @SerializedName("title")
    public String title;

    @SerializedName("url")
    public String url;

    @SerializedName("errorMessage")
    public String errorMessage = "";

    public String toString() {
        return String.format("%s : %s\n%s", this.name, this.title, this.url);
    }
}
