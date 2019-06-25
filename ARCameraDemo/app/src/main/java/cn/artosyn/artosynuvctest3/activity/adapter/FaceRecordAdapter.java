package cn.artosyn.artosynuvctest3.activity.adapter;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Locale;

import cn.artosyn.artosynuvctest3.R;
import cn.artosyn.artosynuvctest3.config.DemoConfig;
import cn.artosyn.artosynuvctest3.facedata.FaceDataUtil;
import cn.artosyn.aruvclib.model.ARUserFace;
import de.hdodenhof.circleimageview.CircleImageView;

public class FaceRecordAdapter extends RecyclerView.Adapter<FaceRecordAdapter.ViewHolder> {
    private final LayoutInflater mLayoutInflater;
    private WeakReference<Context> contextWeakReference;
    private int lastPosition = -1;

    public FaceRecordAdapter(Context context) {
        contextWeakReference = new WeakReference<>(context);
        mLayoutInflater = LayoutInflater.from(context);
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(mLayoutInflater.inflate(R.layout.recordface_list_item, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        if(i>=FaceDataUtil.userFaces_record.size()){
            return;
        }
        ARUserFace userFace = FaceDataUtil.userFaces_record.get(i);
        if(viewHolder.id == userFace.id&&!userFace.bUpdate) {
            viewHolder.itemImg.clearAnimation();
            return;
        }

        if(DemoConfig.instance().isCloudMode) {
            if(!userFace.imageUrl.isEmpty()) {
                Glide.with(contextWeakReference.get()).load(userFace.imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        //.skipMemoryCache(userFace.bUpdate)
                        .into(viewHolder.itemImg);
            }
        }
        else{
            Glide.with(contextWeakReference.get()).load(new File(DemoConfig.FacePicPath +userFace.id+".jpg"))
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    //.skipMemoryCache(userFace.bUpdate)
                    .into(viewHolder.itemImg);
        }
        viewHolder.itemTextName.setText(userFace.name);
        viewHolder.itemTextSimilarity.setText(String.format(Locale.getDefault(),"%d%%", userFace.iSimilarity));

        if(userFace.bUpdate) {
            setAnimation(viewHolder.itemImg);
            userFace.bUpdate = false;
        }
        else{
            viewHolder.itemImg.clearAnimation();
        }
    }

    @Override
    public int getItemCount() {
        return FaceDataUtil.userFaces_record.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView itemImg;
        TextView itemTextName;
        TextView itemTextSimilarity;
        long id = -1;
        ViewHolder(View view) {
            super(view);
            itemImg = view.findViewById(R.id.record_face_image);
            itemTextName = view.findViewById(R.id.record_face_name);
            itemTextSimilarity = view.findViewById(R.id.record_face_similarity);
        }
    }

    private void setAnimation(View view){
        Animation animation = AnimationUtils.loadAnimation(contextWeakReference.get(), android.R.anim.fade_in);
        animation.setDuration(100);
        view.startAnimation(animation);
    }

    private void setAnimation(View viewToAnimate, int position)
    {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > lastPosition)
        {
            Animation animation = AnimationUtils.loadAnimation(contextWeakReference.get(), android.R.anim.slide_in_left);
            animation.setDuration(200);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }
}
