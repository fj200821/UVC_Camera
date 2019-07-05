package com.moons.serial.serialdemo;

import android.util.Log;

import java.io.IOException;

public class DoorLockManager {
    protected static final String TAG = "DoorLockManager";
    private static DoorLockManager mDoorLockManager_ = null;
    private SerialCore mSerial=null;

    private DoorLockManager() {

    }

    public static DoorLockManager getInstance() {
        if (mDoorLockManager_ == null) {
            mDoorLockManager_ = new DoorLockManager();
        }
        return mDoorLockManager_;
    }

    public void initSerial() {
        try {
            mSerial = new SerialCore(listener);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void openLock() {
        if(mSerial==null){
            initSerial();
        }
        byte[] buffer = new byte[9];
        int len;
        buffer[0] = 0x55;
        buffer[1] = 0x55;
        buffer[2] = 0x00;
        buffer[3] = 0x31;
        buffer[4] = (byte) 0xff;
        buffer[5] = (byte) 0xff;
        buffer[6] = (byte) 0xff;
        buffer[7] = (byte) 0xf5;
        buffer[8] = (byte) 0xf5;
        len = mSerial.write(buffer, buffer.length);//串口写
    }


    public void watchDog() {
        byte[] buffer = new byte[9];
        int len;
        buffer[0] = 0x55;
        buffer[1] = 0x55;
        buffer[2] = 0x00;
        buffer[3] = 0x54;
        buffer[4] = (byte) 0x01;//主机自定义数据
        buffer[5] = (byte) 0x02;//主机自定义数据
        buffer[6] = (byte) 0xff;
        buffer[7] = (byte) 0xf5;
        buffer[8] = (byte) 0xf5;
        len = mSerial.write(buffer, buffer.length);//串口写
    }



    private SerialListener listener = new SerialListener() {
        @Override
        public void onReceiveTimeout() {
                       Log.i(TAG, "onReceiveTimeout");
        }

        @Override
        public void onReceiveError() {
                      Log.i(TAG, "onReceiveError");

        }

        @Override
        public void onDataAvailable() {
            Log.i(TAG, "onDataAvailable");
            byte[] buffer = mSerial.read();//串口读
            Log.i(TAG, "received " + buffer.length + " datas");
            int len = buffer.length;
            StringBuilder buf = new StringBuilder(len * 5);
            for (byte b : buffer) {
                buf.append(String.format("0x%02x ", new Integer(b & 0xff)));
            }
            Log.i(TAG, buf.toString());
                 if(buf.toString().contains("0x54")){
                     watchDog();
                 }
        }
    };



}
