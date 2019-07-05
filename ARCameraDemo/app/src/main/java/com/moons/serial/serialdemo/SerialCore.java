package com.moons.serial.serialdemo;

import java.io.IOException;

/**
 * Created by shoushou on 2018-2-8.
 */
public class SerialCore {
    private static final String TAG = "SerialCore";
    private static final int DEFAULT_SERIAL_NUM = 0;
    private long mSerialContext;
    private int mSerialNum;
    private boolean running;
    private SerialListener mListener;

    public SerialCore(SerialListener listener) throws IOException {
        this.mSerialNum = DEFAULT_SERIAL_NUM;
        this.mListener = listener;
        try {
            SerialInit();
        } catch (IOException e) {
            throw e;
        }
    }

    public int write(byte[]buffer, int length) {
        return write(mSerialContext, buffer, length);
    }

    public byte[] read() {
        return read(mSerialContext);
    }

    private void SerialInit() throws IOException {
        boolean result = false;
        this.mSerialContext = allocContext();
        if (this.mSerialContext== 0) {
            throw new IOException("Serial Context Alloc Failure!");
        }
        result = open(mSerialContext,mSerialNum );
        if (!result) {
            throw new IOException("Could not open Serial" + mSerialNum + ", Please Check you hardware");
        }
        Thread pollThread = new Thread(mSerialRunnable);
        this.running = true;
        pollThread.start();
    }

    private Runnable mSerialRunnable = new Runnable() {
        int result;
        @Override
        public void run() {
            while (running) {
                result = poll(mSerialContext);

                switch (result) {
                    case 0:
                        mListener.onReceiveTimeout();
                        break;
                    case 1:
                        mListener.onDataAvailable();
                        break;
                    default:
                        mListener.onReceiveError();
                        break;
                }
            }
        }
    };

//    public static void nativeTest() {
//        Log.d(TAG, "try allocContext");
//        mSerialConext = allocContext();
//
//        Log.d(TAG, "retrun " + mSerialConext );
//        Log.d(TAG, "try releaseContext");
//        boolean result = releaseContext(mSerialConext);
//        Log.d(TAG, "retrun " + result);
//
//    }

    private static native long allocContext();
    private static native boolean open(long context,int num);
    private static native boolean close(long context);
    private static native int poll(long context);
    private static native byte[] read(long context);
    private static native int write(long context,byte[] buffer, int length);
    private static native boolean releaseContext(long context);

    static {
        System.loadLibrary("serialcore-jni");
    }
}
