package com.edexelroots.android.sensoriot.vision.api;

import com.edexelroots.android.sensoriot.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FaceApiService {

    FaceRestApi restApi;
    public static final String BASE_URL = "https://s9jffrx7j5.execute-api.ap-southeast-1.amazonaws.com/v1/";

    public FaceApiService(){
        Gson gson = new GsonBuilder().serializeNulls().create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        restApi = retrofit.create(FaceRestApi.class);
    }

    public void getFace(String faceId, Callback<FaceResponse> callback) {
        if(restApi == null) {
            Utils.logE(getClass().getName(), "restApi is null");
            return;
        }
        Call<FaceResponse> faceCall = restApi.getFace(faceId);
        faceCall.enqueue(callback);
    }

    public void uploadFace(String photo, Callback<FaceUploadResponse> callback) {
        if(restApi == null) {
            Utils.logE(getClass().getName(), "restApi is null");
            return;
        }

        FaceUploadRequest fur = new FaceUploadRequest();
        fur.photo = photo;
        Call<FaceUploadResponse> faceUploadCall = restApi.uploadFace(fur);
        faceUploadCall.enqueue(callback);
    }

}
