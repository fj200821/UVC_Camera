package cn.artosyn.aruvclib.model;

import cn.artosyn.artranslib.FaceResult;

public class ARFaceResult{
    public int personId;
    public float[] rect;
    public ARUserFace userFace;
    public String msg;
    public int code;

    public ARFaceResult(){

    }

    public ARFaceResult(FaceResult faceResult){
        code = faceResult.code;
        msg = faceResult.msg;
        personId = faceResult.personId;
        rect = faceResult.rect;
        if(faceResult.userFace!=null)
            userFace = new ARUserFace(faceResult.userFace);
        else userFace = null;
    }
}
