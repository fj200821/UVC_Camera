package cn.artosyn.artosynuvctest3.register;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.moons.LoginDialog.LoginDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import cn.artosyn.artosynuvctest3.config.DemoConfig;
import cn.artosyn.artosynuvctest3.facedata.FaceDataUtil;
import cn.artosyn.aruvclib.ARAlgoSet;
import cn.artosyn.aruvclib.ARUtil;
import cn.artosyn.aruvclib.model.ARFaceResult;
import cn.artosyn.aruvclib.model.ARUserFace;

public class RegisterHelper {

    private ARAlgoSet arAlgoSet;

    public RegisterHelper(Context ctx) {
        arAlgoSet = new ARAlgoSet(ctx, DemoConfig.DatabasePath);
    }

    public String init() {
        ARFaceResult result = arAlgoSet.init();
        return result.msg;
    }

    public void release() {
        arAlgoSet.release();
    }

    public void delete_alluser() {
        if (arAlgoSet.removeAllUser()) {
            synchronized (FaceDataUtil.userFaces_all) {
                FaceDataUtil.instance().deleteAll();
            }
        }
    }

    public void delete_user(long id) {
        synchronized (FaceDataUtil.userFaces_all) {
            for (Iterator<ARUserFace> iterator = FaceDataUtil.userFaces_all.iterator(); iterator.hasNext(); ) {
                ARUserFace userFace = iterator.next();
                if (userFace.id == id) {
                    arAlgoSet.removeUserByPersonId(userFace.rsid);
                    FaceDataUtil.instance().deleteByID(id);
                    iterator.remove();
                }
            }
        }
    }

    public void delete_users(ArrayList<Integer> ids) {
        for (Integer id : ids) {
            delete_user(id);
        }
    }


    public void checkFaceAndPassword(final Bitmap bitmap, final Context context) {
        final ARFaceResult faceResult = arAlgoSet.registByBitmap(bitmap);
        if (faceResult.code == 0) {
            final LoginDialog confirmDialog = new LoginDialog(context);
            confirmDialog.show();
            confirmDialog.setConfirmButton(new LoginDialog.OnConfirmClickListener() {
                @Override
                public void doConfirm(boolean isPass) {
                    confirmDialog.dismiss();
                    if (isPass) {
                        register(bitmap, context,faceResult);
                    }
                }
            });
        } else {
            Toast.makeText(context, faceResult.msg, Toast.LENGTH_SHORT).show();
        }
    }


    public void register(Bitmap bitmap, Context context, final ARFaceResult faceResult){
        final Bitmap bitmap_face = Bitmap.createBitmap(bitmap, (int) faceResult.rect[0], (int) faceResult.rect[1],
                (int) faceResult.rect[2], (int) faceResult.rect[3], null, false);

        final LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        final EditText editText_name = new EditText(context);
        final ImageView imageView = new ImageView(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(180, 180);
        layoutParams.weight = 0;
        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        layoutParams.setMargins(10, 10, 10, 0);
        imageView.setImageBitmap(bitmap_face);
        LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams1.gravity = Gravity.CENTER_VERTICAL;

        layout.addView(imageView, layoutParams);
        layout.addView(editText_name, layoutParams1);

        final Context context1 = context;
        new AlertDialog.Builder(context)
                .setTitle("输入姓名:")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(layout)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        faceResult.userFace.name = editText_name.getText().toString();
                        if (!faceResult.userFace.name.isEmpty()) {
                            faceResult.userFace.bUpdate = true;
                            long id = FaceDataUtil.instance().insert(faceResult.userFace);
                            if (id == -1) {
                                Toast.makeText(context1, String.format("人脸注册失败:数据库错误"), Toast.LENGTH_SHORT).show();
                            } else {
                                faceResult.userFace.id = id;
                                File file_face = new File(DemoConfig.FacePicPath + id + ".jpg");
                                ARUtil.SaveBitmap(bitmap_face, file_face);
                                Toast.makeText(context1, String.format("人脸注册成功:%s", faceResult.userFace.name), Toast.LENGTH_SHORT).show();
                                synchronized (FaceDataUtil.userFaces_all) {
                                    FaceDataUtil.userFaces_all.add(faceResult.userFace);
                                }
                                bitmap_face.recycle();
                            }
                        } else {
                            arAlgoSet.removeUserByPersonId(faceResult.personId);
                            Toast.makeText(context1, "姓名为空", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancle", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        arAlgoSet.removeUserByPersonId(faceResult.personId);
                        Toast.makeText(context1, "取消注册", Toast.LENGTH_SHORT).show();
                    }
                })
                .setCancelable(false)
                .show();
    }




