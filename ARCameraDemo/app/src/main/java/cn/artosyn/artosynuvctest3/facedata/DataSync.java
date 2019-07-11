package cn.artosyn.artosynuvctest3.facedata;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.SparseIntArray;

import com.moons.serial.serialdemo.DoorLockManager;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import cn.artosyn.artosynuvctest3.common.Common;
import cn.artosyn.artosynuvctest3.config.DemoConfig;
import cn.artosyn.artosynuvctest3.util.BitmapUtil;
import cn.artosyn.artosynuvctest3.util.CustomDraw;
import cn.artosyn.artosynuvctest3.util.ServerUploadUtil;
import cn.artosyn.aruvclib.ARAlgoSet;
import cn.artosyn.aruvclib.ARCommon;
import cn.artosyn.aruvclib.ARUtil;
import cn.artosyn.aruvclib.model.ARFaceResult;
import cn.artosyn.aruvclib.model.ARUserFace;
import cn.artosyn.aruvclib.util.ThreadPoolManager;
import cn.readsense.faceserver.RSFace;

public class DataSync {

    private static final String TAG = DataSync.class.getSimpleName();

    private static final int HANDLE_DOLIVE = 0,HANDLE_CAPTURE = 1;

    private final Queue<ARCommon.FrameData> frameDataCacheQueue = new ArrayBlockingQueue<ARCommon.FrameData>(20);

    public static final Map<Integer,ARUserFace> userFaces_ident = new LinkedHashMap<>();
    private static final Map<Integer,FaceLiveStatus> faceid_livestatus = new LinkedHashMap<>();
    private static final Map<Integer,Boolean> faceid_facehandle = new LinkedHashMap<>();
    private static final SparseIntArray faceid_facenotify = new SparseIntArray();

    private final ArrayBlockingQueue<FaceCaptureOriData> faceHandleQueue;
    private LiveDetectThread liveDetectThread;

    private int capW = -1,capH = -1;

    private final ARAlgoSet arAlgoSet;
    private boolean bAlgoInit;

    private HandlerThread featureFaceHandlerThread;
    private Handler featureFaceHandler;

    private Handler mainHandler;

    private final Object mLiveDetectLock = new Object();

    public DataSync(Context context){
        arAlgoSet = new ARAlgoSet(context,DemoConfig.DatabasePath);
        faceHandleQueue = new ArrayBlockingQueue<>(10);
        initAlgo();
    }

    private String initAlgo(){
        ARFaceResult arFaceResult = arAlgoSet.init();
        Log.i(TAG, String.format("arAlgoSet init\t%s",arFaceResult.msg));
        if(arFaceResult.code==0||arFaceResult.code==-111){
            bAlgoInit = true;
        }
        else{
            arAlgoSet.release();
            bAlgoInit = false;
        }
        return arFaceResult.msg;
    }

    public void init(int w,int h,Handler handler){
        if(!bAlgoInit){
            initAlgo();
        }
        capW = w;
        capH = h;
        mainHandler = handler;
        featureFaceHandlerThread = new HandlerThread("FeatureFaceHandlerThread");
        featureFaceHandlerThread.start();
        featureFaceHandler = new Handler(featureFaceHandlerThread.getLooper(),featureFaceCallBack);
        liveDetectThread = new LiveDetectThread();
        liveDetectThread.Start();
    }

    public void release(){
        frameDataCacheQueue.clear();
        if(featureFaceHandlerThread!=null) {
            featureFaceHandlerThread.quit();
            featureFaceHandlerThread.interrupt();
        }
        featureFaceHandlerThread = null;
        if(liveDetectThread!=null){
            liveDetectThread.Stop();
            liveDetectThread=null;
        }
        ThreadPoolManager.getInstance().shutdown();
    }

    public void  release2(){
        arAlgoSet.release();
    }

    public void addCacheFrame(ARCommon.FrameData frameData){
        if(!frameDataCacheQueue.offer(frameData)){
            removeOneCacheFrame();
            frameDataCacheQueue.offer(frameData);
        }
        //Log.i(TAG, String.format("frameDataCacheQueue size\t%d",frameDataCacheQueue.size()));
    }

