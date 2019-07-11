package com.moons.LoginDialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.moons.user.UserData;
import com.moons.user.UserHelper;

import cn.artosyn.artosynuvctest3.R;
import cn.artosyn.artosynuvctest3.activity.base.ExApplication;

public class LoginDialog extends Dialog {
    private Context context;
    private View customView;
    private OnConfirmClickListener onConfirmClickListener;
    private EditText mAccount;                        //用户名编辑
    private EditText mPwd;                            //密码编辑
    private Button mLoginButton;                      //登录按钮
    private UserHelper mUserListDB;

    public interface OnConfirmClickListener {
        void doConfirm(boolean isPass);
    }

    public LoginDialog(Context context) {
        super(context, R.style.MyDialog);
        this.context = context;
        mUserListDB = ExApplication.getUserListDB();
    }

    public LoginDialog setView(View customView) {
        this.customView = customView;
        return this;
    }

    public LoginDialog setConfirmButton(OnConfirmClickListener onClickListener) {
        this.onConfirmClickListener = onClickListener;
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        init();
    }

    public void init() {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.login_dialog, null);
        setContentView(view);

        mAccount = (EditText) findViewById(R.id.login_edit_account);
        mPwd = (EditText) findViewById(R.id.login_edit_pwd);
        mLoginButton = (Button) findViewById(R.id.login_btn_login);
        mLoginButton.setOnClickListener(new LoginDialogClickListener());

    }


    private class LoginDialogClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            int id = v.getId();
            switch (id) {
                case R.id.login_btn_login:
                    boolean isPass = false;
                    if (onConfirmClickListener != null) {
                        String userName = mAccount.getText().toString().trim();    //获取当前输入的用户名和密码信息
                        String userPwd = mPwd.getText().toString().trim();
                        if (mUserListDB.isUserNameExist(userName)) {
                            UserData userData = mUserListDB.queryUserDataByUserName(userName);
                            if (userData.getUserPwd().toString().equals(userPwd)) {
                                Toast.makeText(ExApplication.getContext(), "验证通过", Toast.LENGTH_SHORT).show();
                                isPass = true;
                            } else {
                                Toast.makeText(ExApplication.getContext(), "密码错误", Toast.LENGTH_SHORT).show();
                                isPass = false;
                            }

                        } else {
                            Toast.makeText(ExApplication.getContext(), "用户名不存在", Toast.LENGTH_SHORT).show();
                            isPass = false;
                        }
                    }
                    onConfirmClickListener.doConfirm(isPass);
                    break;
            }
        }
    }


}