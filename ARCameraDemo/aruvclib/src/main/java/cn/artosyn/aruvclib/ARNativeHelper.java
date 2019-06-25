package cn.artosyn.aruvclib;

import android.graphics.Bitmap;
import android.view.Surface;
import android.view.SurfaceView;

public class ARNativeHelper {
    public static final String TAG = ARNativeHelper.class.getSimpleName();

    long m_pDev;    //v4l2dev pointer

    public static class FrameData{
        public int size;
        public int frame_index;
        public int data_type;
        public FrameData(){
            this.size = 0;
            this.frame_index = 0;
            this.data_type = 0;
        }
    }

    // Used to load the library on application startup.
    static {
        System.loadLibrary("ARCamNativelib");
    }

    ARNativeHelper() {
        m_pDev = 0;
    }

    /**
     * native method
     */
    public native long openUVCDev(String dname);
    public native void closeUVCDev(long lp);
    public native int getCamWidth(long lp);
    public native int getCamHigh(long lp);
    public native int startCapture(long lp);
    public native byte[] getFrame(Object obj,long lp);
    public native void stopCapture(long lp);
    public static native int[] getDevs();
    public static native void drawBitmap(Surface surface, Bitmap bitmap);
}
