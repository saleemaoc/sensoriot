package com.edexelroots.android.sensoriot;

public class Constants {

    public static class Rekognition {

        public static String streamProcessorName="streamProcessorForCam";
        public static String kinesisVideoStreamArn="arn:aws:kinesisvideo:ap-northeast-1:791818220004:stream/liverekprototype/*";
        public static final String kdsStreamName = "AmazonRekognition_datastream";
        public static String kinesisDataStreamArn="arn:aws:kinesis:ap-northeast-1:791818220004:stream/liverekprototype_data";
        public static String roleArn="arn:aws:iam::791818220004:role/liverekprototypeKinesisRek";
//        public static String collectionId="aws:rekognition:ap-northeast-1:791818220004:collection/linkedin";
        public static String collectionId="linkedin";
        public static Float matchThreshold=88F;


/*        public static String streamProcessorName = "streamProcessorForCam";
        public static String kinesisVideoStreamArn = "arn:aws:kinesisvideo:ap-northeast-1:876659712960:stream/facekinesis/1542711912358";
        public static final String kdsStreamName = "AmazonRekognition_datastream";
        public static String kinesisDataStreamArn = "arn:aws:kinesis:ap-northeast-1:876659712960:stream/" + kdsStreamName;
        //public static String roleArn="arn:aws:iam::876659712960:role/facedetect_auth_MOBILEHUB_233006393";
        public static String roleArn = "arn:aws:iam::876659712960:role/Rekognition";
        public static String collectionId = "linkedin";
        public static Float matchThreshold = 88F;*/

    }

    static final int WIDTH_DEFAULT = 100;
    static final int WIDTH_MAX = 1000;
    static final int WIDTH_MIN = 0;

    static final int WEIGHT_DEFAULT = 400;
    static final int WEIGHT_MAX = 1000;
    static final int WEIGHT_MIN = 0;

    static final float ITALIC_DEFAULT = 0f;
    static final float ITALIC_MAX = 1f;
    static final float ITALIC_MIN = 0f;
}
