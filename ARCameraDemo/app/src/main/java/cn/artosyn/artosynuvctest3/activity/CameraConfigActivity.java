package cn.artosyn.artosynuvctest3.activity;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import cn.artosyn.artosynuvctest3.R;
import cn.artosyn.artosynuvctest3.dialog.FileChooseDialog;
import cn.artosyn.aruvclib.ARCommon;
import cn.artosyn.aruvclib.ARHidTransfer;

public class CameraConfigActivity extends AppCompatActivity implements ARHidTransfer.StatusCallback,FileChooseDialog.NoticeDialogListener {

    private static final String TAG = CameraConfigActivity.class.getSimpleName();

    ARHidTransfer hidTransfer;

    private EditText mEditTextUcmd;
    private TextView mTextViewLog;
    private ProgressBar progressBar;

    int transMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_config);

        hidTransfer = new ARHidTransfer(this);
        hidTransfer.register();
        hidTransfer.addCallback(this);

        mEditTextUcmd = findViewById(R.id.editText_ucmd);
        mTextViewLog = findViewById(R.id.textView_log);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);

        mEditTextUcmd.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                        if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                                actionId == EditorInfo.IME_ACTION_DONE ||
                                keyEvent != null &&
                                        keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                                        keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                            if (keyEvent == null || !keyEvent.isShiftPressed()) {
                                // the user is done typing.
                                String scmd = mEditTextUcmd.getText().toString();
                                if(scmd.isEmpty())
                                    return false;
                                if(hidTransfer.SendUCmd(scmd))
                                    mlog("cmd:"+scmd);
                                else{
                                    mlog("cmd failed:"+scmd);
                                }
                                return false; // consume.
                            }
                        }
                        return false; // pass on to other listeners.
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hidTransfer.unregister();
    }

    public void onClick(View item) {
        switch (item.getId()) {
            case R.id.imageButton_back:
                onBackPressed();
                break;
            case R.id.button_usb:
                if(hidTransfer.Open()){
                    mlog("Hid open success");
                }else{
                    mlog("Hid open failed");
                }
                break;
            case R.id.button_upgrade:
                transMode=0;
                FileChooseDialog fileDialog =new FileChooseDialog();
                fileDialog.show(getSupportFragmentManager(), "fileDialog");
                break;
            case R.id.button_sendfile:
                transMode = 1;
                FileChooseDialog fileDialog1 =new FileChooseDialog();
                fileDialog1.show(getSupportFragmentManager(), "fileDialog");
                break;
            default:
                break;
        }
    }

    public void mlog(String msg)
    {
        mTextViewLog.append("-"+msg+"\n");
        if(mTextViewLog.getLineCount()>500)
        {
            mTextViewLog.setText("");
        }
        final Layout layout =  mTextViewLog.getLayout();
        if(layout==null){
            return;
        }
        final int scrollAmount = layout.getLineTop(mTextViewLog.getLineCount())
                - mTextViewLog.getHeight();
        if (scrollAmount > 0)
            mTextViewLog.scrollTo(0, scrollAmount);
        else
            mTextViewLog.scrollTo(0, 0);
    }

    @Override
    public void onStatusChange(ARCommon.LOG_MSG_TYPE msg_type, String msg, int process) {
        final ARCommon.LOG_MSG_TYPE msg_type1 = msg_type;
        final String msg1 = msg;
        final int process1 = process;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (msg_type1){
                    case HID_TRANS_STATUS:
                        mlog(msg1);
                        break;
                    case HID_TRANS_PROGRESS:
                        progressBar.setProgress(process1);
                        break;
                    case HID_UPGRADE_STATUS:
                        mlog(msg1);
                        break;
                    case HID_UCMD_RET:
                        mlog(msg1);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    @Override
    public void onChoseFileClick(DialogFragment dialog, String filename) {
        Log.i(TAG,String.format("File********%s",filename));
        if(filename!=null){
            if(transMode==0){
                hidTransfer.TransFirmware(filename);
            }
            else{
                hidTransfer.TransFile(filename);
            }
        }
    }
}
