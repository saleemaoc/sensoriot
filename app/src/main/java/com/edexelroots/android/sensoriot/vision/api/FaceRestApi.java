package com.edexelroots.android.sensoriot.vision.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface FaceRestApi {

    @GET("faces")
    Call<FaceResponse> getFace(@Query("faceid") String faceId);

    @POST("faceid")
    Call<FaceUploadResponse> uploadFace(@Body FaceUploadRequest request);
}
