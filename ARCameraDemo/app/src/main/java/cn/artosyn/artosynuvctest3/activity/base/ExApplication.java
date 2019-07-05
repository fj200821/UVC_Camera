package cn.artosyn.artosynuvctest3.activity.base;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;

import com.moons.serial.serialdemo.DoorLockManager;
import com.moons.user.DatabaseContext;
import com.moons.user.UserData;
import com.moons.user.UserHelper;

public class ExApplication extends Application {
    public static ExApplication instance;
    private static final String TAG = "ExApplication";
    private static Context mContext = null;
    private static UserHelper mUserListDB;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        mContext = getApplicationContext();
        initDB();
        doSomeBuniss();
    }

    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }

    public static ExApplication getInstance() {
        return instance;
    }


    private void doSomeBuniss() {
        DoorLockManager.getInstance().initSerial();//初始化串口通信
    }

    public static Resources getAppResources() {
        if (mContext == null) return null;
        return mContext.getResources();
    }


    public static Context getContext() {
        return mContext;
    }

    private void initDB() {
        DatabaseContext dbContext = new DatabaseContext(getApplicationContext()); //自定义database context更改路径
        mUserListDB = new UserHelper(dbContext, "userlist.db", null, 1);
        if (!mUserListDB.isUserNameExist("admin")) {
            UserData userData = new UserData();
            userData.setUserName("admin");
            userData.setUserPwd("password");
            mUserListDB.addUserData(userData);
        }
    }

    public static UserHelper getUserListDB() {
        return mUserListDB;
    }
    
}
