package com.moons.serial.serialdemo;

import android.util.Log;

/**
 * Created by shoushou on 2018-2-9.
 */
public class SerialProtocol {
    private static final String TAG = "SerialProtocol";
    private static final int PROTOCOL_MIN_BYTES = 10;
    private static final int PROTOCOL_ERROR_LENGTH = 0;
    private static final int PROTOCOL_ERROR_FORMAT = 1;
    private static final int PROTOCOL_ERROR_VERSION = 2;
    private static final int PROTOCOL_ERROR_CRC = 2;
    private static final int TAG_FRONT = 0x7F;
    private static final int TAG_END = 0x7E;
    private static final int VER_NUM = 0x01;

    private SerialProtocolListener mListener;


    public SerialProtocol(SerialProtocolListener listener) {
        this.mListener = listener;
    }

    public void Input(byte[] buffer) {
        if (buffer.length < PROTOCOL_MIN_BYTES) {
            this.mListener.onPacketError(PROTOCOL_ERROR_LENGTH);
            return;
        }
        if ((buffer[0] != TAG_FRONT) || (buffer[buffer.length - 1] !=TAG_END )){
            this.mListener.onPacketError(PROTOCOL_ERROR_FORMAT);
            return;
        }
        int crc = cn.artosyn.artosynuvctest3.moons.Utils.crc16(buffer, 1, buffer.length - 3);
        Log.d(TAG, "crc is 0x" + Integer.toHexString(crc));
        int crc1 = ((buffer[buffer.length - 3] &0xFF)<<8) | (buffer[buffer.length - 2]&0xFF);
        crc1 &= 0x0000FFFF;
        Log.d(TAG, "crc1 is 0x" + Integer.toHexString(crc1));
        if (crc != crc1) {
            this.mListener.onPacketError(PROTOCOL_ERROR_CRC);
            return;
        }
        if (buffer[1] != VER_NUM) {
            this.mListener.onPacketError(PROTOCOL_ERROR_VERSION);
            return;
        }
        int cmd = buffer[4];
        int len = buffer[5];
        byte[]data = new byte[len];
        for (int i=0; i<len; i++) {
            data[i] = buffer[6 + i];
        }
        //int data = buffer[6];
        this.mListener.onPacketSuccess(cmd, data, len);

 //       if (buffer[3] != 0x00) {
 //           this.mListener.onPacketError(PROTOCOL_ERROR_FORMAT);
 //       }

    }

    public int getPacketBytes(int cmd, int data, byte[]buffer) {
        int crc;
        buffer[0] = TAG_FRONT;
        buffer[1] = VER_NUM;
        buffer[2] = (byte) cn.artosyn.artosynuvctest3.moons.Utils.getSeqNo();
        buffer[3] = (byte)0xFF;
        buffer[4] = (byte)cmd;
        buffer[5] = 1;
        buffer[6] = (byte)data;
        crc = cn.artosyn.artosynuvctest3.moons.Utils.crc16(buffer, 1, 7);
        buffer[7] = (byte)(crc >> 0x08);
        buffer[8] = (byte)(crc & 0xFF);
        buffer[9] = TAG_END;
        return 10;
    }

    public int getPacketBytes(int cmd, byte[] data, int len, byte[]buffer) {
        int crc;
        int i;
        buffer[0] = TAG_FRONT;
        buffer[1] = VER_NUM;
        buffer[2] = (byte) cn.artosyn.artosynuvctest3.moons.Utils.getSeqNo();
        buffer[3] = (byte)0xFF;
        buffer[4] = (byte)cmd;
        buffer[5] = (byte)len;
       for (i=0;i <len; i++) {
           buffer[6 + i] = data[i];
       }

        crc = cn.artosyn.artosynuvctest3.moons.Utils.crc16(buffer, 1,6 + len);
        buffer[6 + len] = (byte)(crc >> 0x08);
        buffer[6 + len + 1] = (byte)(crc & 0xFF);
        buffer[6 + len + 2] = TAG_END;
        return 6 + len + 3;
    }

}
