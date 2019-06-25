package cn.artosyn.artosynuvctest3.register;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.artosyn.artosynuvctest3.config.DemoConfig;
import cn.artosyn.artosynuvctest3.facedata.FaceDataUtil;
import cn.artosyn.artosynuvctest3.register.RegisterHelper;
import cn.artosyn.artosynuvctest3.util.BitmapUtil;
import cn.artosyn.aruvclib.model.ARUserFace;
import fi.iki.elonen.NanoHTTPD;

public class RegistFromRemote {
    private static final String TAG = RegistFromRemote.class.getSimpleName();

    private ARHttpServer arHttpServer;
    private Gson gson;
    private WeakReference<Context> mainActivityWeakReference = null;

    private RegisterHelper registerHelper;

    public RegistFromRemote(Context ctx){
        gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).create();

        registerHelper = new RegisterHelper(ctx);
        registerHelper.init();

        arHttpServer = new ARHttpServer();
        try {
            arHttpServer = new ARHttpServer();
            arHttpServer.start();
        } catch (IOException e) {
            e.printStackTrace();
            Log.w(TAG,"server start failed");
        }
        Log.w(TAG,"server start success");
    }

    class ARHttpServer extends NanoHTTPD{
        ARHttpServer(){
            super(1234);
        }

        @Override
        public Response serve(IHTTPSession session) {
            Map<String, String> headers = session.getHeaders();
            Map<String, List<String>> parms = session.getParameters();
            Method method = session.getMethod();
            String uri = session.getUri();
            Map<String, String> files = new HashMap<>();
            if(uri.equals("/favicon.ico ")){
                return null;
            }
            if(parms.size()>1){
                return newFixedLengthResponse("Error parameter length");
            }
            if (Method.POST.equals(method) || Method.PUT.equals(method)) {
                try {
                    session.parseBody(files);
                } catch (IOException ioe) {
                    return newFixedLengthResponse("Internal Error IO Exception: " + ioe.getMessage());
                } catch (ResponseException re) {
                    return newFixedLengthResponse(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
                }
            }
            Response response = null;
            switch (uri){
                case "/status":{
                    response =  newFixedLengthResponse("ok");
                }
                break;
                case "/reged":{
                    Type listType = new TypeToken<ArrayList<ARUserFace>>() {}.getType();
                    String sjson_reged = gson.toJson(FaceDataUtil.userFaces_all,listType);
                    response = newFixedLengthResponse(sjson_reged);
                }
                break;
                case "/regedimg":{
                    String sid = parms.get("id").get(0);
                    Log.w(TAG, "reged img id "+sid);
                    if(sid==null||sid.isEmpty()){
                        response =  newFixedLengthResponse("get imgage failed");
                    }
                    else {
                        int id = Integer.parseInt(sid);
                        for (ARUserFace userFace :FaceDataUtil.userFaces_all) {
                            if(userFace.id == id){
                                InputStream in = null;
                                try {
                                    in = new FileInputStream(new File(DemoConfig.FacePicPath +userFace.id+".jpg"));
                                    response = newFixedLengthResponse(Response.Status.OK,"application/octet-stream",in,in.available());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                break;
                            }
                        }
                        if(response == null){
                            response = newFixedLengthResponse(Response.Status.NO_CONTENT,"text/xml","no data");
                        }

                    }
                }
                break;
                case "/addface": {
                    Log.w(TAG, ">>>>>>>>>>>>>>>>>>>>>>> begin");
                    AddData_Server addData = null;
                    String jsonFilePath = files.get("jsondata");
                    File src = new File(jsonFilePath);
                    try {
                        InputStream in = new FileInputStream(src);
                        Reader inReader = new InputStreamReader(in);
                        //String sret = convertStreamToString(in);
                        Type addDataType = new TypeToken<AddData_Server>() {}.getType();
                        addData = gson.fromJson(inReader, addDataType);
                        inReader.close();
                        in.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (addData == null) {
                        response = newFixedLengthResponse("fail-json data error");
                        break;
                    }
                    Log.w(TAG, "name: "+addData.name);
                    String path = files.get("img" + addData.index);
                    Bitmap bitmap = BitmapUtil.getSmallBitmap(path,1200,1200);
                    //Bitmap bitmap = BitmapFactory.decodeFile(path);
                    if(bitmap==null){
                        response = newFixedLengthResponse("fail-get image fail");
                        break;
                    }
                    Log.w(TAG, "register begin "+bitmap.getWidth()+"X"+bitmap.getHeight());
                    String sresult = registerHelper.register(bitmap, addData.name);
                    Log.w(TAG, "register end");
                    if (sresult.equals("ok")) {
                        response = newFixedLengthResponse("ok");
                    } else {
                        response = newFixedLengthResponse("fail-"+sresult);
                    }
                    Log.w(TAG, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< end "+sresult);
                }
                break;
                case "/deldata":{
                    String sids = parms.get("ids").get(0);
                    Log.d(TAG, "serve: delete "+sids);
                    if(sids==null||sids.isEmpty()){
                        response =  newFixedLengthResponse("delete failed");
                    }
                    else {
                        Type intlistType = new TypeToken<ArrayList<Integer>>() {}.getType();
                        ArrayList<Integer> ids = gson.fromJson(sids,intlistType);
                        Log.d(TAG, "serve: delte from json "+ids.toString());
                        registerHelper.delete_users(ids);
                        response = newFixedLengthResponse("delete ok");
                    }
                }
                break;
                default:
                    response =  newFixedLengthResponse("parameter error");
                    break;
            }
            return response;
        }
    }

    class AddData_Server{
        @Expose
        int index;
        @Expose
        String name = "";
        @Expose
        int gender;
        @Expose
        int age;
    }
}
