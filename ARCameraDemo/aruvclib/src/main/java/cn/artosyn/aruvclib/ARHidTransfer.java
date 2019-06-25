package cn.artosyn.aruvclib;

import android.content.Context;

import java.lang.ref.WeakReference;

import cn.artosyn.artranslib.ARUsbTransfer;

public class ARHidTransfer{
    private static final String TAG = ARHidTransfer.class.getSimpleName();
    private WeakReference<Context> mWeakContext = null;

    private ARUsbTransfer arUsbTransfer;

    public interface StatusCallback{
        void onStatusChange(ARCommon.LOG_MSG_TYPE msg_type,String msg,int process);
    }
    private StatusCallback statusCallback = null;

    public void addCallback(StatusCallback callback){
        statusCallback = callback;
    }
    public void removeCallback(){
        statusCallback = null;
    }

    private void statusChange(ARCommon.LOG_MSG_TYPE msg_type,String msg,int process){
        if(statusCallback==null)
            return;
        statusCallback.onStatusChange(msg_type,msg,process);
    }

    public ARHidTransfer(Context ctx){
        mWeakContext = new WeakReference<Context>(ctx);
        arUsbTransfer = new ARUsbTransfer(mWeakContext.get());
        arUsbTransfer.addCallback(usbstatusCallback);
    }

    public void register(){
        arUsbTransfer.register();
    }

    public void unregister(){
        arUsbTransfer.unregister();
    }

    public static boolean hasArUvc(Context ctx){
        return ARUsbTransfer.hasArUvc(ctx);
    }

    public boolean Open(){
        return arUsbTransfer.Open();
    }

    public void Close(){
        arUsbTransfer.Close();
    }

    public void TransFirmware(String filename){
        arUsbTransfer.TransFirmware(filename);
    }
    public void TransFile(String filename){
        arUsbTransfer.TransFile(filename);
    }

    public boolean SendUCmd(String ucmd){
        return arUsbTransfer.SendUCmd(ucmd);
    }

    private ARUsbTransfer.StatusCallback usbstatusCallback = new ARUsbTransfer.StatusCallback() {
        @Override
        public void onStatusChange(ARUsbTransfer.LOG_MSG_TYPE log_msg_type, String s, int i) {
            ARCommon.LOG_MSG_TYPE msg_type = ARCommon.LOG_MSG_TYPE.values()[log_msg_type.ordinal()];
            statusChange(msg_type,s,i);
        }
    };
}
