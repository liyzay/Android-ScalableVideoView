package com.yqritc.scalablevideoview.sample;

import android.content.Context;
import android.media.AudioManager;
import android.support.annotation.RawRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.yqritc.scalablevideoview.ScalableType;
import com.yqritc.scalablevideoview.ScalableVideoView;

/**
 * Created by yqritc on 2015/06/14.
 */
public class SampleAdapter extends RecyclerView.Adapter<SampleAdapter.ViewHolder> {

    @RawRes
    private int mVideoResId;
    private LayoutInflater mLayoutInflater;

    public SampleAdapter(Context context) {
        super();
        mVideoResId = R.raw.landscape_sample;
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mLayoutInflater.inflate(R.layout.layout_sample_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Context context = holder.itemView.getContext();

        ScalableType scalableType = ScalableType.values()[position];
        holder.mTextView.setText(context.getString(R.string.sample_scale_title, position,
                scalableType.toString()));
        holder.setScalableType(scalableType);
    }

    @Override
    public void onViewAttachedToWindow(ViewHolder holder) {
        setVideo(holder.mVideoView);
        holder.mVideoView.setScalableType(holder.mScalableType);
    }

    private void setVideo(final ScalableVideoView videoView) {
	    videoView.setRawData(mVideoResId);
	    videoView.setLooping(true);
	    videoView.start();
    }

    @Override
    public int getItemCount() {
        return ScalableType.values().length;
    }

    public void setVideoResId(@RawRes int id) {
        mVideoResId = id;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView mTextView;
        ScalableVideoView mVideoView;
        ScalableType mScalableType;

        public ViewHolder(View view) {
            super(view);
            mTextView = (TextView) view.findViewById(R.id.video_text);
            mVideoView = (ScalableVideoView) view.findViewById(R.id.video_view);
        }

        public void setScalableType(ScalableType type) {
            mScalableType = type;
        }
    }
}
