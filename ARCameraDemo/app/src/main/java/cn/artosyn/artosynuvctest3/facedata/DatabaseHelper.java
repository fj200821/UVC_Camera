package cn.artosyn.artosynuvctest3.facedata;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.gson.Gson;

import java.util.ArrayList;

import cn.artosyn.artosynuvctest3.config.DemoConfig;
import cn.artosyn.aruvclib.ARUtil;
import cn.artosyn.aruvclib.model.ARUserFace;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = SQLiteOpenHelper.class.getSimpleName();

    private static ArrayList<ARUserFace> userList = new ArrayList<>();

    private Gson gson = new Gson();

    private static final int DB_VERSION = 2;
    private static final String DB_NAME = "face_data.db";
    private static final String TABLE_NAME = "RegFaceData";
    private static final String KEY_ID = "Id";
    private static final String KEY_RSID = "rsId";
    private static final String KEY_NAME = "Name";
    private static final String KEY_FEATURE = "Feature";
    private static final String KEY_AGE = "Age";
    private static final String KEY_GENDER = "Gender";

    DatabaseHelper(Context context) {
        super(context, DemoConfig.DatabasePath +DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String sql = "create table if not exists " + TABLE_NAME + " ("+KEY_ID+" integer primary key, "+KEY_RSID+" integer, "+KEY_NAME+" text, "+KEY_AGE+" integer, "+KEY_GENDER+" integer, "+KEY_FEATURE+" blob)";
        sqLiteDatabase.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        sqLiteDatabase.execSQL(sql);
        onCreate(sqLiteDatabase);
    }

    long insert(ARUserFace user){
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(KEY_NAME,    user.name);
        cv.put(KEY_RSID,    user.rsid);
        cv.put(KEY_AGE, user.age);
        int igender = -1;
        if(user.gender.equals("M")) igender = 0;
        else if(user.gender.equals("F")) igender = 1;
        cv.put(KEY_GENDER, igender);
        //String sfeature = gson.toJson(user.feature);
        cv.put(KEY_FEATURE,   ARUtil.floatArr2byteArr(user.feature));
        return database.insert( TABLE_NAME, null, cv );
    }

    ArrayList<ARUserFace> queryAllFaceData(){
        userList.clear();
        SQLiteDatabase db;
        try {
             db = this.getReadableDatabase();
        }
        catch (Exception ex){
            ex.printStackTrace();
            return userList;
        }
        Cursor c = db.query(TABLE_NAME,null,null,null,null,null,null);
        if(c.moveToFirst()){
            while (!c.isAfterLast()){
                int i_id = c.getColumnIndex(KEY_ID);
                int i_name = c.getColumnIndex(KEY_NAME);
                int i_age = c.getColumnIndex(KEY_AGE);
                int i_gender = c.getColumnIndex(KEY_GENDER);
                int i_feature = c.getColumnIndex(KEY_FEATURE);
                int i_rsid = c.getColumnIndex(KEY_RSID);
                ARUserFace user = new ARUserFace();
                user.id = c.getLong(i_id);
                user.name = c.getString(i_name);
                user.age = c.getInt(i_age);
                user.gender = "";
                int igender = c.getInt(i_gender);
                if(igender==0) user.gender = "M";
                else if(igender==1) user.gender = "F";
                //user.gender = c.getInt(i_gender)==0?"M":"F";
                user.rsid = c.getInt(i_rsid);
//                String sfeature = c.getString(i_feature);
//                Float[] feature = gson.fromJson(sfeature, Float[].class);
//                for (int j =0;j<feature.length;j++) {
//                    if(j>=DemoConfig.FEATURE_LEN)
//                        break;
//                    user.feature[j] = feature[j]!=null?feature[j]:0;
//                }
                byte[] feature_bytes = c.getBlob(i_feature);
                user.feature = null;
                user.feature = ARUtil.byteArr2floatArr(feature_bytes);
                user.feature_length = ARUtil.GetVectorLength(user.feature);
                user.bUpdate = true;
                userList.add(user);
                c.moveToNext();
            }
        }
        c.close();
        Log.w(TAG, "queryAllFaceData: "+userList.size());
        return userList;
    }

    void deleteAllFaceData(){
        userList.clear();
        SQLiteDatabase database = this.getWritableDatabase();
        database.delete(TABLE_NAME,null,null);
    }

    boolean deleteFaceByID(Long id){
        SQLiteDatabase database = this.getWritableDatabase();
        String[] args =  new String[1];
        args[0] = id.toString();
        int rowaffect = database.delete(TABLE_NAME,KEY_ID+"=?", args);
        if(rowaffect==1){
            return true;
        }
        return false;
    }
}
