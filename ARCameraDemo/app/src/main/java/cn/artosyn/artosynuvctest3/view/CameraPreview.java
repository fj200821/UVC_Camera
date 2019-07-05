package cn.artosyn.artosynuvctest3.view;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.moons.serial.serialdemo.DoorLockManager;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import cn.artosyn.artosynuvctest3.activity.MainActivity;
import cn.artosyn.artosynuvctest3.config.DemoConfig;
import cn.artosyn.artosynuvctest3.facedata.DataSync;
import cn.artosyn.artosynuvctest3.register.RegisterHelper;
import cn.artosyn.artosynuvctest3.util.CustomDraw;
import cn.artosyn.aruvclib.ARCameraManager;
import cn.artosyn.aruvclib.ARCommon;
import cn.artosyn.aruvclib.ARNativeHelper;
import cn.artosyn.aruvclib.model.ARUserFace;
//摄像机预览类
public class CameraPreview implements ARCameraManager.CameraDataCallback{

    private static final String TAG = CameraPreview.class.getSimpleName();

    private SurfaceView overlayView;   //基于SurfaceView
    private SurfaceView previewView;   //预览SurfaceView

    private boolean mSurfaceAvailable;   //surface是否可用
    private boolean mGLSurfaceAvailable;
    private boolean bOpen;   //摄像机打开标记
    private boolean bRegisterCam;   //摄像机注册标记

    private ARCameraManager arCameraManager;   //AR摄像机管理类
    private final ArrayBlockingQueue<ARCommon.FrameData> frameDataQueue;   //帧数据队列
    private final ArrayBlockingQueue<ARCommon.CustomData> customDataQueue;   //私有数据队列
    private final ArrayBlockingQueue<Bitmap> bitmapQueue;   //bitmap队列

    private DecodeThread decodeThread;   //解析图像的线程
    private DrawThread drawThread;
    private ViewThread viewThread;

    private int mCaptureWidth,mCaptureHeight,mOverlayWidth,mOverlayHeight,mPreviewWidth,mPreviewHeight;

    private RegisterHelper registerHelper = null;

    private Handler handler;

    private WeakReference<MainActivity> mainActivityWeakReference = null;   // 定义弱引用变量,弱引用允许Activity被垃圾收集器清理

    private final Object mFrameCountLock;
    private int mNoFaceFrameCount = 0;

    private DataSync dataSync;

