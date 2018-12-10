package com.edexelroots.android.sensoriot.kinesis;

import android.app.Activity;
import android.content.Context;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.widget.Toast;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.model.DescribeStreamRequest;
import com.amazonaws.services.kinesis.model.DescribeStreamResult;
import com.amazonaws.services.kinesis.model.GetRecordsRequest;
import com.amazonaws.services.kinesis.model.GetRecordsResult;
import com.amazonaws.services.kinesis.model.GetShardIteratorRequest;
import com.amazonaws.services.kinesis.model.GetShardIteratorResult;
import com.amazonaws.services.kinesis.model.Record;
import com.amazonaws.services.kinesis.model.Shard;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.KinesisDataStream;
import com.edexelroots.android.sensoriot.Constants;
import com.edexelroots.android.sensoriot.SensorIoTApp;
import com.edexelroots.android.sensoriot.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class KDSConsumer {

    private AmazonKinesis amazonKinesis;
//    private final String iteratorType = "TRIM_HORIZON";
    private final String iteratorType = "LATEST";

    private final String TAG = getClass().getName();

    private Context mActivity = null;

    public KDSConsumer(Activity a) {
        this.mActivity = a;
        amazonKinesis = new AmazonKinesisClient(SensorIoTApp.getCredentialsProvider());
        amazonKinesis.setRegion(Region.getRegion(SensorIoTApp.KINESIS_VIDEO_REGION));
    }

    public List<Shard> retrieveShards() {
        DescribeStreamRequest describeStreamRequest = new DescribeStreamRequest();
        describeStreamRequest.setStreamName(Constants.Rekognition.kdsStreamName);
        List<Shard> shards = new ArrayList<>();
        String exclusiveStartShardId = null;
        do {
            describeStreamRequest.setExclusiveStartShardId( exclusiveStartShardId );
            DescribeStreamResult describeStreamResult = amazonKinesis.describeStream( describeStreamRequest );
            shards.addAll( describeStreamResult.getStreamDescription().getShards() );
            if (describeStreamResult.getStreamDescription().getHasMoreShards() && shards.size() > 0) {
                exclusiveStartShardId = shards.get(shards.size() - 1).getShardId();
            } else {
                exclusiveStartShardId = null;
            }
        } while ( exclusiveStartShardId != null );
        return shards;
    }

    public void execute() {
        isExecuting = true;
        Utils.logE(TAG, "Executing KDSWorker!");
        new Thread(() -> {
            try {
                getData();
            } catch (Exception e) {
                Utils.logE(TAG, "ERRRORR!!");
                e.printStackTrace();
            }
        }).start();
    }

    public static boolean isExecuting = false;

    public void getData() throws Exception {
        List<Shard> shards = retrieveShards();
        if(shards.size() <= 0) {
            throw new RuntimeException("No shards found!!");
        }
        Shard shard = retrieveShards().get(shards.size() - 1);
        GetShardIteratorRequest getShardIteratorRequest = new GetShardIteratorRequest();
        getShardIteratorRequest.setStreamName(Constants.Rekognition.kdsStreamName);
        getShardIteratorRequest.setShardId(shard.getShardId());
        getShardIteratorRequest.setShardIteratorType(iteratorType);

        GetShardIteratorResult getShardIteratorResult = amazonKinesis.getShardIterator(getShardIteratorRequest);
        String shardIterator = getShardIteratorResult.getShardIterator();

        List<Record> records;

        while (isExecuting) {
            // Create a new getRecordsRequest with an existing shardIterator
            // Set the maximum records to return to 25
            GetRecordsRequest getRecordsRequest = new GetRecordsRequest();
            getRecordsRequest.setShardIterator(shardIterator);
            getRecordsRequest.setLimit(25);

            GetRecordsResult result = amazonKinesis.getRecords(getRecordsRequest);

            // Put the result into record list. The result can be empty.
            records = result.getRecords();
            Utils.logE(TAG, "RECORDS LENGTH: " + records.size());

            for (Record r : records) {
//                Utils.logE(TAG, r.getSequenceNumber());
//                Utils.logE(TAG, r.getPartitionKey());
                byte[] bytes = r.getData().array();
                JSONObject jo = new JSONObject(new String(bytes));
//                Utils.logE(TAG, "Data: " + jo.toString());
                try {
                    JSONArray faceSearchResponse = jo.getJSONArray("FaceSearchResponse");
                    parseFaces(faceSearchResponse);
                    // Utils.logE(TAG, "Face Search >> " + faceSearchResponse.toString());
                }catch (JSONException je) {
                    je.printStackTrace();
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException exception) {
                throw new RuntimeException(exception);
            }

            shardIterator = result.getNextShardIterator();
        }

    }

    private void parseFaces(JSONArray facesArray) throws JSONException {
        for(int i=0; i< facesArray.length(); i++) {
            JSONObject face = facesArray.getJSONObject(i);
            if(face.has("MatchedFaces")) {
                // we have matched faces
                JSONArray matchedFaces = face.getJSONArray("MatchedFaces");
                for(int j=0; j<matchedFaces.length(); j++) {
                    JSONObject matchedFace = matchedFaces.getJSONObject(j);
                    String similarity = matchedFace.getString("Similarity");
                    JSONObject faceDetails = matchedFace.getJSONObject("Face");
                    String externalImageId = "";
                    if(faceDetails.has("ExternalImageId")) {
                        externalImageId = faceDetails.getString("ExternalImageId");
                    }
                    String imageId = faceDetails.getString("ImageId");
                    String matchResult = "Matched: ";
                    if(!TextUtils.isEmpty(imageId)) {
                        matchResult += "ImageId = " + imageId;
                    }
                    if(!TextUtils.isEmpty(externalImageId)) {
                        matchResult += ", ExternalImageId = " + externalImageId;
                    }
                    matchResult += ", Similarity = " + similarity;

                    String finalMatchResult = matchResult;
                    ((KinesisActivity) mActivity).runOnUiThread(() -> Toast.makeText(mActivity, finalMatchResult, Toast.LENGTH_SHORT).show());

                    Utils.logE(TAG, matchResult);
                }
            }
        }
    }
}



/*
[
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
        },
        "MatchedFaces": [
            {
                "Face": {
                    "BoundingBox": {
                        "Height": 0.288462,
                        "Left": 0.361538,
                        "Top": 0.161538,
                        "Width": 0.284615
                    },
                    "Confidence": 99.995705,
                    "ExternalImageId": "thomas",
                    "FaceId": "2a0d0e3b-64f2-48fd-8d71-9f54ffab858f",
                    "ImageId": "4d72071a-43bb-5c6f-8359-84ea59af1934"
                },
                "Similarity": 92.7961
            },
            {
                "Face": {
                    "BoundingBox": {
                        "Height": 0.288462,
                        "Left": 0.361538,
                        "Top": 0.161538,
                        "Width": 0.284615
                    },
                    "Confidence": 99.995705,
                    "ExternalImageId": "thomas.jpeg",
                    "FaceId": "6a1da24e-52f2-4137-8a80-1ab7991cdfec",
                    "ImageId": "de1c51d5-0f2b-53c2-8746-c11dd8016336"
                },
                "Similarity": 92.7961
            }
        ]
    }
]
*/