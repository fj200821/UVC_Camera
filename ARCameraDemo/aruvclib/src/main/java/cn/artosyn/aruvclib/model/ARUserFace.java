package cn.artosyn.aruvclib.model;

import cn.artosyn.artranslib.UserFace;

public class ARUserFace{
    public long id;
    public transient int rsid;
    public String name;
    public transient boolean bUpdate;
    public transient int iSimilarity;
    public String gender;
    public int age;
    public transient float[] feature;
    public transient float feature_length;
    public transient Boolean isLive;
    public transient String person_uuid;
    public transient String face_uuid;
    public transient String imageUrl;
    public ARUserFace(){
        id = -1;
        name = "";
        feature = new float[512];
        bUpdate = false;
        feature_length = 0;
        iSimilarity = -1;
        person_uuid = face_uuid = imageUrl= "";
    }

    ARUserFace(UserFace userFace){
        id = userFace.id;
        rsid = userFace.rsid;
        name = userFace.name;
        bUpdate = userFace.bUpdate;
        iSimilarity = userFace.iSimilarity;
        gender = userFace.gender;
        age = userFace.age;
        feature = userFace.feature;
        feature_length = userFace.feature_length;
        person_uuid = face_uuid = imageUrl= "";
    }
}
