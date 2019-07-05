package com.moons.serial.serialdemo;

/**
 * Created by shoushou on 2018-2-9.
 */
public interface SerialProtocolListener {
    public void onPacketError(int errorNo);
    public void onPacketSuccess(int cmd, byte[] data, int len);
}
