package cn.artosyn.artosynuvctest3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import cn.artosyn.artosynuvctest3.activity.MainActivity;
import cn.artosyn.artosynuvctest3.activity.base.ExApplication;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(ExApplication.getContext(), "收到系统启动成功的消息", Toast.LENGTH_SHORT).show();
        Intent myIntent = new Intent(context, MainActivity.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(myIntent);
    }
}
