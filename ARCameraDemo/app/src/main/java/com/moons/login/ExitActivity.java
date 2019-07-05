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

public class ExitActivity extends Activity {

    private EditText mAccount;                        //用户名编辑
    private EditText mPwd;                            //密码编辑
    private Button mExitButton;                      //退出按钮
    private String userNameValue, passwordValue;
    private UserHelper mUserListDB;
    public static final int RESULT_OK = -1;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.exit);
        //通过id找到相应的控件
        mAccount = (EditText) findViewById(R.id.login_edit_account);
        mPwd = (EditText) findViewById(R.id.login_edit_pwd);
        mExitButton = (Button) findViewById(R.id.login_btn_login);
        setEventListener();
        mUserListDB = ExApplication.getUserListDB();
    }

    private void setEventListener() {
        mExitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exit();
            }
        });
    }


    public void exit() {
        String userName = mAccount.getText().toString().trim();    //获取当前输入的用户名和密码信息
        String userPwd = mPwd.getText().toString().trim();
        if (mUserListDB.isUserNameExist(userName)) {
            UserData userData = mUserListDB.queryUserDataByUserName(userName);
            if (userData.getUserPwd().toString().equals(userPwd)) {
                Toast.makeText(this, "验证通过", Toast.LENGTH_SHORT).show();
                this.setResult(RESULT_OK);
                this.finish();
            } else {
                Toast.makeText(this, "密码错误", Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(this, "用户名不存在", Toast.LENGTH_SHORT).show();
        }
    }


}









