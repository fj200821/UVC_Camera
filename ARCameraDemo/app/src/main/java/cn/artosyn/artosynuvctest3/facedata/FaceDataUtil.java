package cn.artosyn.artosynuvctest3.facedata;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import cn.artosyn.artosynuvctest3.activity.base.BaseActivity;
import cn.artosyn.artosynuvctest3.config.DemoConfig;
import cn.artosyn.aruvclib.ARCommon;
import cn.artosyn.aruvclib.model.ARUserFace;

public class FaceDataUtil {

    private static FaceDataUtil faceDataUtil = null;
    private DatabaseHelper databaseHelper = null;

    public static final ArrayList<ARUserFace> userFaces_all = new ArrayList<>();
    public static final LinkedList<ARUserFace> userFaces_record = new LinkedList<>();

    public static void addRecordUserFace(ARUserFace userFace){
        for(Iterator<ARUserFace> iterator = userFaces_record.iterator(); iterator.hasNext();) {
            ARUserFace userFace1 = iterator.next();
            if(userFace1.id==userFace.id)
                iterator.remove();
        }
        userFaces_record.addFirst(userFace);
        if(userFaces_record.size()>5){
            userFaces_record.removeLast();
        }
    }

    public static void clearRecordUserFace(){
        userFaces_record.clear();
    }

    public static FaceDataUtil instance(){
        if(faceDataUtil==null){
            faceDataUtil = new FaceDataUtil(BaseActivity.applicationCtxtWeakRef.get());
        }
        return  faceDataUtil;
    }

    private FaceDataUtil(Context c){
        databaseHelper = new DatabaseHelper(c);
    }

    public void updateUsers(){
        ArrayList<ARUserFace> userFaces;
        if(DemoConfig.instance().isCloudMode) {
            userFaces = OrionHelper.instance().getAllPerson();
        }
        else{
            userFaces = databaseHelper.queryAllFaceData();
        }
        synchronized (FaceDataUtil.userFaces_all) {
            userFaces_all.clear();
            userFaces_all.addAll(userFaces);
        }
    }

    public long insert(ARUserFace face){
        return databaseHelper.insert(face);
    }

    public void deleteAll(){
        databaseHelper.deleteAllFaceData();
        for (int i = 0; i < userFaces_all.size(); i++) {
            String imgPath = DemoConfig.FacePicPath + userFaces_all.get(i).id + ".jpg";
            File imgFile = new File(imgPath);
            if (imgFile.exists()) {
                imgFile.delete();
            }
        }
        userFaces_all.clear();
    }

    public void deleteByID(long id){
        databaseHelper.deleteFaceByID(id);
        String imgPath = DemoConfig.FacePicPath + id + ".jpg";
        File imgFile = new File(imgPath);
        if (imgFile.exists()) {
            imgFile.delete();
        }
    }
}
