package com.edexelroots.android.sensoriot.kinesis;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.edexelroots.android.sensoriot.Constants;
import com.edexelroots.android.sensoriot.SensorIoTApp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

public class KDSWorker {

    private static final String SAMPLE_APPLICATION_NAME = "SensorIoT";
    // Initial position in the stream when the application starts up for the first time.
    private static final InitialPositionInStream SAMPLE_APPLICATION_INITIAL_POSITION_IN_STREAM = InitialPositionInStream.LATEST;
    // Position can be one of LATEST (most recent data) or TRIM_HORIZON (oldest available data)

    public static void execute() {
        String workerId = null;
        try {
            workerId = InetAddress.getLocalHost().getCanonicalHostName() + ":" + UUID.randomUUID();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            workerId = "unknown";
        }
        KinesisClientLibConfiguration kinesisClientLibConfiguration =
                new KinesisClientLibConfiguration(SAMPLE_APPLICATION_NAME,
                        Constants.Rekognition.streamProcessorName,
                        SensorIoTApp.getCredentialsProvider(),
                        workerId);
        kinesisClientLibConfiguration.withInitialPositionInStream(SAMPLE_APPLICATION_INITIAL_POSITION_IN_STREAM);

        IRecordProcessorFactory recordProcessorFactory = new KRecordProcessorFactory();
        Worker worker = new Worker
                .Builder()
                .recordProcessorFactory(recordProcessorFactory)
                .config(kinesisClientLibConfiguration).build();

        System.out.printf("Running %s to process stream %s as worker %s...\n",
                SAMPLE_APPLICATION_NAME,
                Constants.Rekognition.streamProcessorName,
                workerId);

        int exitCode = 0;
        try {
            worker.run();
        } catch (Throwable t) {
            System.err.println("Caught throwable while processing data.");
            t.printStackTrace();
            exitCode = 1;
        }
        System.exit(exitCode);
    }

}
