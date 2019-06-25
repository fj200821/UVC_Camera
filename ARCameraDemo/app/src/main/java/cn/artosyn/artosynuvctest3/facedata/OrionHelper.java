package cn.artosyn.artosynuvctest3.facedata;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import cn.artosyn.aruvclib.ARUtil;
import cn.artosyn.aruvclib.model.ARUserFace;
import cn.readsense.faceserver.FaceServerManager;
import cn.readsense.faceserver.RSConfig;
import cn.readsense.faceserver.RSFace;
import cn.readsense.faceserver.RSPerson;

public class OrionHelper {
    private final static String TAG = OrionHelper.class.getSimpleName();

    private FaceServerManager faceServerManager = null;

    private static boolean bInit = false;

    private static OrionHelper orionHelper = null;
    public static OrionHelper instance(){
        if(orionHelper==null){
            orionHelper = new OrionHelper();
        }
        return orionHelper;
    }

    private OrionHelper(){

    }

    public String init(Context context){
        if(bInit){
            return "Cloud already init";
        }
        String address = ARUtil.getMacAddr();
        if(address.equals("02:00:00:00:00:00")){
            return "Can't get mac address";
        }
        faceServerManager = FaceServerManager.getInstance();
        faceServerManager.initDBManager(context);
        int code = faceServerManager.init("orion.readsense.cn",12002,feedCallback,
                address,"1.0.5",
                "859265ddb89c4aae","c2e34eb6b69d411d",RSConfig.DEVICE_TYPE_FLAT);
        String sret = "";
        if (code == 0) {
            sret = "Cloud init success";
            bInit = true;
        } else {
            sret = "Cloud init failed: " + faceServerManager.getErrorMsg(code);
        }
        Log.d(TAG,sret);
        return sret;
    }

    public void uninit(){
        faceServerManager.uninit();
    }

    public ArrayList<ARUserFace> getAllPerson(){
        ArrayList<ARUserFace> userFaces = new ArrayList<>();
        if(!bInit)
            return userFaces;
        List<RSPerson> rsPersonList = faceServerManager.getAllPerson();
        int id = 0;
        for (RSPerson person:rsPersonList) {
            ARUserFace user = new ARUserFace();
            user.name = person.getName();
            user.person_uuid = person.getPersonUuid();
            user.id = ++id;
            List<RSFace> rsFaces = person.getRsFaces();
            if(rsFaces!=null&&rsFaces.size()>0){
                user.imageUrl = rsFaces.get(0).getImageUrl();
            }
            userFaces.add(user);
        }
        return userFaces;
    }

    public RSFace identifyFeature(float[] feature){
        if(!bInit)
            return null;
        RSFace rsFace = faceServerManager.identifyFeature(feature);
        if(rsFace==null)
            return null;
        return rsFace;
    }

    public void sendRecRecord(String puuid, String fuuid, String sbitmap){
        if(!bInit)
            return;
        RSPerson person = faceServerManager.getPersonByPersonId(puuid);
        if(person==null)
            return;
        List<RSFace> rsFaces = person.getRsFaces();
        if(rsFaces==null||rsFaces.size()==0){
            return;
        }
        RSFace rsFace1 = null;
        for (RSFace rsFace:rsFaces) {
            if(rsFace.getFaceuuid().equals(fuuid)){
                rsFace1 = rsFace;
                break;
            }
        }
        if(rsFace1==null)
            return;
        person.setHead(sbitmap);
        int iret = faceServerManager.sendRecoRecord(person,1,0,rsFace1.getFaceuuid(),"");
        //faceServerManager.sendGateState(1);
        Log.w(TAG,"sendRecRecord:"+iret);
    }

    public void sendStrangeRecord(String sbitmap){
        if(!bInit)
            return;
        List<RSPerson> rsPersonList = faceServerManager.getAllPerson();
        if(rsPersonList!=null&&rsPersonList.size()>0);
        RSPerson rsPerson = rsPersonList.get(0);
        rsPerson.setHead(sbitmap);
        int iret = faceServerManager.sendRecoRecord(rsPerson,0,0,"","");
        //faceServerManager.sendGateState(0);
        Log.w(TAG,"sendStrangeRecord:"+iret);
    }

    final private FaceServerManager.FeedCallback feedCallback = new FaceServerManager.FeedCallback() {
        @Override
        public void gateControl(int instruct, int duration, String reason, String text, String voice, String name, String uuid) {
            Log.d(TAG,"deviceControl: " + instruct+duration+reason+text+voice+name+uuid);
        }

        @Override
        public void deviceControl(int cmd) {
            Log.d(TAG,"deviceControl: " + cmd);
        }

        @Override
        public void setRecogMode(int mode, int authmode) {

        }

        @Override
        public void get_connect_state(int state) {
            Log.d(TAG,"get_connect_state: " + state);
        }

        @Override
        public String customMsgWarning(String cmd, String msg) {
            Log.d(TAG,"cmd: " + cmd + " msg: " + msg);
            return "{88888}";
        }
    };
}
