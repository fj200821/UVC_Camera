package com.moons.login;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.moons.user.UserData;
import com.moons.user.UserHelper;

import cn.artosyn.artosynuvctest3.R;
import cn.artosyn.artosynuvctest3.activity.base.ExApplication;

public class ResetpwdActivity extends Activity {
    private EditText mAccount;  //用户名编辑
    private EditText mPwd_old;    //密码编辑
    private EditText mPwd_new;    //密码编辑
    private EditText mPwdCheck;   //密码编辑
    private Button mSureButton;   //确定按钮
    private Button mCancelButton; //取消按钮
    private UserHelper mUserListDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.resetpwd);
        initViews();
        setEventListener();
    }

    private void initViews() {
        mAccount = (EditText) findViewById(R.id.resetpwd_edit_name);
        mPwd_old = (EditText) findViewById(R.id.resetpwd_edit_pwd_old);
        mPwd_new = (EditText) findViewById(R.id.resetpwd_edit_pwd_new);
        mPwdCheck = (EditText) findViewById(R.id.resetpwd_edit_pwd_check);
        mSureButton = (Button) findViewById(R.id.resetpwd_btn_sure);
        mCancelButton = (Button) findViewById(R.id.resetpwd_btn_cancel);
        mUserListDB = ExApplication.getUserListDB();
    }

    private void setEventListener() {
        mSureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modify();
            }
        });
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ResetpwdActivity.this.finish();
            }
        });
    }

    private void modify() {
        String userName = mAccount.getText().toString().trim();
        String userPwd_old = mPwd_old.getText().toString().trim();
        String userPwd_new = mPwd_new.getText().toString().trim();
        String userPwdCheck = mPwdCheck.getText().toString().trim();
        if (!mUserListDB.isUserNameExist(userName)) {
            Toast.makeText(this, "用户名不存在", Toast.LENGTH_SHORT).show();
        } else {
            UserData userData = mUserListDB.queryUserDataByUserName(userName);
            if (!userData.getUserPwd().equals(userPwd_old)) {
                Toast.makeText(this, "密码错误", Toast.LENGTH_SHORT).show();
            } else {
                //旧的用户名和密码都正确
                checkNewPassword(userName,userPwd_new, userPwdCheck);
            }
        }


    }

    private void checkNewPassword(String userName,String userPwd_new, String userPwdCheck) {
        if (userPwd_new.equals("") || userPwdCheck.equals("")) {
            Toast.makeText(this, "新密码不能为空", Toast.LENGTH_SHORT).show();
        } else {
            if (userPwd_new.equals(userPwdCheck)) {
                 updatePassword(userName,userPwd_new);
            } else {
                Toast.makeText(this, "新密码不一致，请检查", Toast.LENGTH_SHORT).show();
            }
        }


    }

    private void updatePassword(String userName, String userPwd_new) {
        UserData userData=new UserData();
        userData.setUserName(userName);
        userData.setUserPwd(userPwd_new);
        mUserListDB.updateUserData(userData);
        Toast.makeText(this, "密码修改成功", Toast.LENGTH_SHORT).show();
        this.finish();
    }




}

