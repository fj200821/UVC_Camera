package cn.artosyn.artosynuvctest3.dialog;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import cn.artosyn.artosynuvctest3.R;
//通知对话框类
public class NotifyDialog extends DialogFragment {

    //private static NotifyDialog frag = null;

    public static NotifyDialog instance(String name) {
        NotifyDialog frag = new NotifyDialog();
        Bundle args = new Bundle();
        args.putString("title", name);
        frag.setArguments(args);
        return frag;
    }

//    @Override
//    public Dialog onCreateDialog(Bundle savedInstanceState) {
//        //getting proper access to LayoutInflater is the trick. getLayoutInflater is a                   //Function
//        LayoutInflater inflater = getActivity().getLayoutInflater();
//
//        View view = inflater.inflate(R.layout.layout_notify_dialog, null);
//        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//        builder.setView(view);
//        return builder.create();
//    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_notify_dialog, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Get field from view
//        mEditText = (EditText) view.findViewById(R.id.txt_your_name);
//        // Fetch arguments from bundle and set title
//        String title = getArguments().getString("title", "Enter Name");
//        getDialog().setTitle(title);
//        // Show soft keyboard automatically and request focus to field
//        mEditText.requestFocus();
//        getDialog().getWindow().setSoftInputMode(
//                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }


}
