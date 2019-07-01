package cn.artosyn.aruvclib;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ARUtil {
    public static void int2bytes(byte[] barray,int offset, int a) {
        if (barray == null || barray.length < 4) {
            return;
        }
        long lv = (long)a;
        barray[offset+3] = (byte) ((lv >> 24) & 0xff);
        barray[offset+2] = (byte) ((lv >> 16) & 0xff);
        barray[offset+1] = (byte) ((lv >> 8) & 0xff);
        barray[offset+0] = (byte) ((lv >> 0) & 0xff);
    }

    public static int bytes2int(byte[] barray,int offset){
        //int a = 0;
        //a = (int)barray[offset]+(int)barray[offset+1]<<8+(int)barray[offset+2]<<16+(int)barray[offset+3]<<24;
        int ret=0;
        for (int i=3; i>=0 && i+offset<barray.length; i--) {
            ret <<= 8;
            ret |= (int)barray[offset+i] & 0xFF;
        }
        return ret;
        //return a;
    }

    public static float bytes2float(byte[] barray,int offset){
        //float ret = ByteBuffer.wrap(Arrays.copyOfRange(barray,offset,offset+4)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        float ret = ByteBuffer.wrap(barray,offset,4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        return ret;
    }

//    public static int bytes2int(byte[] barray,int offset){
//        int ret = ByteBuffer.wrap(Arrays.copyOfRange(barray,offset,offset+4)).order(ByteOrder.LITTLE_ENDIAN).getInt();
//        return  ret;
//    }

    public static void short2bytes(byte[] barray,int offset, int a){
        if (barray == null || barray.length < 2) {
            return;
        }
        barray[offset+1] = (byte) ((a >> 8) & 0xff);
        barray[offset+0] = (byte) ((a >> 0) & 0xff);
    }

    public static short bytes2short(byte[] barray,int offset){
        short ret = 0;
        ret |= barray[offset+1]&0xff;
        ret <<=8;
        ret |= barray[offset]&0xff;
        return ret;
    }

    public static void long2bytes(byte[] barray,int offset, long a){
        if (barray == null || barray.length < 8) {
            return;
        }
        barray[offset+7] = (byte) ((a >> 56) & 0xff);
        barray[offset+6] = (byte) ((a >> 48) & 0xff);
        barray[offset+5] = (byte) ((a >> 40) & 0xff);
        barray[offset+4] = (byte) ((a >> 32) & 0xff);
        barray[offset+3] = (byte) ((a >> 24) & 0xff);
        barray[offset+2] = (byte) ((a >> 16) & 0xff);
        barray[offset+1] = (byte) ((a >> 8) & 0xff);
        barray[offset+0] = (byte) ((a >> 0) & 0xff);
    }

    public static long intbytes2long(byte[] barray,int offset){
        long ret=0;
        for (int i=3; i>=0 && i+offset<barray.length; i--) {
            ret <<= 8;
            ret |= (long)barray[offset+i] & 0xFF;
        }
        return ret;
    }

    public static int intbytes2int(byte[] barray,int offset){
        int ret=0;
        for (int i=3; i>=0 && i+offset<barray.length; i--) {
            ret <<= 8;
            ret |= (int)barray[offset+i] & 0xFF;
        }
        return ret;
    }

    public static long longbytes2long(byte[] barray,int offset){
        long ret=0;
        for (int i=7; i>=0 && i+offset<barray.length; i--) {
            ret <<= 8;
            ret |= (long)barray[offset+i] & 0xFF;
        }
        return ret;
    }

    public static long checkSum2long(byte[] data,int psize,int poff){
        long a=0;
        for(int i=0;i<psize;i++)
        {
            a+=data[poff+i]&0xFF;
        }
        return a;
    }

    public static void checkSum(byte[] data,int psize,int poff,int coff)
    {
        long lcs = 0;
        for(int i=0;i<psize;i++)
        {
            lcs+=(int)data[poff+i];
        }
        data[coff+3] = (byte) ((lcs >> 24) & 0xff);
        data[coff+2] = (byte) ((lcs >> 16) & 0xff);
        data[coff+1] = (byte) ((lcs >> 8) & 0xff);
        data[coff+0] = (byte) ((lcs >> 0) & 0xff);
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
    }

    public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:",b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString().toLowerCase();
            }
        } catch (Exception ex) {
        }
        return "02:00:00:00:00:00";
    }

    public static float GetVectorLength(float[] v){
        float len = 0;
        for (float vi : v) {
            len += Math.pow(vi,2);
        }
        len = (float)Math.sqrt(len);
        return  len;
    }

    public static float calcCosValue(float[] v1,float[] v2,int length,float threshold)
    {
        float top = 0,bottom1=0,bottom2 =0;
        for(int i =0; i < length; i++)
        {
            top+=v1[i]*v2[i];
            bottom1 += Math.pow(v1[i],2);
            bottom2 += Math.pow(v2[i],2);
        }
        if(bottom1 == 0 || bottom2 == 0)
            return Float.MIN_NORMAL;
        return (top / (float)(Math.sqrt(bottom1)*Math.sqrt(bottom2)));
    }

    public static float calcCosValue_accel(float[] v1,float v1_len,float[] v2,float v2_len,int length){
        float top = 0;
        for(int i =0; i < length; i++)
        {
            top+=v1[i]*v2[i];
        }
        if(v1_len == 0 || v2_len == 0)
            return Float.MIN_NORMAL;
        return top / (v1_len*v2_len);
    }

    static Bitmap GetScaleBitmap(String path,int out_w,int out_h){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        BitmapFactory.decodeFile(path, options);
        if(options.outHeight<0||options.outWidth<0){
            return null;
        }
        int i = GetSampleSize(options, out_w, out_h);
        options.inSampleSize = i;
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    public static Bitmap GetScaleBitmap(Bitmap image, int maxWidth, int maxHeight) {
        if (image == null)
            return null;
        if (maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > ratioBitmap) {
                finalWidth = (int) ((float) maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float) maxWidth / ratioBitmap);
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
            return image;
        } else {
            return image;
        }
    }

    static int GetSampleSize(BitmapFactory.Options options, int destWidth, int destHeight) {
        int h = options.outHeight;
        int w = options.outWidth;
        int scaleWidth = (int) Math.ceil((double) ((float) w / (float) destWidth));
        int scaleHeight = (int) Math.ceil((double) ((float) h / (float) destHeight));
        return Math.max(scaleWidth, scaleHeight);
    }

    public static boolean SaveBitmap(Bitmap bitmap, File file) {
        if (bitmap == null) {
            return false;
        } else {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                fos.flush();
                boolean var3 = true;
                return var3;
            } catch (Exception var13) {
                var13.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException var12) {
                        var12.printStackTrace();
                    }
                }

            }
            return false;
        }
    }

    public static void safeReleaseBitmap(Bitmap bitmap){
        if(bitmap!=null&&!bitmap.isRecycled())
            bitmap.recycle();
    }

    public static String readFile2Str(File file){
        String sret = "";
        if(!file.exists()||!file.canRead()){
            return sret;
        }
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            return sret;
        }
        return text.toString();
    }

    public static boolean writeStr2File(String data,File file){
        FileOutputStream f =null;
        try {
            f= new FileOutputStream(file);
            f.write(data.getBytes());
            f.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static byte[] floatArr2byteArr(float[] fArr){
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(bas);
        for (float f : fArr) {
            try {
                ds.writeFloat(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bas.toByteArray();
    }

    public static float[] byteArr2floatArr(byte[] bArr){
        ByteArrayInputStream bas = new ByteArrayInputStream(bArr);
        DataInputStream ds = new DataInputStream(bas);
        float[] fArr = new float[bArr.length / 4];  // 4 bytes per float
        for (int i = 0; i < fArr.length; i++)
        {
            try {
                fArr[i] = ds.readFloat();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return fArr;
    }
}