    public CameraPreview(MainActivity activity,SurfaceView preview,SurfaceView overlay) {

        mainActivityWeakReference = new WeakReference<MainActivity>(activity);
        registerHelper = new RegisterHelper(mainActivityWeakReference.get());

        SurfaceCallback surfaceCallback = new SurfaceCallback();
        overlayView = overlay;
        overlayView.getHolder().addCallback(surfaceCallback);
        previewView = preview;
        previewView.getHolder().addCallback(surfaceCallback);

        previewView.getHolder().setFormat(PixelFormat.RGBA_8888);
        overlayView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        overlayView.setZOrderMediaOverlay(true);
        //previewView.setZOrderOnTop(true);

        arCameraManager = new ARCameraManager();   //实例化AR摄像机管理类
        arCameraManager.addCallback(this);   //添加摄像机数据回调接口
        frameDataQueue = new ArrayBlockingQueue<ARCommon.FrameData>(2);   //一个由数组结构组成的有界阻塞队列,图像帧数据
        customDataQueue = new ArrayBlockingQueue<>(2);   //一个由数组结构组成的有界阻塞队列,私有数据
        bitmapQueue = new ArrayBlockingQueue<>(2);   //一个由数组结构组成的有界阻塞队列,bitmap

        bOpen = false;   //打开标记
        bRegisterCam = false;   //摄像机注册标记
        //处理注册人脸和人脸识别的刷新
        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                int what = msg.what;
                if(what==0) {
                    Bitmap bitmap = (Bitmap) msg.obj;
                    registerHelper.init();
                    registerHelper.register(bitmap, mainActivityWeakReference.get());
                }
                else if(what==1) {
                    mainActivityWeakReference.get().onFaceRecordChange();
                    if(msg.obj!=null){
                        ARUserFace userFace = (ARUserFace)msg.obj;
                        String name = userFace.name;
                        String image = "";
                        if(DemoConfig.instance().isCloudMode)
                            image = userFace.imageUrl;
                        else
                            image = DemoConfig.FacePicPath +userFace.id+".jpg";
                        mainActivityWeakReference.get().showNotifyDialog(name,image);
                    }
                }
            }
        };

        dataSync = new DataSync(mainActivityWeakReference.get());
        mFrameCountLock = new Object();

        handler.postDelayed(new Runnable() {
            public void run() {
                autoOpen();
            }
        }, 2000);
    }

    public void registerFromCam(){
        if(!bOpen)
            return;
        bRegisterCam = true;
    }

    public int autoOpen(){
        int ret = -4;
        int[] idevs = ARNativeHelper.getDevs();
        Log.i(TAG, "auto open first device"+Arrays.toString(ARNativeHelper.getDevs()));
        if(idevs.length>0) {
            String devname = "/dev/video" + idevs[0];
            ret =  Open(devname);
        }
        Toast.makeText(mainActivityWeakReference.get(),"Auto Open "+ret,Toast.LENGTH_SHORT).show();
        return ret;
    }
    //打开摄像机设备
    public int Open(String devname) {
        if (bOpen) {
            Close();   //关闭摄像机
        }
        if (arCameraManager.Open(devname)) {
            bOpen = true;
            mCaptureWidth = arCameraManager.GetCapWidth();   //捕获摄像的宽度
            mCaptureHeight = arCameraManager.GetCapHeight();   //捕获摄像的高度
            dataSync.init(mCaptureWidth,mCaptureHeight,handler);
            if (arCameraManager.StartCapture()) {
                decodeThread = new DecodeThread();   //解析图像线程
                drawThread = new DrawThread();
                viewThread = new ViewThread();
                viewThread.setDestRect(mPreviewWidth,mPreviewHeight);
                decodeThread.Start();
                drawThread.Start();
                viewThread.Start();
                return 0;
            } else {
                return -2;
            }
        }
        return -1;
    }

    public void Close(){
        if(!bOpen)
            return;
        arCameraManager.StopCapture();
        arCameraManager.Close();
        if(decodeThread!=null)
            decodeThread.Stop();
        if(drawThread!=null)
            drawThread.Stop();
        if(viewThread!=null)
            viewThread.Stop();
        dataSync.release();
        bOpen = false;
    }

    public void destory(){
        Close();
        dataSync.release2();
    }

    @Override
    public void onFrameDataRecv(ARCommon.FrameData frameData) {
        if(!mSurfaceAvailable) return;
        synchronized (mFrameCountLock){
            mNoFaceFrameCount++;
            if(mNoFaceFrameCount>=10){
                CustomDraw.clearOverlay(overlayView);
                DataSync.clearIdentFace();
                mNoFaceFrameCount = 0;
            }
        }
        if (!frameDataQueue.offer(frameData)) {
            ARCommon.FrameData frameData1 = frameDataQueue.poll();
//            if(frameData1!=null)
//                frameData1.buff = null;
            frameDataQueue.offer(frameData);
        }
        dataSync.addCacheFrame(frameData);
    }
    //私有数据回调
    @Override
    public void onCustomDataRecv(ARCommon.CustomData customData) {
        if(!mSurfaceAvailable) return;
        if(DemoConfig.instance().isPortrait) {
            RectF crop_rect = CustomDraw.getCropRect(
                    mCaptureWidth, mCaptureHeight,
                    mOverlayWidth, mOverlayHeight);
            for(Iterator<ARCommon.BoxFeatureData> iterator = customData.boxFeatureDataList.iterator();iterator.hasNext();){
                ARCommon.BoxFeatureData boxFeatureData = iterator.next();
                Rect rect_face2 = CustomDraw.rectMapping2Crop(boxFeatureData.boxRect,
                        customData.width, customData.height,
                        mCaptureWidth, mCaptureHeight,
                        crop_rect);
                if (rect_face2==null) {
                    iterator.remove();
                    continue;
                }
                if(boxFeatureData.bHasFeature){
                    dataSync.handleFeatureFace(customData);
                    return;
                }
                boxFeatureData.boxRect = rect_face2;
            }
            customData.width = (int)crop_rect.width();
            customData.height = (int)crop_rect.height();
            if(customData.boxFeatureDataList.isEmpty())
                return;
        }
        else{
            if(customData.boxFeatureDataList.size()==1){
                if(customData.boxFeatureDataList.get(0).bHasFeature){
                    DoorLockManager.getInstance().openLock();//开锁
                    dataSync.handleFeatureFace(customData);
                    return;
                }
            }
        }

        //TODO:接收到人脸数据

        if (!customDataQueue.offer(customData)) {
            customDataQueue.poll();
            customDataQueue.offer(customData);
        }
    }
    //SurfaceHolder.Callback
    private class SurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mSurfaceAvailable = true;
            int id = getSurfaceId(holder);
            if (id < 0) {
                Log.w(TAG, "surfaceCreated UNKNOWN holder=" + holder);
            } else {
                Log.d(TAG, "surfaceCreated #" + id + " holder=" + holder);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surface) {
            mSurfaceAvailable = false;
            int id = getSurfaceId(surface);
            Log.d(TAG, "surfaceDestroyed #" + id + " holder=" + surface);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            int id = getSurfaceId(holder);
            Log.d(TAG, "surfaceChanged #" + id + " holder=" + holder);
            switch (id){
                case 1:
                    mPreviewHeight = height;
                    mPreviewWidth = width;
                    break;
                case 2:
                    mOverlayHeight = height;
                    mOverlayWidth = width;
                    break;
                default:
                    break;

            }
        }
    }

    private int getSurfaceId(SurfaceHolder holder) {
        if (holder.equals(previewView.getHolder())) {
            return 1;
        } else if (holder.equals(overlayView.getHolder())) {
            return 2;
        }  else {
            return -1;
        }
    }
    //解析图像线程
    private class DecodeThread extends Thread{
        Boolean bRun;

        BitmapFactory.Options options;

        DecodeThread(){
            options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inMutable = true;
            this.setName("DecodeThread");
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
        public void run(){
            while(bRun) {
                //long time_start = System.currentTimeMillis();
                ARCommon.FrameData frameData = null;
                try {
                    frameData = frameDataQueue.poll(5,TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    break;
                }
                if (null == frameData||frameData.buff==null)
                    continue;
                Log.d(TAG, String.format("------------------decodeBitmap\t%d", frameDataQueue.size()));
                try {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(frameData.buff, 0, frameData.size, options);
                    //frameData.buff = null;
                    if (bitmap != null) {
                        if (bRegisterCam) {
                            Bitmap bitmap1 = bitmap.copy(bitmap.getConfig(), true);
                            handler.obtainMessage(0, bitmap1).sendToTarget();
                            bRegisterCam = false;
                        }
                        //previewView.addFrame(bitmap);
                        viewThread.addBitmap(bitmap);
                    }
                }catch (Exception ex){
                    continue;
                }
                //long time_end = System.currentTimeMillis();
                //Log.d(TAG, String.format("decode time:%d", time_end - time_start));
            }
        }
    }

    private class ViewThread extends Thread{
        private Boolean bRun;
        Paint paintBitmap;
        Rect rect_dst = null;
        ViewThread()
        {
            bRun = false;
            paintBitmap = new Paint();
            //paintBitmap.setAntiAlias(true);
            paintBitmap.setDither(true);
            paintBitmap.setFilterBitmap(true);
            rect_dst = new Rect();
            this.setName("ViewThread");
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

        void setDestRect(int w,int h){
            rect_dst = new Rect(0,0,w,h);
        }

        void addBitmap(Bitmap bitmap){
            if(!bitmapQueue.offer(bitmap)){
                Bitmap bitmap1 = bitmapQueue.poll();
                if(bitmap1!=null&&!bitmap1.isRecycled()){
                    bitmap1.recycle();
                }
                bitmapQueue.offer(bitmap);
            }
        }

        @Override
        public void run() {
            while(bRun) {
                if(this.isInterrupted())
                    break;
                //long time_start = System.currentTimeMillis();
                Bitmap bitmap = null;
                try {
                    bitmap = bitmapQueue.poll(5,TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    break;
                }
                if (null == bitmap)
                    continue;
                Log.d(TAG, String.format("------------------draw bitmap\t%d", bitmapQueue.size()));
                if(DemoConfig.instance().isPortrait) {
                    int crop_x = 0,crop_y=0,crop_width,crop_heith = mCaptureHeight;
                    crop_width = mCaptureHeight * mPreviewWidth / mPreviewHeight;
                    crop_x = (mCaptureWidth - crop_width) / 2;
                    if(crop_x<0){
                        crop_width=mCaptureWidth;
                        crop_y = 0;
                        crop_x = 0;
                    }
                    try {
                        bitmap = Bitmap.createBitmap(bitmap, crop_x, crop_y, crop_width, crop_heith);
                    }
                    catch (IllegalArgumentException ex){
                        continue;
                    }

                }
                if(DemoConfig.instance().useNativeDraw){
                    SurfaceHolder a = previewView.getHolder();
                    Surface surface = previewView.getHolder().getSurface();
                    if(!surface.isValid()){
                        continue;
                    }
                    ARNativeHelper.drawBitmap(surface,bitmap);
                }
                else {
                    Canvas canvas = previewView.getHolder().lockCanvas();
                    if (canvas != null) {
                        Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                        canvas.drawBitmap(bitmap, src, rect_dst, paintBitmap);
                        //canvas.drawText(String.valueOf(bitmapData.index), drawDestRect.left, drawDestRect.top + 50, paint_text);
                        try {
                            previewView.getHolder().unlockCanvasAndPost(canvas);
                        } catch (Exception e) {
                            Log.e("SurfaceDraw", "can't unlock canvas");
                        }
                    }
                }
                if (!bitmap.isRecycled())
                    bitmap.recycle();
                //long time_end = System.currentTimeMillis();
                //Log.d(TAG, String.format("draw bitmap time:%d", time_end - time_start));
            }
        }
    }

    private class DrawThread extends Thread{
        private Boolean bRun;
        DrawThread()
        {
            bRun = false;
            this.setName("DrawThread");
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
                ARCommon.CustomData customData = null;
                try {
                    customData = customDataQueue.poll(5,TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    break;
                }
                if(customData==null)
                    continue;
                //long time_start = System.currentTimeMillis();
                Log.d(TAG, String.format("------------------customDataQueue\t%d", customDataQueue.size()));
                if(mSurfaceAvailable&&!customData.boxFeatureDataList.isEmpty()) {
                    synchronized (mFrameCountLock) {
                        mNoFaceFrameCount = 0;
                        CustomDraw.drawOverlay(customData, overlayView, mOverlayWidth, mOverlayHeight);
                    }
                }
                for(Iterator<ARCommon.BoxFeatureData> iterator = customData.boxFeatureDataList.iterator();iterator.hasNext();){
                    ARCommon.BoxFeatureData boxFeatureData = iterator.next();
                    boxFeatureData.feature = null;
                }
                //long time_end = System.currentTimeMillis();
                //Log.d(TAG, String.format("face draw time :%d", time_end -time_start));
            }
        }
    }

}
