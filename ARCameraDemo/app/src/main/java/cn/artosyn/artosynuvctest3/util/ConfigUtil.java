package cn.artosyn.artosynuvctest3.util;

import android.content.Context;
import android.content.SharedPreferences;

import cn.artosyn.artosynuvctest3.config.DemoConfig;

public class ConfigUtil {
    private static final String PREFS_NAME = "DemoPrefsFile";
    public static void updateConfig(Context context, DemoConfig demoConfig){
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);

        demoConfig.feature_threshold = settings.getFloat("feature_threshold", demoConfig.feature_threshold);
        demoConfig.dev_uuid = settings.getString("dev_uuid", demoConfig.dev_uuid);
        demoConfig.dev_position = settings.getString("dev_position", demoConfig.dev_position);
        demoConfig.useNativeDraw = settings.getBoolean("use_nativedraw",demoConfig.useNativeDraw);
        demoConfig.isPortrait = settings.getBoolean("is_portrait",demoConfig.isPortrait);
        demoConfig.captureFace = settings.getBoolean("capture_face",demoConfig.captureFace);
        demoConfig.faceMatchMode = settings.getInt("face_match_mode",demoConfig.faceMatchMode);
        demoConfig.isLiveDetect = settings.getBoolean("live_detect",demoConfig.isLiveDetect);
        demoConfig.uploadFace = settings.getBoolean("upload_face",demoConfig.uploadFace);
        demoConfig.uploadAssress = settings.getString("upload_address",demoConfig.uploadAssress);
        demoConfig.isCloudMode = settings.getBoolean("cloud_mode",demoConfig.isCloudMode);

    }

    public static void saveConfig(Context context, DemoConfig demoConfig){
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putFloat("feature_threshold", demoConfig.feature_threshold);
        editor.putString("dev_uuid", demoConfig.dev_uuid);
        editor.putString("dev_position", demoConfig.dev_position);
        editor.putBoolean("use_nativedraw", demoConfig.useNativeDraw);
        editor.putBoolean("is_portrait", demoConfig.isPortrait);
        editor.putBoolean("capture_face", demoConfig.captureFace);
        editor.putInt("face_match_mode", demoConfig.faceMatchMode);
        editor.putBoolean("live_detect",demoConfig.isLiveDetect);
        editor.putBoolean("upload_face",demoConfig.uploadFace);
        editor.putString("upload_address", demoConfig.uploadAssress);
        editor.putBoolean("cloud_mode",demoConfig.isCloudMode);

        editor.apply();
    }

}
