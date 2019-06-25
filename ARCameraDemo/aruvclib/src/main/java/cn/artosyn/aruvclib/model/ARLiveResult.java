package cn.artosyn.aruvclib.model;

import android.graphics.Rect;
import cn.artosyn.artranslib.LiveResult;

public class ARLiveResult {
    public Rect face_rect;
    public boolean isLive;

    public ARLiveResult(){
        face_rect = null;
    }

    public ARLiveResult(LiveResult liveResult){
        face_rect = liveResult.face_rect;
        isLive = liveResult.isLive;
    }
}
