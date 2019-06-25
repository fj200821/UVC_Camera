package cn.artosyn.artosynuvctest3.activity.base;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.lang.ref.WeakReference;

//import readsense.face24.Config.DemoConfig;
//import readsense.face24.util.CPUUtil;
//import readsense.face24.util.SharedPrefUtils;


/**
 * 所有Activity基类
 */
public abstract class BaseActivity extends AppCompatActivity {

    private ProgressDialog mProgressDialog;
    public static WeakReference<Context> applicationCtxtWeakRef;
    //public DemoConfig mConfig;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //initView();
        //initData();
        //initEvent();
        initConfig();
        applicationCtxtWeakRef = new WeakReference<>(this.getApplicationContext());
    }


    @Override
    protected void onResume() {
        //hideBottomUIMenu();
        super.onResume();
       // CPUUtil.setScalingMaxFreq("1274000");
    }

//    protected abstract void initData();
//
//    protected abstract void initView();
//
//    protected abstract void initEvent();

    public void toastMsg(String content) {
        Toast.makeText(this, content, Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示LongToast
     *
     * @param context
     * @param content
     */
    protected void toastLongMsg(Context context, String content) {
        Toast.makeText(context, content, Toast.LENGTH_LONG).show();
    }

    /**
     * 显示进度对话框不可手动取消
     */
    protected void showProgressDialog(Context context, String message) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this, ProgressDialog.THEME_HOLO_LIGHT);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setMessage(message);
        }
        mProgressDialog.show();
    }

    /**
     * 显示进度对话框可手动取消
     */
    protected void showCancelableProgressDialog(Context context, String message) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this, ProgressDialog.THEME_HOLO_LIGHT);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(true);
            mProgressDialog.setMessage(message);
        }
        mProgressDialog.show();
    }

    /**
     * 取消显示进度对话框
     */
    protected void dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    private void initConfig() {
//        mConfig = SharedPrefUtils.getObject(ExApplication.getContext(), "DEMO_CONFIG", DemoConfig.class);
//        if (mConfig == null) {
//            mConfig = new DemoConfig();
//            SharedPrefUtils.putObject(ExApplication.getContext(), "DEMO_CONFIG", mConfig);
//        }
    }
}
