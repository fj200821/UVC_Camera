package cn.artosyn.artosynuvctest3.activity.base;

import android.app.Application;
import android.content.Context;

public class ExApplication extends Application {
    public static ExApplication instance;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }
    public static ExApplication getInstance() {
        return instance;
    }

}
