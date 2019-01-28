package com.edexelroots.android.sensoriot;

//Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
//PDX-License-Identifier: MIT-0 (For image, see https://github.com/awsdocs/amazon-rekognition-developer-guide/blob/master/LICENSE-SAMPLECODE.)

// Stream manager class. Provides methods for calling
// Stream Processor operations.

import android.content.Context;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.CreateStreamProcessorRequest;
import com.amazonaws.services.rekognition.model.CreateStreamProcessorResult;
import com.amazonaws.services.rekognition.model.DeleteStreamProcessorRequest;
import com.amazonaws.services.rekognition.model.DeleteStreamProcessorResult;
import com.amazonaws.services.rekognition.model.DescribeStreamProcessorRequest;
import com.amazonaws.services.rekognition.model.DescribeStreamProcessorResult;
import com.amazonaws.services.rekognition.model.FaceMatch;
import com.amazonaws.services.rekognition.model.FaceSearchSettings;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.InvalidParameterException;
import com.amazonaws.services.rekognition.model.KinesisDataStream;
import com.amazonaws.services.rekognition.model.KinesisVideoStream;
import com.amazonaws.services.rekognition.model.ListStreamProcessorsRequest;
import com.amazonaws.services.rekognition.model.ListStreamProcessorsResult;
import com.amazonaws.services.rekognition.model.ResourceInUseException;
import com.amazonaws.services.rekognition.model.ResourceNotFoundException;
import com.amazonaws.services.rekognition.model.SearchFacesByImageRequest;
import com.amazonaws.services.rekognition.model.SearchFacesByImageResult;
import com.amazonaws.services.rekognition.model.StartStreamProcessorRequest;
import com.amazonaws.services.rekognition.model.StartStreamProcessorResult;
import com.amazonaws.services.rekognition.model.StopStreamProcessorRequest;
import com.amazonaws.services.rekognition.model.StopStreamProcessorResult;
import com.amazonaws.services.rekognition.model.StreamProcessor;
import com.amazonaws.services.rekognition.model.StreamProcessorInput;
import com.amazonaws.services.rekognition.model.StreamProcessorOutput;
import com.amazonaws.services.rekognition.model.StreamProcessorSettings;
import com.amazonaws.services.rekognition.model.StreamProcessorStatus;
import com.edexelroots.android.sensoriot.iot.MqttPublishManager;
import com.edexelroots.android.sensoriot.vision.FaceMatchItem;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class StreamManager {

    private String streamProcessorName;
    private String kinesisVideoStreamArn;
    private String kinesisDataStreamArn;
    private String roleArn;
    private String collectionId;
    private float matchThreshold;

    private AmazonRekognition rekognitionClient;


    Context mContext = null;

    public StreamManager(Context c) {
        this(c,
                Constants.Rekognition.streamProcessorName,
                Constants.Rekognition.kinesisVideoStreamArn,
                Constants.Rekognition.kinesisDataStreamArn,
                Constants.Rekognition.roleArn,
                Constants.Rekognition.collectionId,
                Constants.Rekognition.matchThreshold);
        mContext = c;
    }

    public StreamManager(Context c, String spName,
                         String kvStreamArn,
                         String kdStreamArn,
                         String iamRoleArn,
                         String collId,
                         Float threshold){
        streamProcessorName=spName;
        kinesisVideoStreamArn=kvStreamArn;
        kinesisDataStreamArn=kdStreamArn;
        roleArn=iamRoleArn;
        collectionId=collId;
        matchThreshold=threshold;
        CognitoCachingCredentialsProvider cp = new CognitoCachingCredentialsProvider(c, MqttPublishManager.COGNITO_POOL_ID, MqttPublishManager.MY_REGION);
        rekognitionClient = new AmazonRekognitionClient(cp);
        rekognitionClient.setRegion(Region.getRegion(Regions.AP_NORTHEAST_1));
        rekognitionClient.setEndpoint("rekognition.ap-northeast-1.amazonaws.com");
        mContext = c;

    }

    public void createStreamProcessor() {
        try {
            //Setup input parameters
            KinesisVideoStream kinesisVideoStream = new KinesisVideoStream().withArn(kinesisVideoStreamArn);
            StreamProcessorInput streamProcessorInput =
                    new StreamProcessorInput().withKinesisVideoStream(kinesisVideoStream);
            KinesisDataStream kinesisDataStream = new KinesisDataStream().withArn(kinesisDataStreamArn);
            StreamProcessorOutput streamProcessorOutput =
                    new StreamProcessorOutput().withKinesisDataStream(kinesisDataStream);
            FaceSearchSettings faceSearchSettings =
                    new FaceSearchSettings().withCollectionId(collectionId).withFaceMatchThreshold(matchThreshold);
            StreamProcessorSettings streamProcessorSettings =
                    new StreamProcessorSettings().withFaceSearch(faceSearchSettings);

            //Create the stream processor
            CreateStreamProcessorResult createStreamProcessorResult = rekognitionClient.createStreamProcessor(
                    new CreateStreamProcessorRequest().withInput(streamProcessorInput).withOutput(streamProcessorOutput)
                            .withSettings(streamProcessorSettings).withRoleArn(roleArn).withName(streamProcessorName));

            //Display result
            Utils.logE(getClass().getName(), "Stream Processor " + streamProcessorName + " created.");
            Utils.logE(getClass().getName(), "StreamProcessorArn - " + createStreamProcessorResult.getStreamProcessorArn());
        } catch (Exception e) {

        }
    }

    public boolean startFaceSearchRequest(ByteBuffer byteBuffer, FaceMatchItem faceMatchItem) throws InvalidParameterException {
        Image image = new Image().withBytes(byteBuffer);
        SearchFacesByImageRequest sfbir = new SearchFacesByImageRequest()
                .withImage(image)
                .withCollectionId(collectionId)
                .withFaceMatchThreshold(matchThreshold)
                .withMaxFaces(2);

        List<FaceMatch> faceImageMatches = new ArrayList<>();
        try {
            SearchFacesByImageResult sfbi = rekognitionClient.searchFacesByImage(sfbir);
            faceImageMatches = sfbi.getFaceMatches();
            for (FaceMatch face : faceImageMatches) {
                faceMatchItem.awsFaceId = face.getFace().getFaceId();
                faceMatchItem.name = face.getFace().getExternalImageId();
                faceMatchItem.similarity = face.getSimilarity();
                Utils.logE(getClass().getName(), faceMatchItem.name);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return faceImageMatches.size() > 0;
    }

    public void startStreamProcessor() {
        try {
            StartStreamProcessorResult startStreamProcessorResult =
                    rekognitionClient.startStreamProcessor(new StartStreamProcessorRequest().withName(streamProcessorName));
            Utils.logE(getClass(). getName(),"Stream Processor " + streamProcessorName + " started.");
            Utils.logE(getClass().getName(), startStreamProcessorResult.toString());
        }catch (ResourceInUseException riue) {
            riue.printStackTrace();
        }
    }

    public void stopStreamProcessor() {
        try{

            StopStreamProcessorResult stopStreamProcessorResult =
                    rekognitionClient.stopStreamProcessor(new StopStreamProcessorRequest().withName(streamProcessorName));
            Utils.logE(getClass().getName(), "Stream Processor " + streamProcessorName + " stopped.");
        } catch (ResourceInUseException riue) {
            riue.printStackTrace();
        }
    }

    public void deleteStreamProcessor() {
        DeleteStreamProcessorResult deleteStreamProcessorResult = rekognitionClient
                .deleteStreamProcessor(new DeleteStreamProcessorRequest().withName(streamProcessorName));
        Utils.logE(getClass().getName(), "Stream Processor " + streamProcessorName + " deleted.");
    }


     /* Creates a StreamProcess if it doesn't exist already. Once the stream processor is created, it's started and then
     * described to know the result of the stream processor.
     */
    public void process() {
        // Creates a stream processor if it doesn't already exist and start.
        try {
            DescribeStreamProcessorResult result = describeStreamProcessor();
            if (!result.getStatus().equals(StreamProcessorStatus.RUNNING.toString())) {
                startStreamProcessor();
            }
        } catch (ResourceNotFoundException e) {
            Utils.logE(getClass().getName(), "StreamProcessor with name : {} doesnt exist. Creating... " + streamProcessorName);
            createStreamProcessor();
            startStreamProcessor();
        }

        // Describe the Stream Processor results to log the status.
        describeStreamProcessor();
    }

    public DescribeStreamProcessorResult describeStreamProcessor() {
        DescribeStreamProcessorResult describeStreamProcessorResult = rekognitionClient
                .describeStreamProcessor(new DescribeStreamProcessorRequest().withName(streamProcessorName));

        //Display various stream processor attributes.
        Utils.logE(getClass().getName(), "Arn - " + describeStreamProcessorResult.getStreamProcessorArn());
        Utils.logE(getClass().getName(), "Input kinesisVideo stream - "
                + describeStreamProcessorResult.getInput().getKinesisVideoStream().getArn());
        Utils.logE(getClass().getName(), "Output kinesisData stream - "
                + describeStreamProcessorResult.getOutput().getKinesisDataStream().getArn());
        Utils.logE(getClass().getName(), "RoleArn - " + describeStreamProcessorResult.getRoleArn());
        Utils.logE(getClass().getName(),
                "CollectionId - " + describeStreamProcessorResult.getSettings().getFaceSearch().getCollectionId());
        Utils.logE(getClass().getName(), "Status - " + describeStreamProcessorResult.getStatus());
        Utils.logE(getClass().getName(), "Status message - " + describeStreamProcessorResult.getStatusMessage());
        Utils.logE(getClass().getName(), "Creation timestamp - " + describeStreamProcessorResult.getCreationTimestamp());
        Utils.logE(getClass().getName(), "Last update timestamp - " + describeStreamProcessorResult.getLastUpdateTimestamp());
        return describeStreamProcessorResult;
    }

    public void listStreamProcessors() {
        ListStreamProcessorsResult listStreamProcessorsResult =
                rekognitionClient.listStreamProcessors(new ListStreamProcessorsRequest().withMaxResults(100));

        //List all stream processors (and state) returned from Rekognition
        for (StreamProcessor streamProcessor : listStreamProcessorsResult.getStreamProcessors()) {
            Utils.logE(getClass().getName(), "StreamProcessor name - " + streamProcessor.getName());
            Utils.logE(getClass().getName(), "Status - " + streamProcessor.getStatus());
        }
    }
}