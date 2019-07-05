package com.moons.user;

import java.io.Serializable;

public class UserData implements Serializable {

    private String userName;//用户名
    private String userPwd; //用户密码

    private int userID;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String name) {
        userName = name;
    }

    public String getUserPwd() {
        return userPwd;
    }

    public void setUserPwd(String pwd) {
        userPwd = pwd;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int id) {
        userID = id;
    }


}
