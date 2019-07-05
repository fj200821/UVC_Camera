package com.moons.user;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cn.artosyn.artosynuvctest3.activity.base.ExApplication;


public class UserHelper extends SQLiteOpenHelper {
    private String TAG = this.getClass().getSimpleName().toString();
    private static String DB_PATH = ExApplication.getContext().getExternalCacheDir().getPath(); //数据库默认保存路径
    private String DB_NAME;
    //storage/emulated/0/android/data/com.arcsoft.sdk_demo/cache目录下
    private String DB_PATH_BACKUP = ExApplication.getContext().getExternalCacheDir().getPath(); //数据库备份路径
    private Context mContext;

    public static final String CREATE_FACELIST = "create table userlist(" +
            "  userid integer primary key Autoincrement," +
            "  username char(50) not null," +
            "  userpwd char(50)" +
            ");";

    //第一个参数context,第二个参数数据库名,第三个参数一般传入null,第四个参数数据库版本号
    public UserHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        mContext = context;
        this.DB_NAME = name;
        Log.i(TAG, "--UserHelper--");
    }


    public void createDataBase() throws IOException {
        boolean dbExist = checkDataBase();
        if (dbExist) {
            //数据库已存在
        } else {
            this.getReadableDatabase();//获取一个用于操作数据库的SQLiteDatabase实例
            try {
                copyDataBase();//拷贝备份的数据库
            } catch (IOException e) {
                throw new Error("Error copying database");
            }
        }
    }

    private boolean checkDataBase() {
        SQLiteDatabase checkDB = null;
        try {
            String myPath = DB_PATH + DB_NAME;
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
        } catch (SQLiteException e) {
            //数据库不存在
        }
        if (checkDB != null) {
            checkDB.close();
        }
        return checkDB != null ? true : false;
    }


    private void copyDataBase() throws IOException {
        File file = new File(DB_PATH_BACKUP + "/" + DB_NAME); //备份数据库文件
        if (!file.exists()) {
            Log.i(TAG, "backup UserDB isn't exist");
            return;
        }
        InputStream myInput = new FileInputStream(file);
        String outFileName = DB_PATH + "/" + DB_NAME; //默认数据库文件
        OutputStream myOutput = new FileOutputStream(outFileName);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }
        //Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();
    }


    // 创建数据库
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.e(TAG, "--create datebasse--");
        String sql = CREATE_FACELIST; //创建表;
        db.execSQL(sql);
    }

    // 数据库更新
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.e(TAG, "--upgrade datebasse--");
    }


    public void addUserData(UserData userData) {
        Log.e(TAG, "--addUserData--");
        SQLiteDatabase db = getWritableDatabase(); //以读写的形式打开数据库
        db.execSQL("insert into userlist(userid,username,userpwd) values("
                + null + ","
                + String.format("'%s'", userData.getUserName()) + ","
                + String.format("'%s'", userData.getUserPwd()) +
                ");"
        ); // 插入数据库
        db.close(); // 关闭数据库连接
    }


    public void deleteUserData(String username) {
        Log.e(TAG, "--deleteUserData--");
        SQLiteDatabase db = getWritableDatabase(); // 以读写的形式打开数据库
        String sql = "username = ?";
        String wheres[] = {String.valueOf(username)};
        db.delete("userlist", sql, wheres); // 数据库删除
        db.close(); // 关闭数据库`
    }


    public void updateUserData(UserData userData) {
        SQLiteDatabase db = getWritableDatabase(); // 以读写的形式打开数据库
        String sql = "update userlist set username="
                + String.format("'%s'", userData.getUserName())
                + ",userpwd=" + userData.getUserPwd()
                + " where username=" + String.format("'%s'", userData.getUserName());
        db.execSQL(sql); // 更新数据库
        db.close(); // 关闭数据库连接
    }


    public Boolean isUserNameExist(String username) {
        SQLiteDatabase db = getReadableDatabase(); // 以只读方式打开数据库
        String[] columns = {"userid", "username", "userpwd"}; //你想要的数据
        String selection = "username=?";  //条件字段
        String[] selectionArgs = {username}; //具体的条件,对应条件字段
        Cursor cursor = db.query("userlist", columns, selection, selectionArgs,
                null, null, null);
        if (cursor.moveToNext()) {
            return true;
        }
        return false;
    }

    public UserData queryUserDataByUserName(String username) {
        UserData userData = null;
        SQLiteDatabase db = getReadableDatabase(); // 以只读方式打开数据库/"//、、
        String[] columns = {"userid", "username", "userpwd"}; //你想要的数据
        String selection = "username=?";  //条件字段
        String[] selectionArgs = {username}; //具体的条件,对应条件字段
        Cursor cursor = db.query("userlist", columns, selection, selectionArgs,
                null, null, null);
        if (cursor.moveToNext()) {
            userData = new UserData();
            userData.setUserID(cursor.getInt(cursor.getColumnIndex("userid")));
            userData.setUserName(cursor.getString(cursor.getColumnIndex("username")));
            userData.setUserPwd(cursor.getString(cursor.getColumnIndex("userpwd")));
        }
        return userData;

    }


}