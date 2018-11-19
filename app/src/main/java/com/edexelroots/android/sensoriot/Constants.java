package com.edexelroots.android.sensoriot;

public class Constants {

    public static class Rekognition {
/*
        public static String streamProcessorName="streamProcessorForCam";
        public static String kinesisVideoStreamArn="arn:aws:kinesisvideo:ap-northeast-1:791818220004:stream/liverekprototype/*";
        public static String kinesisDataStreamArn="arn:aws:kinesis:ap-northeast-1:791818220004:stream/liverekprototype_data";
        public static String roleArn="arn:aws:iam::791818220004:role/liverekprototypeKinesisRek";
//        public static String collectionId="aws:rekognition:ap-northeast-1:791818220004:collection/linkedin";
        public static String collectionId="linkedin";
        public static Float matchThreshold=88F;
*/

        public static String streamProcessorName="streamProcessorForCam";
        public static String kinesisVideoStreamArn="arn:aws:kinesisvideo:ap-northeast-1:876659712960:stream/facekinesis/1542627966220";
        public static final String kdsStreamName = "AmazonRekognition_datastream";
        public static String kinesisDataStreamArn="arn:aws:kinesis:ap-northeast-1:876659712960:stream/" + kdsStreamName;
        //public static String roleArn="arn:aws:iam::876659712960:role/facedetect_auth_MOBILEHUB_233006393";
        public static String roleArn="arn:aws:iam::876659712960:role/Rekognition";
        public static String collectionId="linkedin";
        public static Float matchThreshold=88F;

    }
}
