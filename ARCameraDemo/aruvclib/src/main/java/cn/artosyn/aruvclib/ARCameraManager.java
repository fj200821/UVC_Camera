package cn.artosyn.aruvclib;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;

public class ARCameraManager extends ARLog {

    private static final String TAG = ARCameraManager.class.getSimpleName();

    private final boolean IS_RS_ALGO = true;

    private boolean bCapture;
    private CaptureThread captureThread;
    private HandlerThread customDataHandlerThread;
    private Handler customDataHandler;

    private static boolean bOpened = false;
    private static String mDevNmae = "";

    private static float FEATURE_THRESHOLD = 0.47f;

    private ARNativeHelper arNativeHelper;

    static final ArrayList<ARCommon.RegisteredFaceData> RecordFaceIDList = new ArrayList<>();

    public interface CameraDataCallback{
        void onFrameDataRecv(ARCommon.FrameData frameData);
        void onCustomDataRecv(ARCommon.CustomData customData);
    }
    private CameraDataCallback cameraDataCallback = null;

    public void addCallback(CameraDataCallback callback){
        cameraDataCallback = callback;
    }
    public void removeCallback(){
        cameraDataCallback = null;
    }

    public ARCameraManager() {
        arNativeHelper = new ARNativeHelper();
        arNativeHelper.m_pDev = 0;
    }

    void SetFeatureThreshold(float v){
        FEATURE_THRESHOLD = v;
        Log.w(TAG, "SetFeatureThreshold: "+v);
    }
    float GetFeatureThreshold(){
        return FEATURE_THRESHOLD;
    }

    public boolean Open(String devName){
        try {
            Process sh = Runtime.getRuntime().exec("su", null,null);
            DataOutputStream os = new DataOutputStream(sh.getOutputStream());
            os.write(("chmod 666 /dev/video*\n").getBytes("ASCII"));
            os.flush();
            os.close();
            sh.waitFor();
        } catch (Exception e) {
            Log.i(TAG, e.getMessage());
            SendLog(ARCommon.LOG_MSG_TYPE.UVC_ERROR,e.getMessage(),0);
        }
        arNativeHelper.m_pDev = arNativeHelper.openUVCDev(devName);
        if(arNativeHelper.m_pDev!=0) {
            bOpened = true;
            mDevNmae = devName;
            return true;
        }
        return false;
    }
    public void Close(){
        arNativeHelper.closeUVCDev(arNativeHelper.m_pDev);
        bOpened = false;
    }

    public boolean Reconnect(){
        if(bOpened||mDevNmae.isEmpty())
            return false;
        return Open(mDevNmae);
    }

    public int GetCapWidth(){
        return arNativeHelper.getCamWidth(arNativeHelper.m_pDev);
    }
    public int GetCapHeight(){
        return arNativeHelper.getCamHigh(arNativeHelper.m_pDev);
    }

