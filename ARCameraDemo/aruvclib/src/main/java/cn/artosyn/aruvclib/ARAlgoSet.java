package cn.artosyn.aruvclib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import cn.artosyn.artranslib.ARTransSet;
import cn.artosyn.artranslib.FaceResult;
import cn.artosyn.aruvclib.model.ARFaceResult;

public class ARAlgoSet {
    private WeakReference<Context> contextWeakRef = null;
    private ARTransSet arTransSet;
    private static final Object algoLock = new Object();
    public ARAlgoSet(Context c, String dbPath) {
        contextWeakRef = new WeakReference<>(c);
        arTransSet = new ARTransSet(c,dbPath);
    }

    public ARFaceResult init(){
        FaceResult faceResult = arTransSet.init();
        return new ARFaceResult(faceResult);
    }

    public void release(){
        arTransSet.release();
    }

    public ARFaceResult registByBitmap(Bitmap bitmap){
        FaceResult faceResult;
        synchronized (algoLock) {
            faceResult = arTransSet.registByBitmap(bitmap);
        }
        return new ARFaceResult(faceResult);
    }

    public boolean removeAllUser(){
        return arTransSet.removeAllUser();
    }

    public boolean removeUserByPersonId(int personId) {
        return arTransSet.removeUserByPersonId(personId);
    }

    public Boolean detectLiveness(Bitmap bitmap,Rect rect){
        Boolean bresult = null;
        synchronized (algoLock) {
            bresult = arTransSet.detectLiveness(bitmap,rect);
        }
        return bresult;
    }
}
