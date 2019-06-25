package cn.artosyn.artosynuvctest3.dialog;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import cn.artosyn.aruvclib.ARNativeHelper;

public class DevicesDialog {
    private WeakReference<Context> contextWeakReference;

    public interface ChooseDeviceListener{
        void onChooseDevice(String dev_name);
    }

    private ChooseDeviceListener chooseDeviceLintener = null;

    public DevicesDialog(Context ctx,ChooseDeviceListener listener) {
        contextWeakReference = new WeakReference<>(ctx);
        chooseDeviceLintener = listener;
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(contextWeakReference.get(), cn.artosyn.aruvclib.R.style.Base_Theme_AppCompat_Light_Dialog_Alert);
        builder.setTitle("点击打开相机");

        final List<String> dev_names = new ArrayList<String>();
        int[] dev_indexs = ARNativeHelper.getDevs();
        for (int var:dev_indexs) {
            dev_names.add("/dev/video"+var);
        }
        String[] dev_names2 = new String[dev_names.size()];
        dev_names2 = dev_names.toArray(dev_names2);
        builder.setSingleChoiceItems(dev_names2,-1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(chooseDeviceLintener!=null){
                    chooseDeviceLintener.onChooseDevice(dev_names.get(which));
                }
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("关闭相机", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(chooseDeviceLintener!=null){
                    chooseDeviceLintener.onChooseDevice(null);
                }
            }
        });

        builder.setPositiveButton("取消",null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