    private void removeOneCacheFrame(){
        ARCommon.FrameData frameData1 = frameDataCacheQueue.poll();
        if(frameData1!=null) {
            frameData1.buff = null;
            ARUtil.safeReleaseBitmap(frameData1.decode_img);
        }
    }

    private void addIdentFace(int faceid,ARUserFace userFace){
        synchronized (userFaces_ident) {
            userFaces_ident.put(faceid, userFace);
            if (userFaces_ident.size() > 10) {
                Set set = userFaces_ident.entrySet();
                Iterator i = set.iterator();
                if (i.hasNext()) {
                    i.next();
                    i.remove();
                }
            }
        }
    }
    private void removeIdentFace(int faceid){
        userFaces_ident.remove(faceid);
    }
    private boolean hasIdentFace(int faceid){
        synchronized (userFaces_ident) {
            return userFaces_ident.containsKey(faceid);
        }
    }
    public static void clearIdentFace(){
        synchronized (userFaces_ident){
            userFaces_ident.clear();
        }
    }

    private void addLiveFace(int id, Boolean live, int timestamp,ARCommon.BoxFeatureData boxFeatureData){
        synchronized (faceid_livestatus) {
            FaceLiveStatus status = new FaceLiveStatus(live,boxFeatureData.boxRect.width(),timestamp,boxFeatureData.facePose);
            status.isDetecting = true;
            faceid_livestatus.put(id, status);
            if (faceid_livestatus.size() > 10) {
                Set set = faceid_livestatus.entrySet();
                Iterator i = set.iterator();
                if (i.hasNext()) {
                    i.next();
                    i.remove();
                }
            }
        }
    }

    private void updateLiveFace(int id,Boolean live){
        synchronized (faceid_livestatus) {
            FaceLiveStatus status = faceid_livestatus.get(id);
            if(status!=null){
                status.result = live;
                status.isDetecting = false;
            }
        }
    }

    public static Boolean isLiveFace(int id){
        FaceLiveStatus status = faceid_livestatus.get(id);
        if(status == null) return null;
        return status.result;
    }

    public static void clearLiveFaces(){
        synchronized (faceid_livestatus) {
            faceid_livestatus.clear();
        }
    }

    private void notifyFaceInfo(int faceid,ARUserFace userFace){
        if(faceid_facenotify.get(faceid,-1)<0) {
            faceid_facenotify.put(faceid,1);
            mainHandler.obtainMessage(1, userFace).sendToTarget();
        }
        else{
            mainHandler.obtainMessage(1, null).sendToTarget();
        }
    }

    public void handleFeatureFace(ARCommon.CustomData customData){
        featureFaceHandler.obtainMessage(0,customData).sendToTarget();
    }

    private final Handler.Callback featureFaceCallBack = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            ARCommon.CustomData customData = (ARCommon.CustomData)message.obj;

            ARCommon.BoxFeatureData boxFeatureData = customData.boxFeatureDataList.get(0);
            Log.d(TAG, String.format("get face with feature, faceid:%d", boxFeatureData.face_id));

            Common.SingleFaceWithFeature singleFaceWithFeature = new Common.SingleFaceWithFeature();
            singleFaceWithFeature.boxFeatureData = boxFeatureData;
            singleFaceWithFeature.ori_w = customData.width;
            singleFaceWithFeature.ori_h = customData.height;
            singleFaceWithFeature.frameindex = customData.index;

