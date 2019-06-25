package cn.artosyn.aruvclib;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;

//import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.Date;

public class ARCommon {

//    public static final int MSG_CUSTOMER_DATAREV = 1;
//    public static final int HID_TRANS_STATUS = 2;
//    public static final int HID_TRANS_PROGRESS = 3;
//    public static final int HID_TRANS_ERROR = 4;
//    public static final int UVC_STATUS = 5;
//    public static final int UVC_ERROR = 6;

    public static final int FACE_FEATURE_LENGHT = 512;
    public static float FEATURE_THRESHOLD = 0.40f;

    public enum LOG_MSG_TYPE {
        UVC_STATUS,
        UVC_ERROR,
        HID_TRANS_STATUS,
        HID_TRANS_PROGRESS,
        HID_TRANS_ERROR,
        HID_UPGRADE_STATUS,
        HID_UCMD_RET,
        FACERECORD_UPDATE,
        FACEREGED_UPDATE
    }

    enum FRAME_TYPE{
        UNKNOWN,
        VIDEO_DATA,
        CUSTOM_DATA
    }

    public static class FrameData{
        public byte[] buff;
        public int size;
        public int frame_index;
        public FRAME_TYPE type;
        public Bitmap decode_img;
        public FrameData(){
            buff = new byte[]{};
            size = 0;
            frame_index = 0;
            type = FRAME_TYPE.UNKNOWN;
            decode_img = null;
        }
        public FrameData(byte[] buf,int size,int findex,FRAME_TYPE type){
            this.buff = buf.clone();
            this.size = size;
            this.frame_index = findex;
            type = type;
        }
    }

    public static class FacePose{
        public int yaw;
        public int pitch;
        public int roll;
    }
    static class LandMark{
        Point[] points;
        LandMark(){
            points = new Point[5];
            for (int i=0;i<points.length;i++) {
                points[i] = new Point();
            }
        }
    }

    public static class BoxFeatureData{
        public Rect boxRect;
        public int gender;
        public int age;
        public int face_id;
        public boolean bHasFeature;
        public float[] feature;
        public String name;
        public FacePose facePose;
        public LandMark landMark;
        public float match_cosdis_max;
        public long match_id;
        public Boolean isLive;
        public BoxFeatureData(){
            boxRect = new Rect();
            gender = -1;
            age = -1;
            face_id = -1;
            feature = new float[FACE_FEATURE_LENGHT];
            name = "";
            facePose = new FacePose();
            landMark = new LandMark();
            match_cosdis_max = Float.MIN_NORMAL;
            match_id = -1;
            bHasFeature = false;
            isLive = null;
        }
    }
    public static class CustomData{
        public int index;
        public int width;
        public int height;
        public ArrayList<BoxFeatureData> boxFeatureDataList;
        public CustomData(){
            index = 0;
            width = 0;
            height = 0;
            boxFeatureDataList = new ArrayList<>();
        }
    }
    static class FdFaceData{
        Rect faceBox;
        Point[] points;
        float[] feature;
        Bitmap img;
        int age;
        int gender;
        FdFaceData(){
            faceBox = new Rect();
            points = new Point[5];
            feature = new float[FACE_FEATURE_LENGHT];
            img = null;
            age = -1;
            gender = -1;
        }
    }

    static class RegisterFaceInfo{
        int gender;
        int age;
        FacePose facePose;
        Bitmap bmp;
        RegisterFaceInfo(){
            facePose = new FacePose();
            bmp = null;
        }
    }

    static class AddData_Server{
        //@Expose
        int index;
        //@Expose
        String name = "";
        //@Expose
        int gender;
        //@Expose
        int age;
    }
    static class RegisterFaceInfo_Server{
        int index;
        String name = "";
        int gender;
        int age;
        String imgTmpFileName = "";
        Bitmap bmp;
        RegisterFaceInfo_Server(){
            bmp = null;
        }
    }

    static class RegisteredFaceData {
        //@Expose
        long id;
        //@Expose
        String name;
        //@Expose
        Date   record_time;
        boolean bUpdate;
        //@Expose
        int gender;
        //@Expose
        int age;
        float[] feature;
        float feature_length;
        float match_cosdis_max;
        RegisteredFaceData(){
            id = 0;
            name = "";
            feature = new float[FACE_FEATURE_LENGHT];
            record_time = new Date();
            bUpdate = false;
            feature_length = 0;
        }
    }

}
