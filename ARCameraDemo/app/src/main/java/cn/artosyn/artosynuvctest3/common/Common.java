package cn.artosyn.artosynuvctest3.common;

import android.graphics.Rect;

import java.util.ArrayList;

import cn.artosyn.aruvclib.ARCommon;

public class Common {
    public static class IdentiFace{
        public Rect face_rect;
        public int ori_w;
        public int ori_h;
        public int faceid;
        public int frameindex;
    }

    public static class SingleFaceWithFeature{
        public ARCommon.BoxFeatureData boxFeatureData;
        public int ori_w;
        public int ori_h;
        public int frameindex;
        public boolean isRecognized;
    }

}