            boolean bMatch = false;
            if(DemoConfig.instance().faceMatchMode==0){
                bMatch = true;
            }
            else if(DemoConfig.instance().faceMatchMode==1){
                bMatch = !hasIdentFace(boxFeatureData.face_id);
            }
            ARUserFace userFace = null;
            if(bMatch) {
                userFace = GetRegisteredInfo(boxFeatureData.feature);
                if (userFace != null) {
                    //TODO:识别匹配成功
                    DoorLockManager.getInstance().openLock();//开锁
                    userFace.bUpdate = true;

                    boxFeatureData.match_cosdis_max = userFace.iSimilarity;
                    boxFeatureData.name = userFace.name;
                    singleFaceWithFeature.isRecognized = true;
                    userFace.isLive = DataSync.isLiveFace(boxFeatureData.face_id);

                    FaceDataUtil.addRecordUserFace(userFace);
                    notifyFaceInfo(boxFeatureData.face_id,userFace);
                    addIdentFace(singleFaceWithFeature.boxFeatureData.face_id,userFace);

                    if(userFace.isLive==null){
                        //TODO:匹配成功，无活体检测结果
                    }
                    else if(userFace.isLive){
                        //TODO:匹配成功，活体通过
                    }
                    else{
                        //TODO:匹配成功，活体失败
                    }
                }
                else{
                    //TODO:识别匹配失败
                    singleFaceWithFeature.isRecognized = false;
                    removeIdentFace(singleFaceWithFeature.boxFeatureData.face_id);
                }
            }
            else{
                singleFaceWithFeature.isRecognized = true;
            }
            onSignleFace(singleFaceWithFeature,userFace);

