package com.edexelroots.android.sensoriot.kinesis;

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

import java.util.ArrayList;
import java.util.List;

public class KDSConsumer {

    private AmazonKinesis amazonKinesis;
    private final String iteratorType = "TRIM_HORIZON";

    private final String TAG = getClass().getName();

    public KDSConsumer() {
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
        Shard shard = retrieveShards().get(0);
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
                Utils.logE(TAG, r.getSequenceNumber());
                Utils.logE(TAG, r.getPartitionKey());
                byte[] bytes = r.getData().array();
                Utils.logE(TAG, new String(bytes));
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException exception) {
                throw new RuntimeException(exception);
            }

            shardIterator = result.getNextShardIterator();
        }

    }
}
