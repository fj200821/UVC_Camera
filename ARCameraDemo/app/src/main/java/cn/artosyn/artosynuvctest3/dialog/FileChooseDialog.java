package cn.artosyn.artosynuvctest3.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.preference.DialogPreference;
import android.support.v4.app.DialogFragment;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;

public class FileChooseDialog extends DialogFragment {
    private static final String TAG = FileChooseDialog.class.getSimpleName();

    public interface NoticeDialogListener {
        void onChoseFileClick(DialogFragment dialog,String filename);
    }

    NoticeDialogListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setStyle( STYLE_NO_FRAME,android.R.style.Theme_Black);
    }

    public void setDataListener(NoticeDialogListener listener) {
        this.mListener = listener;
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mChosenFile = null;
        Dialog dialog = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        loadFileList();
        builder.setTitle("Choose your file");
        if (mFileList == null) {
            Log.e(TAG, "Showing file picker before loading the file list");
            dialog = builder.create();
            return dialog;
        }
        builder.setItems(mFileList, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mChosenFile = mFileList[which];
                Log.i(TAG,String.format("File:%s",mChosenFile));
                //you can do stuff with the file here too
                mListener.onChoseFileClick(FileChooseDialog.this,mChosenFile);
            }
        });

        dialog = builder.show();
        return dialog;
    }

    public String GetChosenFIle(){
        return mChosenFile;
    }

    //In an Activity
    private String[] mFileList;
    private File mPath = new File(Environment.getExternalStorageDirectory() + "//");
    private String mChosenFile;
    private static final String FTYPE = ".apk";
    private static final int DIALOG_LOAD_FILE = 1000;

    private void loadFileList() {
        try {
            mPath.mkdirs();
        }
        catch(SecurityException e) {
            Log.e(TAG, "unable to write on the sd card " + e.toString());
        }
        if(mPath.exists()) {
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    return sel.isFile()&&sel.canRead();
                }
            };
            mFileList = mPath.list(filter);
        }
        else {
            mFileList= new String[0];
        }
    }
}
