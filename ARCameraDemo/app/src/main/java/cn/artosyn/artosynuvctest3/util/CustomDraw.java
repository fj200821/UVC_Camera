package cn.artosyn.artosynuvctest3.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.SurfaceView;

import cn.artosyn.artosynuvctest3.config.DemoConfig;
import cn.artosyn.artosynuvctest3.facedata.DataSync;
import cn.artosyn.artosynuvctest3.facedata.FaceDataUtil;
import cn.artosyn.aruvclib.ARCommon;
import cn.artosyn.aruvclib.model.ARUserFace;

public class CustomDraw {

    private static Paint paint;
    private static int fontSize = 15;
    private static int rectSize = 4;

    public static void clearOverlay(SurfaceView outputView){
        Canvas canvas = null;
        try {
            canvas = outputView.getHolder().lockCanvas();
            if (canvas == null) return;
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                outputView.getHolder().unlockCanvasAndPost(canvas);
            }
            catch (Exception e){
                Log.e("SurfaceDraw","can not unlock canvas");
            }
        }
    }

    public static void drawOverlay(ARCommon.CustomData customData, SurfaceView outputView, int view_width, int view_height){
        if(customData==null||customData.boxFeatureDataList.isEmpty())
            return;
        Canvas canvas = null;
        try {
            canvas = outputView.getHolder().lockCanvas();
            if (canvas == null) return;
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            if (paint == null) {
                paint = new Paint();
                paint.setAntiAlias(true);
            }

            for (ARCommon.BoxFeatureData faces : customData.boxFeatureDataList) {
                if (faces == null) continue;
                //Log.d("*********",faces.boxRect.toString());
//                //人脸位置坐标
//                float[] rect = faces.getRect();
//                //xy坐标点
//                FacePoint xy = getXY(mConfig, rect, cameraId, scale_bit, viewW, viewH, mConfig.specialCameraLeftRightReverse, mConfig.specialCameraTopDownReverse);
//                //人脸框宽度
//                xy.mWidth = rect[2] * scale_bit;
//                //防止人脸框抖动
//                xy = adjustView(xy, faces.getTrackId());

//                //绘制人脸关键点
//                if (showPoint) {
//                    drawPoints(faces.getLandmarks(), canvas, scale_bit, viewW);
//                }
//                //判断是否识别
//                FaceInfo faceInfo = drawRecog(faces, null, userMap);
//                //判断活体
//                faceInfo = faceLiveness(faces, faceInfo);

                paint.reset();
                paint.setTextSize(fontSize);
                Paint.FontMetrics fontMetrics = paint.getFontMetrics();
                //每一行文字的高度
                float fHeight = fontMetrics.bottom - fontMetrics.top;
                //绘画人脸框
                Rect rect_r = rectMapping(faces.boxRect,customData.width,customData.height,view_width,view_height);
                //Rect rect_r = new Rect(10,10,100,100);
                drawRect(canvas, rect_r,faces, fHeight, view_width, view_height);

//                if (mConfig.isDrawIr) {
//                    viewW = (int) (viewW * zoom);
//                    viewH = (int) (viewH * zoom);
//                    rect = faces.getRect(); //人脸位置坐标
//                    xy.mWidth = (float) (rect[2] * scale_bit * zoom); //人脸框宽度
//                    xy = getXY(mConfig, rect, cameraId, (float) (scale_bit * zoom), viewW, viewH, mConfig.specialCameraLeftRightReverse, mConfig.specialCameraTopDownReverse);
//                    drawRect(canvas, xy.mX, xy.mY, xy.mWidth, Color.WHITE);
//                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                outputView.getHolder().unlockCanvasAndPost(canvas);
            }
            catch (Exception e){
                Log.e("SurfaceDraw","can not unlock canvas");
            }
        }
    }

    private static void drawRect(Canvas canvas, Rect rect_face, ARCommon.BoxFeatureData faceInfo, float fontH, float viewW, float viewH) {
        //if (faceInfo == null) return;
        //人脸框四角
        paint.setStrokeWidth(rectSize);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);

        int x1 = rect_face.left;
        int y1 = rect_face.top;
        int rect_width = rect_face.width();
        int rect_height = rect_face.height();

        float rw = rect_width / 10;
        float rh = rect_height / 10;

        Path path = new Path();
        path.moveTo(x1, y1 + rw);
        path.lineTo(x1, y1);
        path.lineTo(x1 + rw, y1);
        path.moveTo(x1 + rect_width - rw, y1);
        path.lineTo(x1 + rect_width, y1);
        path.lineTo(x1 + rect_width, y1 + rw);
        path.moveTo(x1, y1 + rect_height - rw);
        path.lineTo(x1, y1 + rect_height);
        path.lineTo(x1 + rw, y1 + rect_height);
        path.moveTo(x1 + rect_width - rw, y1 + rect_height);
        path.lineTo(x1 + rect_width, y1 + rect_height);
        path.lineTo(x1 + rect_width, y1 + rect_height - rw);
        canvas.drawPath(path, paint);

        //人脸框虚线
        paint.setStrokeWidth(rectSize / 4);
        paint.setPathEffect(new DashPathEffect(new float[]{15, 15, 15, 15}, 1));
        RectF rectf = new RectF(x1, y1, x1 + rect_width, y1 + rect_height);
        canvas.drawRect(rectf, paint);
        if(!DemoConfig.instance().isLiveDetect){
            ARUserFace userFace = DataSync.userFaces_ident.get(faceInfo.face_id);
            if(userFace==null)
                return;
        }
        int num = 3;
        drawFaceInfo(canvas,faceInfo, x1, y1, rect_width, fontH * 10, fontH * num, fontH, viewW, viewH);
    }

    private static void drawFaceInfo(Canvas canvas, ARCommon.BoxFeatureData faceInfo, float x1, float y1, float rect_width, float w, float h, float fontH, float viewW, float viewH) {
        paint.reset();
        //人物信息卡
        paint.setStrokeWidth(rectSize / 4);
        paint.setPathEffect(new DashPathEffect(new float[]{0, 0, 0, 0}, 1));
        paint.setStyle(Paint.Style.FILL);
        if (true/*faceInfo.isRecog == 1 || faceInfo.isLiveness == 1*/) {
            paint.setColor(Color.parseColor("#1E90FF"));
        } else {
            paint.setColor(Color.parseColor("#1E90FF"));
        }
        paint.setAlpha(90);
        Path path = new Path();
        float left;
        float right;
        float top;
        float bottom;
        float distance = fontH /2;
        //top
        left = x1;
        right = left + w;
        top = y1 - h - distance;
        bottom = y1 - distance;
        if (top > distance - fontH) {
            canvas.drawRoundRect(new RectF(left, top, right, bottom), 20, 20, paint);
            //canvas.drawCircle(left + (right - left) / 4, bottom + 5 * distance / 6, distance / 6, paint);
            //canvas.drawCircle(left + (right - left) / 3, bottom + 1 * distance / 2, distance / 4, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.WHITE);
            canvas.drawRoundRect(new RectF(left, top, right, bottom), 20, 20, paint);
            //canvas.drawCircle(left + (right - left) / 4, bottom + 5 * distance / 6, distance / 6, paint);
            //canvas.drawCircle(left + (right - left) / 3, bottom + 1 * distance / 2, distance / 4, paint);
//            if (true/*faceInfo.isRecog != -1 || faceInfo.isLiveness != -1*/) {
//                //绘画笑脸
//                drawSmile(canvas, top, left, right, bottom, fontH, true);
//            } else {
//                drawSmile(canvas, top, left, right, bottom, fontH, false);
//            }
        } else {
            //left
            left = x1 - distance - w;
            right = left + w;
            top = y1;
            bottom = top + h;
            if (left > distance - fontH) {
                canvas.drawRoundRect(new RectF(left, top, right, bottom), 20, 20, paint);
//                canvas.drawCircle(right + distance * 5 / 6, (y1 + rect_width - bottom) * 2 / 3 + bottom, distance / 6, paint);
//                canvas.drawCircle(right, (y1 + rect_width - bottom) / 4 + bottom, distance / 4, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.WHITE);
                canvas.drawRoundRect(new RectF(left, top, right, bottom), 20, 20, paint);
//                canvas.drawCircle(right + distance * 5 / 6, (y1 + rect_width - bottom) * 2 / 3 + bottom, distance / 6, paint);
//                canvas.drawCircle(right, (y1 + rect_width - bottom) / 4 + bottom, distance / 4, paint);
            } else {
                //right
                left = x1 + rect_width + distance;
                right = left + w;
                top = y1;
                bottom = y1 + h;
                if (viewW - right > distance - fontH) {
                    canvas.drawRoundRect(new RectF(left, top, right, bottom), 20, 20, paint);
//                    canvas.drawCircle(left - distance / 6, (y1 + rect_width - bottom) * 2 / 3 + bottom, distance / 6, paint);
//                    canvas.drawCircle(left - distance / 2, (y1 + rect_width - bottom) / 4 + bottom, distance / 4, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(Color.WHITE);
                    canvas.drawRoundRect(new RectF(left, top, right, bottom), 20, 20, paint);
//                    canvas.drawCircle(left - distance / 6, (y1 + rect_width - bottom) * 2 / 3 + bottom, distance / 6, paint);
//                    canvas.drawCircle(left - distance / 2, (y1 + rect_width - bottom) / 4 + bottom, distance / 4, paint);
                } else {
                    //bottom
                    left = x1;
                    right = left + w;
                    top = y1 + rect_width + distance;
                    bottom = top + h;
                    canvas.drawRoundRect(new RectF(left, top, right, bottom), 20, 20, paint);
//                    canvas.drawCircle(left + (right - left) / 4, top - distance * 5 / 6, distance / 6, paint);
//                    canvas.drawCircle(left + (right - left) / 3, top - distance / 2, distance / 4, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(Color.WHITE);
                    canvas.drawRoundRect(new RectF(left, top, right, bottom), 20, 20, paint);
//                    canvas.drawCircle(left + (right - left) / 4, top - distance * 5 / 6, distance / 6, paint);
//                    canvas.drawCircle(left + (right - left) / 3, top - distance / 2, distance / 4, paint);
                }
            }
        }

        //人物信息卡边框
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        canvas.drawPath(path, paint);
        //绘画人脸信息
        paint.reset();
        //人物信息内容
        paint.setTextSize(fontSize);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(0);
        paint.setStyle(Paint.Style.FILL);
//        String personId = paint.measureText("FaceId:" + faceInfo.face_id) > w ? ("FaceId:" +
//                faceInfo.face_id).substring(0, 7) + "..." : "FaceId:" + faceInfo.face_id;
        int toph = 1;
        ARUserFace userFace = DataSync.userFaces_ident.get(faceInfo.face_id);
        String name = "",ageAndmGender = "";
        if(userFace!=null){
            String name_ori = "姓名:" + userFace.name;
            name = paint.measureText(name_ori) > w ? (name_ori).substring(0, 7) + "..." : name_ori;
            String ag_ori = "年龄:" + userFace.age + "   性别:" + userFace.gender;
            ageAndmGender = paint.measureText(ag_ori) > w ? (ag_ori).substring(0, 7)+ "..." : ag_ori;
            drawInfoText(canvas, name, Color.WHITE, left + 20, top + toph++ * fontH, paint);
            drawInfoText(canvas, ageAndmGender, Color.WHITE, left + 20, top + toph++ * fontH, paint);
        }

        //String isRecog = "" + (paint.measureText("未开启识别") > w ? ("未开启识别").substring(0, 7) + "..." : "未开启识别");

        //drawInfoText(canvas, personId, Color.WHITE, left + 20, top + toph++ * fontH, paint);

//        if (false) {
//            drawInfoText(canvas, isRecog, Color.YELLOW, left + 20, top + 4 * fontH, paint);
//        } else if (false) {
//            drawInfoText(canvas, isRecog, Color.RED, left + 20, top + 4 * fontH, paint);
//        } else {
//            drawInfoText(canvas, isRecog, Color.WHITE, left + 20, top + 4 * fontH, paint);
//        }
        if(DemoConfig.instance().isLiveDetect) {
            Boolean blive = DataSync.isLiveFace(faceInfo.face_id);
            String slive = "活体:";
            //String isLiveness = "" + (paint.measureText("未开启活体") > w ? ("未开启活体").substring(0, 7) + "..." : "未开启活体");
            if (blive == null) {
                slive = slive + "未检测";
                drawInfoText(canvas, slive, Color.YELLOW, left + 20, top + toph++ * fontH, paint);
            } else if (blive) {
                slive = slive + "通过";
                drawInfoText(canvas, slive, Color.GREEN, left + 20, top + toph++ * fontH, paint);
            } else {
                slive = slive + "失败";
                drawInfoText(canvas, slive, Color.RED, left + 20, top + toph++ * fontH, paint);
            }
        }
        paint.reset();
    }

    private static void drawSmile(Canvas canvas, float top, float left, float right, float bottom, float fontH, boolean isSmile) {
        paint.setStrokeWidth(rectSize / 4);
        paint.setPathEffect(new DashPathEffect(new float[]{0, 0, 0, 0}, 1));
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);//取消锯齿
        //笑脸半径
        float sR = 2 * fontH;
        if (isSmile) {
            paint.setColor(Color.YELLOW);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(right - sR, top, sR, paint);
            paint.setColor(Color.BLACK);
            canvas.drawCircle(right - 3 * sR / 2, top - sR / 2, sR / 4, paint);
            canvas.drawCircle(right - sR / 2, top - sR / 2, sR / 4, paint);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(right - sR, top, sR, paint);
            paint.setColor(Color.BLACK);
            canvas.drawArc(new RectF(right - 5 * sR * 2 / 6, top - 2 * sR / 3, right - sR * 2 / 6, top + sR * 2 / 3), 0, 180, false, paint);
        } else {
            paint.setColor(Color.GRAY);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(right - sR, top, sR, paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(right - 3 * sR / 2, top - sR / 2, sR / 6, paint);
            canvas.drawCircle(right - sR / 2, top - sR / 2, sR / 6, paint);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(right - sR, top, sR, paint);
        }
        paint.reset();
    }

    private static void drawInfoText(Canvas canvas, String content, int color, float x, float y, Paint paint) {
        paint.setColor(color);
        canvas.drawText(content, x, y, paint);
    }

    public static Rect rectMapping(Rect in,int src_w,int src_h,int dest_w,int dest_h){
        if(src_w==dest_w&&src_h==dest_h)
            return in;
        Rect rect = new Rect();
        rect.left = in.left * dest_w / src_w;
        rect.right = in.right * dest_w / src_w;
        rect.top = in.top * dest_h / src_h;
        rect.bottom = in.bottom * dest_h / src_h;
        return rect;
    }

    public static RectF getCropRect(int cap_w,int cap_h,int view_w,int view_h){
        float vw_r = cap_h*view_w/view_h;
        float x_offset = (cap_w-vw_r)/2.0f;
        if(x_offset<0){
            x_offset = 0;
            vw_r = cap_w;
        }
        RectF crop_rect = new RectF(x_offset,0,x_offset+vw_r,cap_h);
        return crop_rect;
    }

    public static Rect rectMapping2Crop(Rect in,int src_w,int src_h,int cap_w,int cap_h,RectF crop_rect){
        Rect cap_rect = rectMapping(in,src_w,src_h,cap_w,cap_h);
        RectF cap_rectf = new RectF(cap_rect);
        if(crop_rect.contains(cap_rectf)){
            RectF cap_rectf2 = new RectF(cap_rectf);
            cap_rectf2.left=cap_rectf2.left-crop_rect.left;
            cap_rectf2.right = cap_rectf2.right-crop_rect.left;
            Rect cap_rect2 = new Rect();
            cap_rect2.left = (int)cap_rectf2.left;
            cap_rect2.right = (int)cap_rectf2.right;
            cap_rect2.top = (int)cap_rectf2.top;
            cap_rect2.bottom = (int)cap_rectf2.bottom;
            return cap_rect2;
        }
        return null;
    }

    private static Rect rectMapping4Portrait(Rect in,int src_w,int src_h,int cap_w,int cap_h,int view_w,int view_h){
        Rect cap_rect = rectMapping(in,src_w,src_h,cap_w,cap_h);
        RectF cap_rectf = new RectF(cap_rect);
        float vw_r = cap_h*view_w/view_h;
        float x_offset = (cap_w-vw_r)/2.0f;
        RectF crop_rect = new RectF(x_offset,0,x_offset+vw_r,cap_h);
        if(crop_rect.contains(cap_rectf)){
            RectF cap_rectf2 = new RectF(cap_rectf);
            cap_rectf2.left=cap_rectf2.left-x_offset;
            cap_rectf2.right = cap_rectf2.right-x_offset;
            Rect cap_rect2 = new Rect();
            cap_rect2.left = (int)cap_rectf2.left;
            cap_rect2.right = (int)cap_rectf2.right;
            cap_rect2.top = (int)cap_rectf2.top;
            cap_rect2.bottom = (int)cap_rectf2.bottom;
            Rect drect = rectMapping(cap_rect2,(int)crop_rect.width(),(int)crop_rect.height(),view_w,view_h);
            return drect;
        }
        return null;
    }

}
