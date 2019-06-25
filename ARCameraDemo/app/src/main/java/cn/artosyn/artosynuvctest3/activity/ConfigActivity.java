package cn.artosyn.artosynuvctest3.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import cn.artosyn.artosynuvctest3.R;
import cn.artosyn.artosynuvctest3.config.DemoConfig;
import cn.artosyn.artosynuvctest3.facedata.DataSync;
import cn.artosyn.artosynuvctest3.facedata.FaceDataUtil;
import cn.artosyn.aruvclib.ARUtil;

public class ConfigActivity extends AppCompatActivity {

    EditText editText_feature_threshold;
    EditText editText_dev_uuid;
    EditText editText_dev_position;
    EditText editText_upload_addr;

    Switch switch_portrait;
    Switch switch_captureface;
    Switch switch_livedetect;
    Switch switch_faceupload;
    Switch switch_cloudmode;

    Spinner spinner_FaceMatchMode;

    boolean cloudmode_tmp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        editText_feature_threshold = ((EditText)findViewById(R.id.editText_feature_threshold));
        editText_feature_threshold.setText(Float.toString(DemoConfig.instance().feature_threshold));

        editText_dev_uuid = ((EditText)findViewById(R.id.editText_dev_uuid));
        editText_dev_uuid.setText(DemoConfig.instance().dev_uuid);

        editText_dev_position = ((EditText)findViewById(R.id.editText_dev_position));
        editText_dev_position.setText(DemoConfig.instance().dev_position);

        switch_portrait = findViewById(R.id.switch_portrait);
        switch_portrait.setChecked(DemoConfig.instance().isPortrait);

        switch_captureface = findViewById(R.id.switch_captureface);
        switch_captureface.setChecked(DemoConfig.instance().captureFace);

        spinner_FaceMatchMode = findViewById(R.id.spinner_facematch_mod);
        spinner_FaceMatchMode.setSelection(DemoConfig.instance().faceMatchMode);

        switch_livedetect = findViewById(R.id.switch_livedetect);
        switch_livedetect.setChecked(DemoConfig.instance().isLiveDetect);

        editText_upload_addr = findViewById(R.id.editText_upload_addr);
        editText_upload_addr.setText(DemoConfig.instance().uploadAssress);

        switch_faceupload = findViewById(R.id.switch_faceupload);
        switch_faceupload.setChecked(DemoConfig.instance().uploadFace);

        switch_cloudmode = findViewById(R.id.switch_cloudmode);
        switch_cloudmode.setChecked(DemoConfig.instance().isCloudMode);
        cloudmode_tmp = DemoConfig.instance().isCloudMode;

        ((TextView)findViewById(R.id.textView_ipaddr)).setText(ARUtil.getIPAddress(true));
        ((TextView)findViewById(R.id.textView_macaddr)).setText(ARUtil.getMacAddr());

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        if(width>height){
            switch_portrait.setEnabled(false);
        }
        else {
            switch_portrait.setEnabled(true);
        }
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case android.R.id.home:
//                // API 5+ solution
//                onBackPressed();
//                return true;
//
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }

    public void onClick(View item) {
        switch (item.getId()) {
            case R.id.imageButton_back:
                onBackPressed();
                break;
            case R.id.imageButton_save_config:
                float v = Float.parseFloat(editText_feature_threshold.getText().toString());
                DemoConfig.instance().feature_threshold = v;
                DemoConfig.instance().dev_uuid = editText_dev_uuid.getText().toString();
                DemoConfig.instance().dev_position = editText_dev_position.getText().toString();
                DemoConfig.instance().isPortrait = switch_portrait.isChecked();
                DemoConfig.instance().captureFace = switch_captureface.isChecked();
                DemoConfig.instance().faceMatchMode = spinner_FaceMatchMode.getSelectedItemPosition();
                DemoConfig.instance().isLiveDetect = switch_livedetect.isChecked();
                DemoConfig.instance().uploadAssress = editText_upload_addr.getText().toString();
                DemoConfig.instance().uploadFace = switch_faceupload.isChecked();
                DemoConfig.instance().isCloudMode = switch_cloudmode.isChecked();
                DemoConfig.instance().save(this);
                if(!DemoConfig.instance().isLiveDetect)
                    DataSync.clearLiveFaces();
                if(cloudmode_tmp^DemoConfig.instance().isCloudMode){
                    Glide.get(this).clearMemory();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Glide.get(ConfigActivity.this).clearDiskCache();
                        }
                    }).start();
                    DataSync.clearIdentFace();
                    FaceDataUtil.clearRecordUserFace();
                }
                Toast.makeText(this,"保存完成",Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }
}
