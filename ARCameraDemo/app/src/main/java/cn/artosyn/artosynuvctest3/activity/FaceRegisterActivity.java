package cn.artosyn.artosynuvctest3.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Locale;

import cn.artosyn.artosynuvctest3.R;
import cn.artosyn.artosynuvctest3.activity.base.BaseActivity;
import cn.artosyn.artosynuvctest3.config.DemoConfig;
import cn.artosyn.artosynuvctest3.facedata.FaceDataUtil;
import cn.artosyn.artosynuvctest3.register.RegisterHelper;
import cn.artosyn.aruvclib.model.ARUserFace;

public class FaceRegisterActivity extends BaseActivity {

    final private int PICK_IMAGE = 1;
    TextView textView_reged_info;

    private RegisterHelper registerHelper;

    public static WeakReference<Context> faceRedActivityCtxtWeakRef;
    RegedFaceRecyclerViewAdapter regedFaceRecyclerViewAdapter;
    RecyclerView mRecyclerView_Reged;

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_register);

        faceRedActivityCtxtWeakRef = new WeakReference<Context>(this);

        registerHelper = new RegisterHelper(this);

        mRecyclerView_Reged = findViewById(R.id.recyclerView_RegedFace);
        mRecyclerView_Reged.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        //mRecyclerView_Reged.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView_Reged.setLayoutManager(new GridLayoutManager(this,4));
        regedFaceRecyclerViewAdapter = new RegedFaceRecyclerViewAdapter(this);
        mRecyclerView_Reged.setAdapter(regedFaceRecyclerViewAdapter);

        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                Bitmap bitmap = (Bitmap)msg.obj;
                registerHelper.init();
                registerHelper.register(bitmap, faceRedActivityCtxtWeakRef.get());
            }
        };

        textView_reged_info = findViewById(R.id.textView_reged_info);

    }

    @Override
    protected void onStart() {
        super.onStart();
        String result = registerHelper.init();
        Toast.makeText(this,result,Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //FaceDataUtil.instance().updateUsers();
        new UpdateUserInfo(this).execute();
    }

    private static class UpdateUserInfo extends AsyncTask<Void, Void, Void>{

        WeakReference<FaceRegisterActivity> activityWeakReference;
        UpdateUserInfo(FaceRegisterActivity ctx){
            activityWeakReference = new WeakReference<>(ctx);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            FaceDataUtil.instance().updateUsers();
            return null;
        }

        @Override
        protected void onPreExecute() {
            activityWeakReference.get().setUserListEnable(false);
            activityWeakReference.get().updateInfoText("更新数据中...");
        }

        @Override
        protected void onPostExecute(Void result) {
            activityWeakReference.get().updateInfoText("更新完成:"+FaceDataUtil.userFaces_all.size());
            activityWeakReference.get().regedFaceRecyclerViewAdapter.notifyDataSetChanged();
            activityWeakReference.get().setUserListEnable(true);
        }
    }

    void updateInfoText(String info){
        textView_reged_info.setText(info);
    }
    void setUserListEnable(boolean enable){
        mRecyclerView_Reged.setEnabled(enable);
    }

    private void runInBackground(Runnable runnable) {
        handler.post(runnable);
    }

    public void onClick(View item) {
        switch (item.getId()) {
            case R.id.imageButton_back:
                onBackPressed();
                break;
            case R.id.imageButton_reg_picture:
                Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getIntent.setType("image/*");
                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("image/*");
                Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});
                startActivityForResult(chooserIntent, PICK_IMAGE);
                break;
            case R.id.imageButton_scroll:
                String sname = ((EditText)findViewById(R.id.editText_name)).getText().toString();
                if(sname.isEmpty())
                    break;
                int iposition = 0;
                for(Iterator<ARUserFace> iterator = FaceDataUtil.userFaces_all.iterator(); iterator.hasNext();){
                    ARUserFace userFace = iterator.next();
                    iposition++;
                    Log.d("Name",sname+"  "+userFace.name+" "+iposition);
                    if(userFace.name.equals(sname)){
                        break;
                    }
                }
                if(iposition>0){
                    ((RecyclerView)findViewById(R.id.recyclerView_RegedFace)).getLayoutManager().scrollToPosition(iposition);
                }
                break;
            case R.id.imageButton_delteall:
                new AlertDialog.Builder(FaceRegisterActivity.this)
                        .setTitle("确定删除所有数据么？")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                registerHelper.delete_alluser();
                                regedFaceRecyclerViewAdapter.notifyDataSetChanged();
                                updateInfoText("更新完成:"+FaceDataUtil.userFaces_all.size());
                            }
                        })
                        .setNegativeButton("Cancle", null)
                        .show();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                Toast.makeText(this,"Pick picture data error",Toast.LENGTH_SHORT).show();
                return;
            }
            InputStream inputStream = null;
            try {
                inputStream = this.getContentResolver().openInputStream(data.getData());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this,"Picture data input error",Toast.LENGTH_SHORT).show();
                return;
            }
            final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if(bitmap==null)
                return;
            handler.obtainMessage(0,bitmap).sendToTarget();
        }
    }

    //recycelview holder
    public class RegedFaceRecyclerViewAdapter extends RecyclerView.Adapter<RegedFaceRecyclerViewAdapter.ViewHolder> {
        private final LayoutInflater mLayoutInflater;
        private final Context mContext;

        public RegedFaceRecyclerViewAdapter(Context context) {
            mContext = context;
            mLayoutInflater = LayoutInflater.from(context);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(mLayoutInflater.inflate(R.layout.regface_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ARUserFace userFace = FaceDataUtil.userFaces_all.get(position);
            if(holder.id == userFace.id)
                return;
            holder.id = userFace.id;
            if(DemoConfig.instance().isCloudMode) {
                if(!userFace.imageUrl.isEmpty()) {
                    Glide.with(FaceRegisterActivity.this).load(userFace.imageUrl)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(holder.itemImg);
                }
            }
            else{
                Glide.with(FaceRegisterActivity.this).load(new File(DemoConfig.FacePicPath + userFace.id + ".jpg"))
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(holder.itemImg);
            }
            holder.itemTextID.setText(String.format(Locale.getDefault(),"%d",userFace.id));
            holder.itemTextName.setText(String.format(Locale.getDefault(),"%s (%s %d)", userFace.name,userFace.gender, userFace.age));
        }

        @Override
        public int getItemCount() {
            return FaceDataUtil.userFaces_all.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView itemImg;
            TextView itemTextID;
            TextView itemTextName;
            ImageButton imageButtonDel;
            long id = -1;
            ViewHolder(View view) {
                super(view);
                itemImg = view.findViewById(R.id.imageView_itemImg);
                itemTextID = view.findViewById(R.id.textView_itemID);
                itemTextName = view.findViewById(R.id.textView_imteName);
                imageButtonDel = view.findViewById(R.id.imageButton_itemDelete);
                imageButtonDel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!mRecyclerView_Reged.isEnabled()){
                            toastMsg("更新中无法删除");
                            return;
                        }
                        registerHelper.delete_user(id);
                        regedFaceRecyclerViewAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
    }
}
