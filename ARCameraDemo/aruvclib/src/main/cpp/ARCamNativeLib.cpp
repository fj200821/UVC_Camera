//
// Created by glfeng on 2019/4/25.
//

#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/bitmap.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

#ifdef __cplusplus
extern "C" {
#endif

#include "v4l2core.h"

#define TAG "ARCamNativeLib"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,TAG ,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,TAG ,__VA_ARGS__)

extern "C" JNIEXPORT jlong JNICALL
Java_cn_artosyn_aruvclib_ARNativeHelper_openUVCDev(
        JNIEnv *env,
        jobject /* this */,
        jstring dname) {

    v4l2_dev_t* vd = NULL;
    const char *c_dname = env->GetStringUTFChars(dname, JNI_FALSE);
    //open device
    vd = v4l2core_dev_open(c_dname);
    env->ReleaseStringUTFChars(dname, c_dname);
    if(vd == NULL)
    {
        puts("Can't open video device.");
        return 0;
    }
    v4l2core_dev_init(vd);
    return jlong(vd);
}

extern "C" JNIEXPORT void JNICALL
Java_cn_artosyn_aruvclib_ARNativeHelper_closeUVCDev(
        JNIEnv *env,
        jobject /* this */,jlong lp) {

    v4l2_dev_t* vd = (v4l2_dev_t*)lp;
    if(vd==NULL)
        return;
    vd->bcapture = 0;
    v4l2core_dev_uninit(vd);
    v4l2core_dev_clean(vd);
    vd = NULL;
    return;
}

extern "C" JNIEXPORT jint JNICALL
Java_cn_artosyn_aruvclib_ARNativeHelper_getCamWidth(
        JNIEnv *env,
        jobject /* this */,jlong lp) {

    v4l2_dev_t* vd = (v4l2_dev_t*)lp;
    if(!vd)
    {
        return -1;
    }
    return vd->width;
}

extern "C" JNIEXPORT jint JNICALL
Java_cn_artosyn_aruvclib_ARNativeHelper_getCamHigh(
        JNIEnv *env,
        jobject /* this */,jlong lp) {

    v4l2_dev_t* vd = (v4l2_dev_t*)lp;
    if(!vd)
    {
        return -1;
    }
    return vd->height;
}

extern "C" JNIEXPORT jint JNICALL
Java_cn_artosyn_aruvclib_ARNativeHelper_startCapture(
        JNIEnv *env,
        jobject obj/* this */,jlong lp) {

    v4l2_dev_t* vd = (v4l2_dev_t*)lp;
    int ret =0;
    vd->bcapture = 1;

    if(vd->width<=0||vd->height<=0)
    {
        puts("UVCCaptureThread:no width or width.");
        return -1;
    }

    //int ret;
    ret = v4l2core_capture_start(vd);
    if(ret<0)
    {
        return ret;
    }
    return 0;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_cn_artosyn_aruvclib_ARNativeHelper_getFrame(
        JNIEnv *env,
        jobject obj/* this */, jobject retObj,jlong lp) {
    v4l2_dev_t* vd = (v4l2_dev_t*)lp;
    if (!vd || vd->bcapture == 0)
        return NULL;
    int ret = -1;
    ret = v4l2core_capture_getframe(vd);
    char error[255];
    if (ret == 1) {
        jbyteArray jarray = env->NewByteArray(vd->frame.size);
        env->SetByteArrayRegion(jarray, 0, vd->frame.size, (jbyte *) vd->frame.buf);
        jclass clazz = env->GetObjectClass(retObj);
        jfieldID param_size = env->GetFieldID(clazz, "size", "I");
        jfieldID param_index = env->GetFieldID(clazz, "frame_index", "I");
        jfieldID param_type = env->GetFieldID(clazz, "data_type", "I");
        env->SetIntField(retObj, param_size, vd->frame.size);
        env->SetIntField(retObj, param_index, vd->frame.index);
        env->SetIntField(retObj, param_type, 1);
        env->DeleteLocalRef(clazz);
        //env->DeleteLocalRef(jarray);
        return jarray;
    } else if (ret == 2) {
        jbyteArray jarray = env->NewByteArray(vd->sdata.size);
        env->SetByteArrayRegion(jarray, 0, vd->sdata.size, (jbyte *) vd->sdata.buf);
        jclass clazz = env->GetObjectClass(retObj);
        jfieldID param_size = env->GetFieldID(clazz, "size", "I");
        jfieldID param_index = env->GetFieldID(clazz, "frame_index", "I");
        jfieldID param_type = env->GetFieldID(clazz, "data_type", "I");
        env->SetIntField(retObj, param_size, vd->sdata.size);
        env->SetIntField(retObj, param_index, 0);
        env->SetIntField(retObj, param_type, 2);
        env->DeleteLocalRef(clazz);
        //env->DeleteLocalRef(jarray);
        return jarray;
    }
    return NULL;
}

extern "C" JNIEXPORT void JNICALL
Java_cn_artosyn_aruvclib_ARNativeHelper_stopCapture(
        JNIEnv *env,
        jobject /* this */,jlong lp) {
    v4l2_dev_t* vd = (v4l2_dev_t*)lp;
    vd->bcapture = 0;
    if(!vd)
        return;
    v4l2core_capture_stop(vd);
    //MJPGDecoder_destroy();
    SAFEFREE(vd->img.buf);
}

extern "C" JNIEXPORT jintArray JNICALL
Java_cn_artosyn_aruvclib_ARNativeHelper_getDevs(
        JNIEnv *env,
        jobject/* this */) {

    char dev_name[16];
    struct stat st;
    int i;
    int count = 0;
    int dev_indexs[32];

    for(i=0 ; i<32 ; i++){
        memset(dev_name,0, sizeof(dev_name));
        sprintf(dev_name,"/dev/video%d",i);
        if (-1 == stat (dev_name, &st)) {
            //nothing to do
        }else{
            dev_indexs[count] = i;
            count++;
        }
    }

    jintArray jarray = env->NewIntArray(count);
    env->SetIntArrayRegion(jarray, 0, count, (jint*)dev_indexs);

    return jarray;
}

extern "C" JNIEXPORT void JNICALL
Java_cn_artosyn_aruvclib_ARNativeHelper_drawBitmap(JNIEnv *env, jobject obj, jobject surface, jobject bitmap) {

    AndroidBitmapInfo info;
    int32_t result;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        //ThrowException(env, "java/lang/RuntimeException", "unable to get bitmap info");
        LOGE("AndroidBitmap_getInfo fail");
        return;
    }

    char *data = NULL;
    if (AndroidBitmap_lockPixels(env, bitmap, (void **) &data) < 0) {
        LOGE("AndroidBitmap_lockPixels fail");
        return;
    }

    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    if (NULL == window) {
        LOGE("ANativeWindow_fromSurface fail");
        goto RET2;
    }
    result = ANativeWindow_setBuffersGeometry(window, info.width, info.height,WINDOW_FORMAT_RGBA_8888);
    if (result < 0) {
        LOGE("ANativeWindow_setBuffersGeometry fail");
        goto RET;
    }
    ANativeWindow_acquire(window);

    ANativeWindow_Buffer buffer;
    if (ANativeWindow_lock(window, &buffer, NULL) < 0) {
        LOGE("ANativeWindow_lock fail");
        goto RET;
    }

    if (buffer.width == buffer.stride) {
        memcpy(buffer.bits, data, buffer.width * buffer.height * 4);
    } else {
        for (int h = 0; h < buffer.height; h++)
        {
            memcpy(((char *)buffer.bits + h * buffer.stride*4), data + h * buffer.width*4,buffer.width*4);
        }
    }

    if (ANativeWindow_unlockAndPost(window) < 0) {
        LOGE("ANativeWindow_unlockAndPost fail");
    }

RET:
    ANativeWindow_release(window);
    window = NULL;
RET2:
    if (AndroidBitmap_unlockPixels(env, bitmap) < 0) {
        LOGE("AndroidBitmap_unlockPixels fail");
    }
}


#ifdef __cplusplus
}
#endif

