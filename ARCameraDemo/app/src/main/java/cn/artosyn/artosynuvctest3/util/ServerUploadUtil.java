package cn.artosyn.artosynuvctest3.util;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import cn.artosyn.artosynuvctest3.config.DemoConfig;

public class ServerUploadUtil {
    private static final String TAG = ServerUploadUtil.class.getSimpleName();

    private static Gson gson= new GsonBuilder()/*.excludeFieldsWithoutExposeAnnotation()*/.create();

    static class ZLJ_Up_Data{
        String content;     //bitmap(header+base64)
        int fileType;
    }
    static class ZLJ_Ret_Data{
        int code;
        String msg;
        String data;    //image url
    }

    public static class Upload_Data{
        public String face_image;
        public float face_feature[] = new float[512];
        public float face_cos_distance;
        public String face_name;
        public String device_uuid;
        public String device_position;
    }
    static class Ret_Data{
        int code;
        String msg;
        String data;    //image url
    }

    public static void upload(Upload_Data upload_data){
        Type upType = new TypeToken<Upload_Data>() {}.getType();
        final String sjson_up_data = gson.toJson(upload_data,upType);
        httpPost(sjson_up_data);
    }

    private static void httpPost(String sjson){
        URL url;
        try {
            //url = new URL("http://47.105.204.106:8081/api/face-common/file/upload");
            url = new URL("http://"+DemoConfig.instance().uploadAssress);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }
        HttpURLConnection urlConn = null;
        try {
            urlConn = (HttpURLConnection) url.openConnection();

            urlConn.setDoOutput(true);
            urlConn.setDoInput(true);
            urlConn.setUseCaches(false);
            urlConn.setConnectTimeout(3 * 1000);   // 设置连接超时时间
            urlConn.setReadTimeout(3 * 1000);   // 读取超时
            urlConn.setRequestMethod("POST");
            //urlConn.setRequestProperty("connection", "Keep-Alive");
            urlConn.setRequestProperty("Accept-Charset", "UTF-8");
            urlConn.setRequestProperty("Content-Type", "application/json");
            urlConn.setChunkedStreamingMode(0);

            DataOutputStream requestStream = new DataOutputStream(urlConn.getOutputStream());
            requestStream.write(sjson.getBytes("UTF-8"));
            requestStream.flush();

            int statusCode = urlConn.getResponseCode();
            if (statusCode == 200) {
                String result = streamToString(urlConn.getInputStream());
                Log.d(TAG, "上传成功，result--->" + result);
                Type retType = new TypeToken<Ret_Data>() {}.getType();
                Ret_Data ret_data = gson.fromJson(result,retType);
            } else {
                //String result = streamToString(urlConn.getErrorStream());
                //Log.e(TAG, "Get方式请求失败 code=" + urlConn.getResponseCode() + result);
                Log.d(TAG, "上传失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConn != null)
                urlConn.disconnect();
        }
    }

    public static void uploadBitmap2ZLJ(Bitmap bmp){
        if(bmp==null||bmp.isRecycled())
            return;
        ZLJ_Up_Data up_data = new ZLJ_Up_Data();
        up_data.fileType = 0;
        up_data.content = "data:image/jpeg;base64," + BitmapUtil.bitmapToBase64(bmp);
        Type listType = new TypeToken<ZLJ_Up_Data>() {}.getType();
        final String sjson_up_data = gson.toJson(up_data,listType);

        new Thread(new Runnable(){
            @Override
            public void run() {
                URL url = null;
                try {
                    url = new URL("http://47.105.204.106:8081/api/face-common/file/upload");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    return;
                }
                HttpURLConnection urlConn = null;
                try {
                    urlConn = (HttpURLConnection) url.openConnection();

                    urlConn.setDoOutput(true);
                    urlConn.setDoInput(true);
                    urlConn.setUseCaches(false);
                    urlConn.setConnectTimeout(3 * 1000);   // 设置连接超时时间
                    urlConn.setReadTimeout(3 * 1000);   // 读取超时
                    urlConn.setRequestMethod("POST");
                    //urlConn.setRequestProperty("connection", "Keep-Alive");
                    urlConn.setRequestProperty("Accept-Charset", "UTF-8");
                    urlConn.setRequestProperty("Content-Type", "application/json");
                    urlConn.setChunkedStreamingMode(0);

                    DataOutputStream requestStream = new DataOutputStream(urlConn.getOutputStream());
                    requestStream.write(sjson_up_data.getBytes("UTF-8"));
                    requestStream.flush();

                    int statusCode = urlConn.getResponseCode();
                    if (statusCode == 200) {
                        String result = streamToString(urlConn.getInputStream());
                        Log.e(TAG, "上传成功，result--->" + result);
                    } else {
                        //String result = streamToString(urlConn.getErrorStream());
                        //Log.e(TAG, "Get方式请求失败 code=" + urlConn.getResponseCode() + result);
                        Log.e(TAG, "上传失败");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (urlConn != null)
                        urlConn.disconnect();
                }
            }
        }).start();
    }

    private static String streamToString(InputStream in) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int count = -1;
        while((count = in.read(data,0,1024)) != -1)
            outStream.write(data, 0, count);
        data = null;
        return new String(outStream.toByteArray(),"UTF-8");
    }
}
