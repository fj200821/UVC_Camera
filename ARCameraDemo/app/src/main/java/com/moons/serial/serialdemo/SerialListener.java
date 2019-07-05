package com.moons.serial.serialdemo;

/**
 * Created by shoushou on 2018-2-9.
 */
public interface SerialListener {
    public void onReceiveTimeout();
    public void onReceiveError();
    public void onDataAvailable();

}
