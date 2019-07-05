package com.moons.util;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

public class DensityUtil {

	private static final String TAG = "DensityUtil";
	public static float scale_ =1;
	public static float  init(Context context)
	{
		
		 scale_ = context.getResources().getDisplayMetrics().density;
		 return scale_;
	}
	

	public static int getScreenHeight(Activity activity) {
		return activity.getWindowManager().getDefaultDisplay().getHeight();
	}

	public static int getScreenWidth(Activity activity) {
		return activity.getWindowManager().getDefaultDisplay().getWidth();
	}

	/**
	 * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
	 */
	public static int dip2px( float dpValue) {
		if(scale_==0)
		{
			Log.e(TAG,"you forget to ini DensityUtil!!! ");
		}
		return (int) (dpValue * scale_ + 0.5f);
	}

	/**
	 * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
	 */
	public static int px2dip(float pxValue) {
		if(scale_==0)
		{
			Log.e(TAG,"you forget to ini DensityUtil!!! ");
		}
		return (int) (pxValue / scale_ + 0.5f);//float0.5
	}

	
	
	
	
	//单位是px
	//base机型:1280x720,密度160
	//适配机型1920x1080,密度320
	public static int px_720dip1080(int pxValue) {
		
		return (int) (pxValue / scale_ + 0.5f);
	}
	
	
	
	
	
	
	
}