    public boolean StartCapture(){
        int ret = arNativeHelper.startCapture(arNativeHelper.m_pDev);
        if(ret == 0)
        {
            if(captureThread!=null&&captureThread.isAlive()){
                captureThread.interrupt();
            }
            captureThread = new CaptureThread();
            customDataHandlerThread = new HandlerThread("CustomDataHandlerThread");
            customDataHandlerThread.start();
            customDataHandler = new Handler(customDataHandlerThread.getLooper(),customDataCallback);
            bCapture = true;
            captureThread.start();
            return true;
        }
        else
        {
            SendLog(ARCommon.LOG_MSG_TYPE.UVC_ERROR,"Camera capture failed.",0);
            return false;
        }
    }
    public void StopCapture(){
        bCapture = false;
        if(captureThread!=null) {
            captureThread.interrupt();
            if (captureThread.isAlive()) {
                try {
                    captureThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if(customDataHandlerThread!=null) {
            customDataHandlerThread.quit();
            customDataHandlerThread.interrupt();
        }
        arNativeHelper.stopCapture(arNativeHelper.m_pDev);
    }

    private class CaptureThread extends Thread {
        // This is worker thread handler.
        public Handler workerThreadHandler;
        private ARNativeHelper.FrameData fdata;
        private byte[] retArray;

        public CaptureThread() {
            fdata = new ARNativeHelper.FrameData();
        }

        @Override
        public void run() {
            while(bCapture)
            {
                retArray = arNativeHelper.getFrame((Object)fdata,arNativeHelper.m_pDev);
                if(retArray==null)
                    continue;
                ARCommon.FrameData frameData = new ARCommon.FrameData();
                frameData.frame_index = fdata.frame_index;
                frameData.size = fdata.size;
                frameData.buff = retArray;
                switch (fdata.data_type)
                {
                    case 1:
                        Log.d(TAG, String.format("read video data"));
                        frameData.type = ARCommon.FRAME_TYPE.VIDEO_DATA;
                        if(cameraDataCallback!=null){
                            cameraDataCallback.onFrameDataRecv(frameData);
                        }
                        break;
                    case 2:
                        Log.d(TAG, String.format("read custom data"));
                        frameData.type = ARCommon.FRAME_TYPE.CUSTOM_DATA;
                        customDataHandler.obtainMessage(0,frameData).sendToTarget();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private static class DataPktHeader{
        public byte[] magic = new byte[]{(byte)0xFF,(byte)0x55,(byte)0xFF,(byte)0xAA,(byte)0x41,(byte)0x52,(byte)0x54,(byte)0x4F};
        public static final int LENGTH = 36;
        public byte[] magic_number = new byte[8];
        public int header_len;
        public int total_len;
        public int payload_len;
        public int fragment_num;
        public int fragment_index;
        public byte sub_protocol;
        public byte[] reserve = new byte[3];
        public long check_sum;
    }

    private static class SDataHeader{
        public static final int LENGTH = 40;
        public int protocol;
        public int headerlen;
        public int payloadlen;
        public int segmentnumb;
        public long frameindex;
        public long timestamp;
        public int width;
        public int height;
    }
    private static class FaceBox{
        public float score;
        public int x0;
        public int y0;
        public int x1;
        public int y1;
    }
    private static class LandMark{
        public float score;
        public float[] points = new float[10];
    }
    private static class Feature{
        public final int LENGTH = 1128+3072;
        public float[] gender = new float[2];
        public float[] age = new float[24];
        //public char[] data = new char[1024];
        public float[] data = new float[1024];
    }
    private static class Identity {
        public final int LENGTH = 4096;
        public char[] data = new char[4096];
    }
    private static class SItemHeader{
        public static final int LENGTH = 80;
        public int item_type;
        public int item_size;
        public int faceid;
        public int status;
        public FaceBox faceBox = new FaceBox();
        public LandMark landMark = new LandMark();
    }
    private static class SItemData1{
        public static final int LENGTH = 1128;
        public Feature feature = new Feature();
    }

    //tiam min custom data
    private static class YM_Segment_Header{
        int item_mask;
        int item_size;
        int status;
    }
    private static final int FACE_LANDMARK_NUM = 21;
    private static class YM_Segment_Payload{
        int     rsface_index;
        byte[]	snap_type=new byte[16];
        int     rsface_left;
        int     rsface_top;
        int     rsface_right;
        int     rsface_bottom;
        float	rsface_blur_prob;
        float	rsface_front_prob;
        float   pose_roll;
        float   pose_yaw;
        float   pose_pitch;
        float[] face_landmark = new float[FACE_LANDMARK_NUM*2];
        float[] feature = new float[ARCommon.FACE_FEATURE_LENGHT];
    }
    private static class MY_Segment_data {
        static final int LENGTH = 100;
        YM_Segment_Header header = new YM_Segment_Header();
        YM_Segment_Payload payload = new YM_Segment_Payload();
    }
    private static class MY_Custom_Header {
        static final int LENGTH = 40;
        int	protocol;
        int headerlen;
        int payloadlen;
        int segmentnum;
        long frameindex;
        long timestamp;
        int width;
        int height;
    }

    private final Handler.Callback customDataCallback = new Handler.Callback() {
        private DataPktHeader GetPktHeader(byte[] harray){
            DataPktHeader header = new DataPktHeader();
            System.arraycopy(harray,0,header.magic_number,0,header.magic_number.length);
            header.header_len = ARUtil.bytes2int(harray,8);
            header.total_len = ARUtil.bytes2int(harray,12);
            header.payload_len = ARUtil.bytes2int(harray,16);
            header.fragment_num = ARUtil.bytes2int(harray,20);
            header.fragment_index = ARUtil.bytes2int(harray,24);
            header.sub_protocol = harray[28];
            header.check_sum = ARUtil.intbytes2long(harray,32);
            return header;
        }
        private SDataHeader GetSDataHeader(byte[] parray){
            SDataHeader header = new SDataHeader();
            header.protocol = ARUtil.bytes2int(parray,0);
            header.headerlen = ARUtil.bytes2int(parray,4);
            header.payloadlen = ARUtil.bytes2int(parray,8);
            header.segmentnumb = ARUtil.bytes2int(parray,12);
            header.frameindex = ARUtil.longbytes2long(parray,16);
            header.timestamp = ARUtil.longbytes2long(parray,24);
            header.width = ARUtil.bytes2int(parray,32);
            header.height = ARUtil.bytes2int(parray,36);
            return header;
        }
        private SItemHeader GetItemHeader(byte[] parray,int offset){
            SItemHeader header = new SItemHeader();
            header.item_type = ARUtil.bytes2int(parray,offset);
            header.item_size = ARUtil.bytes2int(parray,offset+4);
            header.faceid = ARUtil.bytes2int(parray,offset+8);
            header.status = ARUtil.bytes2int(parray,offset+12);
            header.faceBox.score = ARUtil.bytes2float(parray,offset+16);
            header.faceBox.x0 = ARUtil.bytes2int(parray,offset+20);
            header.faceBox.y0 = ARUtil.bytes2int(parray,offset+24);
            header.faceBox.x1 = ARUtil.bytes2int(parray,offset+28);
            header.faceBox.y1 = ARUtil.bytes2int(parray,offset+32);
            header.landMark.score = ARUtil.bytes2float(parray,offset+36);
            for(int i=0;i<10;i++){
                header.landMark.points[i] = ARUtil.bytes2float(parray,offset+40+4*i);
            }
            return header;
        }
        private SItemData1 GetItemData1(byte[] parray,int offset){
            SItemData1 data1 = new SItemData1();
            data1.feature.gender[0] = ARUtil.bytes2float(parray,offset);
            data1.feature.gender[1] = ARUtil.bytes2float(parray,offset+4);
            for(int i=0;i<24;i++) {
                data1.feature.age[i] = ARUtil.bytes2float(parray, offset + 8 + i * 4);
            }
            for(int i=0;i<ARCommon.FACE_FEATURE_LENGHT;i++){
                data1.feature.data[i] = ARUtil.bytes2float(parray, offset + 104 + i * 4);
            }
            return data1;
        }
        private MY_Custom_Header YMGetCustomHeader(byte[] parray, int offset){
            if(offset+MY_Custom_Header.LENGTH>parray.length)
                return null;
            MY_Custom_Header header = new MY_Custom_Header();
            header.protocol = ARUtil.bytes2int(parray,offset);
            header.headerlen = ARUtil.bytes2int(parray,offset+4);
            header.payloadlen = ARUtil.bytes2int(parray,offset+8);
            header.segmentnum = ARUtil.bytes2int(parray,offset+12);
            header.frameindex = ARUtil.longbytes2long(parray,offset+16);
            header.timestamp = ARUtil.longbytes2long(parray,offset+24);
            header.width = ARUtil.bytes2int(parray,offset+32);
            header.height = ARUtil.bytes2int(parray,offset+36);
            return header;
        }
        private MY_Segment_data YMGetFaceSegmentData(byte[] parray, int offset){
//            if(offset+YM_Segment_data.LENGTH>parray.length)
//                return null;
            MY_Segment_data segment_data = new MY_Segment_data();
            try {
                segment_data.header.item_mask = ARUtil.bytes2int(parray, offset);
                segment_data.header.item_size = ARUtil.bytes2int(parray, offset + 4);
                segment_data.header.status = ARUtil.bytes2int(parray, offset + 8);
                if (segment_data.header.item_size > parray.length - offset) {
                    Log.e("CustomDataError", "Wrong array length");
                    return null;
                }
                if (segment_data.header.item_mask >= 1) {
                    segment_data.payload.rsface_index = ARUtil.bytes2int(parray, offset + 12);
                    System.arraycopy(parray, offset + 16, segment_data.payload.snap_type, 0, segment_data.payload.snap_type.length);
                    segment_data.payload.rsface_left = ARUtil.bytes2int(parray, offset + 32);
                    segment_data.payload.rsface_top = ARUtil.bytes2int(parray, offset + 36);
                    segment_data.payload.rsface_right = ARUtil.bytes2int(parray, offset + 40);
                    segment_data.payload.rsface_bottom = ARUtil.bytes2int(parray, offset + 44);
                    segment_data.payload.rsface_blur_prob = ARUtil.bytes2float(parray, offset +48);
                    segment_data.payload.pose_roll = ARUtil.bytes2float(parray, offset +52);
                    segment_data.payload.pose_yaw = ARUtil.bytes2float(parray, offset +56);
                    segment_data.payload.pose_pitch = ARUtil.bytes2float(parray, offset +60);
                }
                if (segment_data.header.item_mask >= 4) {
                    for (int i = 0; i < ARCommon.FACE_FEATURE_LENGHT; i++) {
                        segment_data.payload.feature[i] = ARUtil.bytes2float(parray, offset + 64 + i * 4);
                    }
                }
            }
            catch (Exception e){
                Log.e("CustomDataError", "Exception");
                e.printStackTrace();
                return null;
            }
            return  segment_data;
        }

        @Override
        public boolean handleMessage(Message message) {
            ARCommon.FrameData frameData = (ARCommon.FrameData)message.obj;
            ARCommon.CustomData customData = new ARCommon.CustomData();

            byte[] harray = new byte[DataPktHeader.LENGTH];
            System.arraycopy(frameData.buff, 0, harray, 0, DataPktHeader.LENGTH);
            DataPktHeader pkt_header = GetPktHeader(harray);
            int payload_size = frameData.size - DataPktHeader.LENGTH;
            long cal_checksum = ARUtil.checkSum2long(frameData.buff, payload_size, DataPktHeader.LENGTH);
//            Log.i(TAG, String.format("sdata size:%d cal plen:%d real plen:%d cal sum:%d real sum:%d",
//                    frameData.size,payload_size,pkt_header.payload_len,cal_checksum,pkt_header.check_sum));
            if (cal_checksum != pkt_header.check_sum){
                Log.i(TAG, "check sum error");
                frameData.buff = null;
                return false;
            }

            String stemp = "";

            if(IS_RS_ALGO) {
                MY_Custom_Header header = YMGetCustomHeader(frameData.buff, DataPktHeader.LENGTH);
                if (header == null) {
                    Log.i(TAG, "data length for header error");
                    frameData.buff = null;
                    return false;
                }
                customData.index = (int) header.frameindex;
                customData.width = header.width;
                customData.height = header.height;
                int sdata_offset = DataPktHeader.LENGTH + MY_Custom_Header.LENGTH;
//                stemp = stemp.concat( String.format(Locale.getDefault(),"frameindex:%d segmentnum:%d timestamp:%d width:%d height:%d\n"
//                    ,header.frameindex,header.segmentnum,header.timestamp,header.width,header.height));
                for (int i = 0; i < header.segmentnum; i++) {
                    MY_Segment_data segment_data = YMGetFaceSegmentData(frameData.buff, sdata_offset);
                    if (segment_data == null)
                        break;
//                    stemp= stemp.concat(String.format(Locale.getDefault(),"face id:%d\n"
//                            ,segment_data.payload.rsface_index));
                    ARCommon.BoxFeatureData boxFeatureData = new ARCommon.BoxFeatureData();
                    if(segment_data.header.item_mask>=1) {
                        boxFeatureData.face_id = segment_data.payload.rsface_index;
                        boxFeatureData.boxRect.left = segment_data.payload.rsface_left;
                        boxFeatureData.boxRect.right = segment_data.payload.rsface_right;
                        boxFeatureData.boxRect.top = segment_data.payload.rsface_top;
                        boxFeatureData.boxRect.bottom = segment_data.payload.rsface_bottom;
                        boxFeatureData.facePose.yaw = (int) segment_data.payload.pose_yaw;
                        boxFeatureData.facePose.pitch = (int) segment_data.payload.pose_pitch;
                        boxFeatureData.facePose.roll = (int) segment_data.payload.pose_roll;
                        //Log.e(TAG, "blur "+segment_data.payload.rsface_blur_prob+" pose "+segment_data.payload.pose_yaw+" " + segment_data.payload.pose_pitch+" "+segment_data.payload.pose_roll);
                    }
                    if(segment_data.header.item_mask>=4){
                        System.arraycopy(segment_data.payload.feature, 0, boxFeatureData.feature, 0, ARCommon.FACE_FEATURE_LENGHT);
                        boxFeatureData.bHasFeature = true;
                    }
                    customData.boxFeatureDataList.add(boxFeatureData);
                    sdata_offset += segment_data.header.item_size;
                }
            }
            else {

                int poffset = 0;
                byte[] parray = new byte[pkt_header.payload_len];
                System.arraycopy(frameData.buff, DataPktHeader.LENGTH, parray, 0, pkt_header.payload_len);
                SDataHeader sdata_header = GetSDataHeader(parray);
//            stemp = stemp.concat( String.format(Locale.getDefault(),"segment:%d frameindex:%d timestamp:%d width:%d height:%d\n"
//                    ,sdata_header.segmentnumb,sdata_header.frameindex,sdata_header.timestamp,sdata_header.width,sdata_header.height));
                poffset = SDataHeader.LENGTH;
                customData.index = (int) sdata_header.frameindex;
                customData.width = sdata_header.width;
                customData.height = sdata_header.height;

                for (int i = 0; i < sdata_header.segmentnumb; i++) {
                    ARCommon.BoxFeatureData boxFeatureData = new ARCommon.BoxFeatureData();

                    SItemHeader itemHeader = GetItemHeader(parray, poffset);

                    //Log.i(TAG, String.format("item_type:%d item_size:%d",itemHeader.item_type,itemHeader.item_size));
//                stemp = stemp.concat(String.format(Locale.getDefault(),"   faceid:%d score:%f x0:%d y0:%d x1:%d y1:%d\n"
//                        ,itemHeader.faceid,itemHeader.faceBox.score,
//                        itemHeader.faceBox.x0,itemHeader.faceBox.y0
//                        ,itemHeader.faceBox.x1,itemHeader.faceBox.y1));

                    boxFeatureData.boxRect.left = itemHeader.faceBox.x0;
                    boxFeatureData.boxRect.right = itemHeader.faceBox.x1;
                    boxFeatureData.boxRect.top = itemHeader.faceBox.y0;
                    boxFeatureData.boxRect.bottom = itemHeader.faceBox.y1;

                    for (int j = 0; j < 5; j++) {
                        boxFeatureData.landMark.points[j].x = (int) itemHeader.landMark.points[j * 2];
                        boxFeatureData.landMark.points[j].y = (int) itemHeader.landMark.points[j * 2 + 1];
                    }

                    if (itemHeader.item_type == 1) {
                        SItemData1 data1 = GetItemData1(parray, poffset + SItemHeader.LENGTH);
                        if (data1.feature.gender[0] != 0 || data1.feature.gender[1] != 0) {
                            boxFeatureData.gender = data1.feature.gender[0] > data1.feature.gender[1] ? 0 : 1;
                        }
                        if (data1.feature.age[0] != 0)
                            boxFeatureData.age = (int) data1.feature.age[0];

                        boxFeatureData.facePose.yaw = (int) data1.feature.age[1];
                        boxFeatureData.facePose.pitch = (int) data1.feature.age[2];
                        boxFeatureData.facePose.roll = (int) data1.feature.age[3];

                        //Log.w(TAG,String.format("pose yaw:%d pitch:%d roll:%d ",boxFeatureData.facePose.yaw,boxFeatureData.facePose.pitch,boxFeatureData.facePose.roll));

                        System.arraycopy(data1.feature.data, 0, boxFeatureData.feature, 0, ARCommon.FACE_FEATURE_LENGHT);

                        ARCommon.RegisteredFaceData registeredFaceData = GetRegisteredInfo(boxFeatureData.feature);
                        if (registeredFaceData != null) {
                            boxFeatureData.match_cosdis_max = registeredFaceData.match_cosdis_max;
                            boxFeatureData.match_id = registeredFaceData.id;
                        }
                        if (registeredFaceData != null && registeredFaceData.match_cosdis_max > FEATURE_THRESHOLD) {
                            boxFeatureData.name = registeredFaceData.name;
                            boolean bfind = false;
                            Iterator<ARCommon.RegisteredFaceData> iterator = RecordFaceIDList.iterator();
                            while (iterator.hasNext()) {
                                ARCommon.RegisteredFaceData registeredFaceData1 = iterator.next();
                                if (registeredFaceData1.id == registeredFaceData.id) {
                                    registeredFaceData1.record_time = new Date();
                                    registeredFaceData1.bUpdate = true;
                                    bfind = true;
                                    break;
                                }
                            }
                            if (!bfind) {
                                registeredFaceData.bUpdate = true;
                                registeredFaceData.record_time = new Date();
                                RecordFaceIDList.add(0, registeredFaceData);
                                while (RecordFaceIDList.size() > 8) {
                                    ARCommon.RegisteredFaceData old_data = Collections.min(RecordFaceIDList, new RegedDataComparator());
                                    RecordFaceIDList.remove(old_data);
                                }
                            }
                            Collections.sort(RecordFaceIDList, new RegedDataComparator2());
                            SenLogByHandle(ARCommon.LOG_MSG_TYPE.FACERECORD_UPDATE, null);
                        }
                    }
                    boxFeatureData.face_id = itemHeader.faceid;

                    customData.boxFeatureDataList.add(boxFeatureData);

                    poffset += itemHeader.item_size;
                }
            }
            //Log.i(TAG, stemp);

            if(cameraDataCallback!=null){
                cameraDataCallback.onCustomDataRecv(customData);
            }

            frameData.buff = null;

            return true;
        }
    };

    class RegedDataComparator implements Comparator<ARCommon.RegisteredFaceData> {
        @Override
        public int compare(ARCommon.RegisteredFaceData s1, ARCommon.RegisteredFaceData s2) {
            return s1.record_time.compareTo(s2.record_time);
        }
    }

    class RegedDataComparator2 implements Comparator<ARCommon.RegisteredFaceData> {
        @Override
        public int compare(ARCommon.RegisteredFaceData s1, ARCommon.RegisteredFaceData s2) {
            return s2.record_time.compareTo(s1.record_time);
        }
    }

    static ARCommon.RegisteredFaceData GetRegisteredInfo(float[] feature) {
//        if (feature.length != ARCommon.FACE_FEATURE_LENGHT || ARFaceRegister.RegedFaceIDList.isEmpty())
//            return null;
//        float feature_flen = ARUtil.GetVectorLength(feature);
//        if (feature_flen == 0)
//            return null;
//        float cv_max = Float.MIN_NORMAL;
//        ARCommon.RegisteredFaceData registeredFaceData_max = null;
//        long time_start = System.currentTimeMillis();
//        int count = 0;
//        synchronized (ARFaceRegister.RegedFaceIDList) {
//            for(Iterator<ARCommon.RegisteredFaceData> iterator = ARFaceRegister.RegedFaceIDList.iterator();iterator.hasNext();){
//                ARCommon.RegisteredFaceData registeredFaceData = iterator.next();
//                if (registeredFaceData.feature_length == 0)
//                    continue;
//                float cv = calcCosValue_accel(feature, feature_flen, registeredFaceData.feature, registeredFaceData.feature_length, ARCommon.FACE_FEATURE_LENGHT);
//                if (cv > cv_max) {
//                    cv_max = cv;
//                    registeredFaceData_max = registeredFaceData;
//                    registeredFaceData_max.match_cosdis_max = cv_max;
//                }
//                count++;
//            }
//        }
//        long time_end = System.currentTimeMillis();
//        Log.w(TAG, String.format("cosine similarity time:%d ms, count:%d", time_end -time_start,count));
//        Log.w(TAG, String.format("*****************calcCosValue max:   %f %f", cv_max, FEATURE_THRESHOLD));
////        if (cv_max > FEATURE_THRESHOLD&&registeredFaceData_max!=null) {
////            Log.w(TAG, String.format("*****************return name of max value:   %s", registeredFaceData_max.name));
////            return registeredFaceData_max;
////        }
//        if(registeredFaceData_max!=null)
//        {
//            return registeredFaceData_max;
//        }
        return null;
    }

    private static float calcCosValue(float[] v1,float[] v2,int length,float threshold)
    {
        float top = 0,bottom1=0,bottom2 =0;
        for(int i =0; i < length; i++)
        {
            top+=v1[i]*v2[i];
            bottom1 += Math.pow(v1[i],2);
            bottom2 += Math.pow(v2[i],2);
        }
        if(bottom1 == 0 || bottom2 == 0)
            return Float.MIN_NORMAL;
        float cos_value = top / (float)(Math.sqrt(bottom1)*Math.sqrt(bottom2));
        return cos_value;
    }

    private static float calcCosValue_accel(float[] v1,float v1_len,float[] v2,float v2_len,int length){
        float top = 0;
        for(int i =0; i < length; i++)
        {
            top+=v1[i]*v2[i];
        }
        if(v1_len == 0 || v2_len == 0)
            return Float.MIN_NORMAL;
        return top / (v1_len*v2_len);
    }

}
