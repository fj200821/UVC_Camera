package cn.artosyn.artosynuvctest3.config;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.Serializable;

import cn.artosyn.artosynuvctest3.util.ConfigUtil;

public class DemoConfig implements Serializable {
    private static DemoConfig demoConfig = null;
    public static DemoConfig instance(){
        if(demoConfig==null){
            demoConfig = new DemoConfig();
        }
        return demoConfig;
    }

    private DemoConfig(){

    }

    public void update(Context context){
        ConfigUtil.updateConfig(context,demoConfig);
    }

    public void save(Context context){
        ConfigUtil.saveConfig(context,demoConfig);
    }


    public static final int FEATURE_LEN = 256;

    //本地数据目录
    public static final String RootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ar_face_data/";
    //注册人脸图像路径
    public static final String FacePicPath = RootPath + "ar_face_pic/";
    //数据库路径
    public static final String DatabasePath = RootPath + "ar_face_db/";
    //抓拍路径
    public static final String CapturePath = RootPath + "ar_face_capture/";

    public float feature_threshold = 0.40f;

    public String dev_uuid = "";
    public String dev_position = "";

    public boolean useNativeDraw = true;
    public boolean isPortrait = false;

    public boolean captureFace = false;
    public boolean isLiveDetect = false;
    public boolean uploadFace = false;
    public String uploadAssress = "";
    public int faceMatchMode = 0;

    public boolean isCloudMode = false;

    public void updateFolder() {
        File file = new File(DemoConfig.RootPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        File file2 = new File(DemoConfig.FacePicPath);
        if (!file2.exists()) {
            file2.mkdirs();
        }
        File file3 = new File(DemoConfig.DatabasePath);
        if (!file3.exists()) {
            file3.mkdirs();
        }
        File file4 = new File(DemoConfig.CapturePath);
        if (!file4.exists()) {
            file4.mkdirs();
        }
    }
}
