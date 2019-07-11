package cn.artosyn.artosynuvctest3.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.moons.login.ExitActivity;
import com.moons.login.LoginActivity;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import cn.artosyn.artosynuvctest3.R;
import cn.artosyn.artosynuvctest3.activity.adapter.FaceRecordAdapter;
import cn.artosyn.artosynuvctest3.activity.base.BaseActivity;
import cn.artosyn.artosynuvctest3.config.DemoConfig;
import cn.artosyn.artosynuvctest3.dialog.DevicesDialog;
import cn.artosyn.artosynuvctest3.facedata.FaceDataUtil;
import cn.artosyn.artosynuvctest3.facedata.OrionHelper;
import cn.artosyn.artosynuvctest3.register.RegistFromRemote;
import cn.artosyn.artosynuvctest3.view.CameraPreview;
import cn.artosyn.aruvclib.ARHidTransfer;
import cn.artosyn.aruvclib.ARNativeHelper;
import io.reactivex.functions.Consumer;

public class MainActivity extends BaseActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final boolean CEXTESTART = false;

    CameraPreview cameraPreview;    //摄像机预览类
    DevicesDialog devicesDialog;   //设备对话框

    RegistFromRemote registFromRemote;   //PC远程端的注册

    FaceRecordAdapter faceRecordAdapter;   //人脸识别记录适配器

    MyBroadcastReceiver myBroadcastReceiver;    //广播接收器

    View bottom_panel;   //底部界面
    //日期格式
    private final SimpleDateFormat _sdfWatchTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat _sdfWatchDate = new SimpleDateFormat("yyyy年MM月dd日 E", Locale.getDefault());
    private final SimpleDateFormat _sdfWatchDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    TextView textView_time, textView_date, textView_notifyName, textView_notifyTime;//显示日期、时刻、姓名、时间的textview


    private static final int REQUEST_CODE_FACE_REGIST = 2;   //人脸注册请求
    private static final int REQUEST_CODE_IMAGEVIW_CAMREG = 3;   //注册管理请求
    private static final int REQUEST_CODE_EXIT = 4;   //退出请求

    Handler handler_main;
    ImageView imageView_notifyAvatar;
    View notify_dialog;

    boolean btemp;

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (CEXTESTART) {
            Thread.setDefaultUncaughtExceptionHandler(new MyExceptionHandler(this));
            if (getIntent().getBooleanExtra("crash", false)) {
                Toast.makeText(this, "App restarted after crash", Toast.LENGTH_SHORT).show();
            }
        }

        DemoConfig.instance().updateFolder();
        DemoConfig.instance().update(this);

        bottom_panel = findViewById(R.id.bottom_panel);
        textView_time = findViewById(R.id.textView_time);
        textView_date = findViewById(R.id.textView_date);

        setBottomPanelVisible();

        SurfaceView preview = findViewById(R.id.surfaceView_preview);
        SurfaceView overlay = findViewById(R.id.surfaceView_overlay);
        cameraPreview = new CameraPreview(this, preview, overlay);

        faceRecordAdapter = new FaceRecordAdapter(this);
        RecyclerView recyclerView;
        recyclerView = findViewById(R.id.recyclerView_FaceRecord);
        //recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        faceRecordAdapter = new FaceRecordAdapter(this);
        recyclerView.setAdapter(faceRecordAdapter);

        final RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.RECEIVE_BOOT_COMPLETED,Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (!aBoolean) {
                            onDestroy();
                        } else init();
                    }
                });

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(Intent.ACTION_TIME_TICK);
        myBroadcastReceiver = new MyBroadcastReceiver();
        registerReceiver(myBroadcastReceiver, filter);

        handler_main = new Handler();
        notify_dialog = findViewById(R.id.notify_dialog);
        textView_notifyName = findViewById(R.id.textView_notify_name);
        textView_notifyTime = findViewById(R.id.textView_notify_time);
        imageView_notifyAvatar = findViewById(R.id.imageView_notify_avatar);
    }

    //处理返回键和Home键
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) || (keyCode == KeyEvent.KEYCODE_HOME)) {
            Intent intent = new Intent(this, ExitActivity.class);
            startActivityForResult(intent, REQUEST_CODE_EXIT);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //init();
    }

    @Override
    protected void onStop() {
        //cameraPreview.destory();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        cameraPreview.destory();
        if (DemoConfig.instance().isCloudMode)
            OrionHelper.instance().uninit();
        unregisterReceiver(myBroadcastReceiver);
        super.onDestroy();
        finish();
        System.exit(0);
    }

    public void init() {
        OrionHelper.instance().setContext(this);
        if (DemoConfig.instance().isCloudMode) {
            toastMsg(OrionHelper.instance().init(this));

        } else {
            registFromRemote = new RegistFromRemote(this);
        }
        new InitTask(this).execute();
    }

    public static class InitTask extends AsyncTask<Void, Void, Void> {

        WeakReference<MainActivity> activityWeakReference;

        InitTask(MainActivity ctx) {
            activityWeakReference = new WeakReference<>(ctx);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Glide.get(activityWeakReference.get()).clearDiskCache();
            FaceDataUtil.instance().updateUsers();
            return null;
        }

        @Override
        protected void onPreExecute() {
            activityWeakReference.get().showProgressDialog(null, "初始化中...");
        }

        @Override
        protected void onPostExecute(Void result) {
            activityWeakReference.get().dismissProgressDialog();
        }
    }

    public void onClick(View item) {
        switch (item.getId()) {
            case R.id.tool_devices:
                devicesDialog = new DevicesDialog(this, new DevicesDialog.ChooseDeviceListener() {
                    @Override
                    public void onChooseDevice(String dev_name) {
                        if (dev_name == null) {
                            cameraPreview.Close();
                            return;
                        }
                        int ret = cameraPreview.Open(dev_name);
                        switch (ret) {
                            case 0:
                                toastMsg("Capture success");
                                break;
                            case -1:
                                toastMsg("Open failed");
                                break;
                            case -2:
                                toastMsg("Capture failed");
                                break;
                            case -3:
                                toastMsg("Already open");
                                break;
                        }
                    }
                });
                devicesDialog.show();
                break;
            case R.id.tool_camreg:
                if (DemoConfig.instance().isCloudMode) {
                    toastMsg("云端模式无法注册");
                } else {
                    cameraPreview.registerFromCam();
                    onFaceRecordChange();
                }
//                Intent intent2 = new Intent(MainActivity.this, LoginActivity.class);
//                startActivityForResult(intent2, REQUEST_CODE_IMAGEVIW_CAMREG);
                break;
            case R.id.tool_users:
//                Intent intent = new Intent(MainActivity.this, FaceRegisterActivity.class);
//                startActivity(intent);
                Intent intent3 = new Intent(MainActivity.this, LoginActivity.class);
                startActivityForResult(intent3, REQUEST_CODE_FACE_REGIST);
                break;
//            case R.id.tool_camconfig:
//                Intent intent4 = new Intent(MainActivity.this, CameraConfigActivity.class);
//                startActivity(intent4);
//                break;
            case R.id.tool_config:
                btemp = DemoConfig.instance().isCloudMode;
                Intent intent5 = new Intent(MainActivity.this, ConfigActivity.class);
                startActivityForResult(intent5, 1);
                break;
            default:
                break;
        }
    }

    //activity回调
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_CODE_FACE_REGIST && resultCode == RESULT_OK) {
            Intent intent = new Intent(MainActivity.this, FaceRegisterActivity.class);
            startActivity(intent);
        } else if (requestCode == REQUEST_CODE_EXIT && resultCode == RESULT_OK) {
            this.finish();
        } else if (requestCode == REQUEST_CODE_IMAGEVIW_CAMREG && resultCode == RESULT_OK) {
            if (DemoConfig.instance().isCloudMode) {
                toastMsg("云端模式无法注册");
            } else {
                cameraPreview.registerFromCam();   //cameraPreview通过摄像机注册
                onFaceRecordChange();   //人脸识别记录更新
            }
        } else if (requestCode == 1) {
            setBottomPanelVisible();
            onFaceRecordChange();
            if (btemp ^ DemoConfig.instance().isCloudMode)
                new InitTask(this).execute();
        }
    }

    void setBottomPanelVisible() {
        if (DemoConfig.instance().isPortrait) {
            bottom_panel.setVisibility(View.VISIBLE);
            setCurrentTime();
        } else {
            bottom_panel.setVisibility(View.GONE);
        }
    }

    void setCurrentTime() {
        textView_time.setText(_sdfWatchTime.format(new Date()));
        textView_date.setText(_sdfWatchDate.format(new Date()));
    }

    public void onFaceRecordChange() {
        faceRecordAdapter.notifyDataSetChanged();
    }

    class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Log.i(TAG, "USB_CONNECTED");
                //EventBus.getDefault().post(new SettingBusEvent("USB_CONNECTED"));
                if (ARHidTransfer.hasArUvc(MainActivity.this)) {
                    Log.i(TAG, "Find artosyn camera" + Arrays.toString(ARNativeHelper.getDevs()));
                    int ret = cameraPreview.autoOpen();

                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.i(TAG, "USB_DISCONNECTED");
                if (!ARHidTransfer.hasArUvc(MainActivity.this)) {
                    Log.i(TAG, "No artosyn camera,close");
                    cameraPreview.Close();
                }
            } else if (Intent.ACTION_TIME_TICK.equals(action)) {
                if (DemoConfig.instance().isPortrait)
                    setCurrentTime();
            }
        }
    }
    //显示通知对话框(人脸识别成功）
    public void showNotifyDialog(String name, String image) {
        textView_notifyName.setText(name);
        textView_notifyTime.setText(_sdfWatchDateTime.format(new Date()));
        notify_dialog.setVisibility(View.VISIBLE);
        if (DemoConfig.instance().isCloudMode) {
            if (!image.isEmpty()) {
                Glide.with(this).load(image)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(imageView_notifyAvatar);
            }
        } else {
            Glide.with(this).load(new File(image))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView_notifyAvatar);
        }
        handler_main.removeCallbacks(closeRunnable);
        handler_main.postDelayed(closeRunnable, 1600);
    }

    private Runnable closeRunnable = new Runnable() {
        @Override
        public void run() {
            notify_dialog.setVisibility(View.INVISIBLE);
        }
    };

}
