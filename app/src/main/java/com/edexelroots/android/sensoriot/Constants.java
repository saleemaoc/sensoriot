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
        public static String kinesisVideoStreamArn="arn:aws:kinesisvideo:ap-northeast-1:876659712960:stream/facekinesis/1541933661361";
        public static String kinesisDataStreamArn="arn:aws:kinesis:ap-northeast-1:876659712960:stream/facestream";
        public static String roleArn="arn:aws:iam::876659712960:role/facedetect_auth_MOBILEHUB_233006393";
        public static String collectionId="linkedin";
        public static Float matchThreshold=88F;

    }
}
