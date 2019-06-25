package cn.artosyn.aruvclib;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class ARLog {

    public interface ARLogListener {
        void onARLogRecv(ARCommon.LOG_MSG_TYPE msg_type,String slog,int pro);
    }

    private ARLogListener logListener;

    private Handler mainThreadHandler;

    public void setLogListener(ARLogListener arLogListener) {
        this.logListener = arLogListener;
    }

    private static ARLog instance;

    public static ARLog getInstance() {
        if (instance == null) {
            instance = new ARLog();
        }
        return instance;
    }

    protected ARLog() {
        //thread message
        mainThreadHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == ARCommon.LOG_MSG_TYPE.HID_TRANS_STATUS.ordinal())
                {
                    String str = (String)msg.obj;
                    SendLog(ARCommon.LOG_MSG_TYPE.HID_TRANS_STATUS,str,0);
                }else if(msg.what == ARCommon.LOG_MSG_TYPE.HID_TRANS_PROGRESS.ordinal())
                {
                    int p = (int)msg.obj;
                    SendLog(ARCommon.LOG_MSG_TYPE.HID_TRANS_PROGRESS,"",p);
                }
                else if(msg.what == ARCommon.LOG_MSG_TYPE.HID_UPGRADE_STATUS.ordinal()){
                    String str = (String)msg.obj;
                    SendLog(ARCommon.LOG_MSG_TYPE.HID_UPGRADE_STATUS,str,0);
                }
                else if(msg.what == ARCommon.LOG_MSG_TYPE.HID_UCMD_RET.ordinal()){
                    String str = (String)msg.obj;
                    SendLog(ARCommon.LOG_MSG_TYPE.HID_UCMD_RET,str,0);
                }
                else if(msg.what == ARCommon.LOG_MSG_TYPE.FACERECORD_UPDATE.ordinal()){
                    SendLog(ARCommon.LOG_MSG_TYPE.FACERECORD_UPDATE,"",0);
                }
                else if(msg.what == ARCommon.LOG_MSG_TYPE.FACEREGED_UPDATE.ordinal()){
                    SendLog(ARCommon.LOG_MSG_TYPE.FACEREGED_UPDATE,"",0);
                }

            }
        };
    }

    protected void SendLog(ARCommon.LOG_MSG_TYPE msg_type,String slog,int pro){
        if (instance!=null&&instance.logListener != null) {
            instance.logListener.onARLogRecv(msg_type, slog, pro);
        }
    }

    protected void SenLogByHandle(ARCommon.LOG_MSG_TYPE msg_type,Object obj){
        mainThreadHandler.obtainMessage(msg_type.ordinal(),obj).sendToTarget();
    }
}