    public String register(Bitmap bitmap, Context context) {
        final ARFaceResult faceResult = arAlgoSet.registByBitmap(bitmap);
        if (faceResult.code == 0) {
            final Bitmap bitmap_face = Bitmap.createBitmap(bitmap, (int) faceResult.rect[0], (int) faceResult.rect[1],
                    (int) faceResult.rect[2], (int) faceResult.rect[3], null, false);

            final LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            final EditText editText_name = new EditText(context);
            final ImageView imageView = new ImageView(context);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(180, 180);
            layoutParams.weight = 0;
            layoutParams.gravity = Gravity.CENTER_VERTICAL;
            layoutParams.setMargins(10, 10, 10, 0);
            imageView.setImageBitmap(bitmap_face);
            LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams1.gravity = Gravity.CENTER_VERTICAL;

            layout.addView(imageView, layoutParams);
            layout.addView(editText_name, layoutParams1);

            final Context context1 = context;
            new AlertDialog.Builder(context)
                    .setTitle("输入姓名:")
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setView(layout)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            faceResult.userFace.name = editText_name.getText().toString();
                            if (!faceResult.userFace.name.isEmpty()) {
                                faceResult.userFace.bUpdate = true;
                                long id = FaceDataUtil.instance().insert(faceResult.userFace);
                                if (id == -1) {
                                    Toast.makeText(context1, String.format("人脸注册失败:数据库错误"), Toast.LENGTH_SHORT).show();
                                } else {
                                    faceResult.userFace.id = id;
                                    File file_face = new File(DemoConfig.FacePicPath + id + ".jpg");
                                    ARUtil.SaveBitmap(bitmap_face, file_face);
                                    Toast.makeText(context1, String.format("人脸注册成功:%s", faceResult.userFace.name), Toast.LENGTH_SHORT).show();
                                    synchronized (FaceDataUtil.userFaces_all) {
                                        FaceDataUtil.userFaces_all.add(faceResult.userFace);
                                    }
                                    bitmap_face.recycle();
                                }
                            } else {
                                arAlgoSet.removeUserByPersonId(faceResult.personId);
                                Toast.makeText(context1, "姓名为空", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .setNegativeButton("Cancle", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            arAlgoSet.removeUserByPersonId(faceResult.personId);
                            Toast.makeText(context1, "取消注册", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setCancelable(false)
                    .show();
            return "ok";
        }
        Toast.makeText(context, faceResult.msg, Toast.LENGTH_SHORT).show();
        return faceResult.msg;
    }

    public String register(Bitmap bitmap, String name) {
        long time_start = System.currentTimeMillis();
        final ARFaceResult faceResult = arAlgoSet.registByBitmap(bitmap);
        long time_end = System.currentTimeMillis();
        Log.w("RegiterHelper   ", String.format("arFaceSet time :%d", time_end - time_start));
        String sresult = "";
        if (faceResult.code == 0) {
            final Bitmap bitmap_face = Bitmap.createBitmap(bitmap, (int) faceResult.rect[0], (int) faceResult.rect[1],
                    (int) faceResult.rect[2], (int) faceResult.rect[3], null, false);
            faceResult.userFace.name = name;
            if (!faceResult.userFace.name.isEmpty()) {
                faceResult.userFace.bUpdate = true;
                long id = FaceDataUtil.instance().insert(faceResult.userFace);
                if (id == -1) {
                    arAlgoSet.removeUserByPersonId(faceResult.personId);
                    sresult = "数据库操作失败";
                } else {
                    faceResult.userFace.id = id;
                    File file_face = new File(DemoConfig.FacePicPath + id + ".jpg");
                    ARUtil.SaveBitmap(bitmap_face, file_face);
                    synchronized (FaceDataUtil.userFaces_all) {
                        FaceDataUtil.userFaces_all.add(faceResult.userFace);
                    }
                    sresult = "ok";
                }
            }
            bitmap_face.recycle();
        } else {
            bitmap.recycle();
            return faceResult.msg;
        }
        bitmap.recycle();
        long time_end2 = System.currentTimeMillis();
        Log.w("RegiterHelper   ", String.format("database time :%d", time_end2 - time_end));
        return sresult;
    }
}