            return false;
        }
    };

    private void onSignleFace(Common.SingleFaceWithFeature singleFaceWithFeature,ARUserFace userFace){
        Iterator<ARCommon.FrameData> iterator = frameDataCacheQueue.iterator();
        while (iterator.hasNext()){
            ARCommon.FrameData frameData = iterator.next();
            if(frameData==null)
                break;
            if(singleFaceWithFeature.frameindex<frameData.frame_index){
                break;
            }
            else if(singleFaceWithFeature.frameindex==frameData.frame_index){
                Log.i(TAG, String.format("get one data & img\t%d %d",singleFaceWithFeature.frameindex,frameData.frame_index));

                if(singleFaceWithFeature.isRecognized) {
                    //已认识人脸处理
                    if (faceid_facehandle.containsKey(singleFaceWithFeature.boxFeatureData.face_id)) {
                        //保存过
                    } else {
                        faceid_facehandle.put(singleFaceWithFeature.boxFeatureData.face_id,false);
                        if (DemoConfig.instance().captureFace||DemoConfig.instance().uploadFace||DemoConfig.instance().isCloudMode) {
                            //保存第一次识别到的人脸
                            FaceCaptureOriData faceCaptureOriData = new FaceCaptureOriData(frameData.buff,singleFaceWithFeature,HANDLE_CAPTURE,userFace);
                            faceHandleQueue.offer(faceCaptureOriData);
                        }
                    }
                }
                else{
                    //陌生人脸处理
                }


                if (DemoConfig.instance().isLiveDetect) {
                    FaceLiveStatus status = faceid_livestatus.get(singleFaceWithFeature.boxFeatureData.face_id);
                    if(status!=null){
                        Log.d(TAG, String.format("----- 已存在的活体 faceid:%d blive:%b count:%d",
                                singleFaceWithFeature.boxFeatureData.face_id,
                                status.result,status.count));
                        if(status.isDetecting){
                            break;
                        }
                        int csize = singleFaceWithFeature.boxFeatureData.boxRect.width();
                        ARCommon.FacePose cfacePose = singleFaceWithFeature.boxFeatureData.facePose;
                        if (postureChange(status.size,csize,cfacePose, status.facePose)) {
                            Log.d(TAG, String.format("+++++ 变化帧活体检测 faceid:%d csize:%d oldsize:%d y:%d %d r:%d %d p:%d %d",
                                    singleFaceWithFeature.boxFeatureData.face_id,
                                    csize,status.size, cfacePose.yaw,status.facePose.yaw, cfacePose.roll,status.facePose.roll, cfacePose.pitch,status.facePose.pitch));
                            status.size = csize;
                            status.facePose = cfacePose;
                            status.isDetecting = true;
                            doLiveDetect(frameData.buff, singleFaceWithFeature);
                        }
                    }
                    else {
                        //未进行活体检测
                        Log.d(TAG,String.format(">>>>> 新帧活体检测 frameindex:%d faceid:%d size:%d y:%d r:%d p:%d",
                                singleFaceWithFeature.frameindex,singleFaceWithFeature.boxFeatureData.face_id,
                                singleFaceWithFeature.boxFeatureData.boxRect.width(),
                                singleFaceWithFeature.boxFeatureData.facePose.yaw,
                                singleFaceWithFeature.boxFeatureData.facePose.roll,
                                singleFaceWithFeature.boxFeatureData.facePose.pitch));
                        addLiveFace(singleFaceWithFeature.boxFeatureData.face_id,null,-1,singleFaceWithFeature.boxFeatureData);
                        doLiveDetect(frameData.buff,singleFaceWithFeature);
                    }
                }
                break;
            }
        }
    }

    private boolean postureChange(int s1,int s2,ARCommon.FacePose f1,ARCommon.FacePose f2){
        int s_delt = Math.abs(s1-s2);
        int y_delt = Math.abs(f1.yaw-f2.yaw);
        int p_delt = Math.abs(f1.pitch-f2.pitch);
        int r_delt = Math.abs(f1.roll-f2.roll);
        return s_delt>(s1/5)||y_delt>20||p_delt>20||r_delt>20;
    }

    private void doLiveDetect(byte[] buff,Common.SingleFaceWithFeature singleFaceWithFeature){
        FaceCaptureOriData faceCaptureOriData = new FaceCaptureOriData(buff, singleFaceWithFeature,HANDLE_DOLIVE,null);
        faceHandleQueue.offer(faceCaptureOriData);
    }


    private class FaceCaptureOriData{
        byte[] framebuff;
        Common.SingleFaceWithFeature singleFaceWithFeature;
        ARUserFace userFace;
        int iHandleType;
        FaceCaptureOriData(byte[] f,Common.SingleFaceWithFeature s,int type,ARUserFace face){
            framebuff = null;
            if(f!=null)
                framebuff = Arrays.copyOf(f,f.length);
            else{
                Log.e(TAG,"frame data is null");
            }
            singleFaceWithFeature = s;
            iHandleType = type;
            userFace = face;
        }
    }

    private class FaceLiveStatus{
        Boolean result;
        int size;
        long timestamp;
        int count;
        ARCommon.FacePose facePose;
        boolean isDetecting;
        FaceLiveStatus() {
            result = null;
            size = -1;
            timestamp = -1;
            count = 0;
            facePose = null;
            isDetecting = false;
        }
        FaceLiveStatus(Boolean r,int s,long t,ARCommon.FacePose pose){
            result = r;
            size = s;
            timestamp = t;
            count = 0;
            facePose = pose;
            isDetecting = false;
        }
        int addCount(){
            synchronized (this) {
                return ++count;
            }
        }
        void resetCount(){
            synchronized (this) {
                count = 0;
            }
        }
    }

    private class LiveDetectThread extends Thread{
        private Boolean bRun;
        BitmapFactory.Options options;
        LiveDetectThread()
        {
            this.setName("LiveDetectThread");
            bRun = false;
            options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inMutable = true;
        }

        void Start(){
            bRun = true;
            this.start();
        }
        void Stop(){
            bRun = false;
            try {
                this.interrupt();
                this.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while(bRun) {
                FaceCaptureOriData faceCaptureOriData = null;
                try {
                    faceCaptureOriData = faceHandleQueue.poll(200,TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    break;
                }
                if(faceCaptureOriData==null)
                    continue;
                if(faceCaptureOriData.iHandleType==HANDLE_DOLIVE) {
                    //活体检测
                    int faceid = faceCaptureOriData.singleFaceWithFeature.boxFeatureData.face_id;
                    if (!bAlgoInit) {
                        faceCaptureOriData.framebuff = null;
                        faceid_livestatus.remove(faceid);
                        continue;
                    }
                    int frameindex = faceCaptureOriData.singleFaceWithFeature.frameindex;
                    if (faceCaptureOriData.framebuff == null) {
                        faceid_livestatus.remove(faceid);
                        continue;
                    }
                    Bitmap bitmap = null;
                    try {
                        bitmap = BitmapFactory.decodeByteArray(faceCaptureOriData.framebuff, 0, faceCaptureOriData.framebuff.length, options);
                    } catch (Exception ex) {
                        faceid_livestatus.remove(faceid);
                        continue;
                    }

                    if (bitmap == null) {
                        faceid_livestatus.remove(faceid);
                        continue;
                    }
                    if (bitmap.getHeight() != capH || bitmap.getWidth() != capW || bitmap.getRowBytes() * bitmap.getHeight() != capH * capW * 4) {
                        faceid_livestatus.remove(faceid);
                        continue;
                    }
                    Rect realFaceRect = CustomDraw.rectMapping(faceCaptureOriData.singleFaceWithFeature.boxFeatureData.boxRect,
                            faceCaptureOriData.singleFaceWithFeature.ori_w, faceCaptureOriData.singleFaceWithFeature.ori_h,
                            capW, capH);
                    Boolean bLive = null;
                    synchronized (mLiveDetectLock) {
                        bLive = arAlgoSet.detectLiveness(bitmap, realFaceRect);
                    }
                    if (bLive == null) {
                        Log.d(TAG, String.format("XXXXX 活体调用接口失败 frameindex:%d faceid:%d", frameindex, faceid));
                        faceid_livestatus.remove(faceid);
                        continue;
                    }
                    Log.d(TAG, String.format("OOOOO 活体调用接口成功 frameindex:%d faceid:%d islive:%b", frameindex, faceid, bLive));

                    updateLiveFace(faceid, bLive);
                    //faceid_livestatus.remove(faceid);

                    ARUtil.safeReleaseBitmap(bitmap);
                    faceCaptureOriData.framebuff = null;
                }
                else{
                    if (faceCaptureOriData.framebuff == null) {
                        continue;
                    }
                    //抓拍保存
                    Bitmap bitmap = null;
                    try {
                        bitmap = BitmapFactory.decodeByteArray(faceCaptureOriData.framebuff, 0,faceCaptureOriData.framebuff.length);
                    } catch (Exception ex) {
                        continue;
                    }
                    if (bitmap == null) {
                        continue;
                    }
                    Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    Rect rect_face_ori = faceCaptureOriData.singleFaceWithFeature.boxFeatureData.boxRect;
                    Rect rect_face = new Rect();
                    rect_face.left = rect_face_ori.left * src.width() / faceCaptureOriData.singleFaceWithFeature.ori_w;
                    rect_face.right = rect_face_ori.right * src.width() / faceCaptureOriData.singleFaceWithFeature.ori_w;
                    rect_face.top = rect_face_ori.top * src.height() / faceCaptureOriData.singleFaceWithFeature.ori_h;
                    rect_face.bottom = rect_face_ori.bottom * src.height() / faceCaptureOriData.singleFaceWithFeature.ori_h;
                    //extend 1.6
//                    int rf_w = rect_face.width();
//                    int rf_h = rect_face.height();
//                    int rf_w_offset = ((int) (rf_w * 1.6) - rf_w) / 2;
//                    int rf_h_offset = ((int) (rf_h * 1.6) - rf_h) / 2;
//                    rect_face.inset(-rf_w_offset, -rf_h_offset);
                    if (!src.contains(rect_face))
                        continue;
                    try {
                        final Bitmap bitmap_face = Bitmap.createBitmap(bitmap, rect_face.left, rect_face.top,
                                rect_face.width(), rect_face.height(), null, false);
                        if(DemoConfig.instance().captureFace) {
                            File capture_dir = new File(DemoConfig.CapturePath);
                            String fileName = faceCaptureOriData.singleFaceWithFeature.frameindex + "-" + faceCaptureOriData.singleFaceWithFeature.boxFeatureData.face_id + ".jpg";
                            File file_save = new File(capture_dir, fileName);
                            ARUtil.SaveBitmap(bitmap_face, file_save);
                        }
                        if(DemoConfig.instance().uploadFace){
                            ServerUploadUtil.Upload_Data upload_data = new ServerUploadUtil.Upload_Data();
                            upload_data.device_position = DemoConfig.instance().dev_position;
                            upload_data.device_uuid = DemoConfig.instance().dev_uuid;
                            upload_data.face_name = faceCaptureOriData.singleFaceWithFeature.boxFeatureData.name;
                            upload_data.face_cos_distance = faceCaptureOriData.singleFaceWithFeature.boxFeatureData.match_cosdis_max;
                            upload_data.face_image = BitmapUtil.bitmapToBase64(bitmap_face);
                            upload_data.face_feature = faceCaptureOriData.singleFaceWithFeature.boxFeatureData.feature;
                            ServerUploadUtil.upload(upload_data);
                        }
                        if(DemoConfig.instance().isCloudMode){
                            String sbitmap = BitmapUtil.bitmapToBase64(bitmap_face);
                            if(faceCaptureOriData.userFace!=null) {
                                OrionHelper.instance().sendRecRecord(faceCaptureOriData.userFace.person_uuid, faceCaptureOriData.userFace.face_uuid, sbitmap);
                            }
                            else
                                OrionHelper.instance().sendStrangeRecord(sbitmap);
                        }
                        ARUtil.safeReleaseBitmap(bitmap_face);
                    } catch (Exception ex) {
                        Log.e("faceCaputreCallback", "error");
                        ex.printStackTrace();
                    }
                    ARUtil.safeReleaseBitmap(bitmap);
                    faceCaptureOriData.framebuff = null;
                }
            }
        }
    }

    private static ARUserFace GetRegisteredInfo(float[] feature) {
        if (feature.length != ARCommon.FACE_FEATURE_LENGHT || FaceDataUtil.userFaces_all.isEmpty())
            return null;
        float threshold = DemoConfig.instance().feature_threshold;
        if(DemoConfig.instance().isCloudMode){
            RSFace rsFace = OrionHelper.instance().identifyFeature(feature);
            if(rsFace==null)
                return null;
            //Log.w(TAG, String.format("get identiface person uuid:%s %f", rsFace.getPersonuuid(),rsFace.getConfidence()));
            if(rsFace.getConfidence()<(threshold*100))
                return null;
            synchronized (FaceDataUtil.userFaces_all) {
                for(Iterator<ARUserFace> iterator = FaceDataUtil.userFaces_all.iterator(); iterator.hasNext();){
                    ARUserFace userFace = iterator.next();
                    if(userFace.person_uuid.equals(rsFace.getPersonuuid())) {
                        userFace.face_uuid = rsFace.getFaceuuid();
                        userFace.iSimilarity = (int)(rsFace.getConfidence());
                        return userFace;
                    }
                }
            }
            return null;
        }

        float feature_flen = ARUtil.GetVectorLength(feature);
        if (feature_flen == 0)
            return null;
        float cv_max = Float.MIN_NORMAL;
        ARUserFace userFace_max = null;
        long time_start = System.currentTimeMillis();
        int count = 0;
        synchronized (FaceDataUtil.userFaces_all) {
            for(Iterator<ARUserFace> iterator = FaceDataUtil.userFaces_all.iterator(); iterator.hasNext();){
                ARUserFace userFace = iterator.next();
                if(userFace.feature.length!=ARCommon.FACE_FEATURE_LENGHT)
                    continue;
                if (userFace.feature_length == 0)
                    continue;
                float cv = ARUtil.calcCosValue_accel(feature, feature_flen, userFace.feature, userFace.feature_length, ARCommon.FACE_FEATURE_LENGHT);
                if (cv > cv_max) {
                    cv_max = cv;
                    userFace_max = userFace;
                    //userFace_max.match_cosdis_max = cv_max;
                }
                count++;
            }
        }
        long time_end = System.currentTimeMillis();
        Log.w(TAG, String.format("cosine similarity time:%d ms, count:%d", time_end -time_start,count));
        Log.w(TAG, String.format("*****************calcCosValue max:   %f %f  ", cv_max, threshold));
        if(userFace_max!=null&&cv_max>threshold)
        {
            userFace_max.iSimilarity = (int)(cv_max*100);
            return userFace_max;
        }
        return null;
    }
}
