package com.edexelroots.android.sensoriot;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;

public interface CredentialsReciever {

    public void onCredentialsRecieved(CognitoCachingCredentialsProvider credentialsProvider);
}
