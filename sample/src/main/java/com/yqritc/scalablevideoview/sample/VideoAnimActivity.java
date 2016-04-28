package com.yqritc.scalablevideoview.sample;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.ProgressBar;

import com.yqritc.scalablevideoview.ScalableType;
import com.yqritc.scalablevideoview.ScalableVideoView;

/**
 * Created by Alex on 2016/4/28.
 */
public class VideoAnimActivity extends AppCompatActivity implements OnClickListener {
	private final String VIDEO_URL = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";

	private ScalableVideoView mVideoView ;
	private View mAnimView ;
	private ProgressBar mProgressBar ;
	private ImageButton btn_collapse_screen ;

	private boolean isFullScreen = true ;
	private int MIN_HEIGHT ;
	private int FULL_HEIGHT ;

	public static void callMe(Activity activity){
		Intent intent = new Intent(activity,VideoAnimActivity.class);
		activity.startActivity(intent);
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN,LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_video_anim);

		MIN_HEIGHT = getResources().getDisplayMetrics().widthPixels ;
		mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
		mVideoView = (ScalableVideoView) findViewById(R.id.video_view);
		mAnimView = findViewById(R.id.lay_container);
		btn_collapse_screen = (ImageButton) findViewById(R.id.btn_collapse_screen);
		btn_collapse_screen.setOnClickListener(this);
		mVideoView.setOnPreparedListener(new OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				mProgressBar.setVisibility(View.GONE);
			}
		});
		mVideoView.setVideoPath(VIDEO_URL);
		mVideoView.start();
		mVideoView.setMediaController(new MediaController(this));
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()){
			case R.id.btn_collapse_screen:
				if(isFullScreen){
					collapseScreen();
				}else {
					expandScreen();
				}
				break;
		}
	}

	void expandScreen(){
		ValueAnimator animator = ValueAnimator.ofInt(MIN_HEIGHT,FULL_HEIGHT);
		animator.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				int newHeight = (int) animation.getAnimatedValue();
				ViewGroup.LayoutParams params = mAnimView.getLayoutParams();
				params.height = newHeight ;
				mAnimView.setLayoutParams(params);
			}
		});
		animator.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {

			}

			@Override
			public void onAnimationEnd(Animator animation) {
				isFullScreen = true ;
				btn_collapse_screen.setImageResource(R.drawable.ic_collapse_screen);
				mVideoView.setScalableType(ScalableType.CENTER_CROP);
			}

			@Override
			public void onAnimationCancel(Animator animation) {

			}

			@Override
			public void onAnimationRepeat(Animator animation) {

			}
		});
		animator.start();
	}

	void collapseScreen(){
		if(FULL_HEIGHT==0){
			FULL_HEIGHT = mAnimView.getHeight() ;
		}
		ValueAnimator animator = ValueAnimator.ofInt(FULL_HEIGHT,MIN_HEIGHT);
		animator.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				int newHeight = (int) animation.getAnimatedValue();
				ViewGroup.LayoutParams params = mAnimView.getLayoutParams();
				params.height = newHeight ;
				mAnimView.setLayoutParams(params);
			}
		});
		animator.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {

			}

			@Override
			public void onAnimationEnd(Animator animation) {
				isFullScreen = false ;
				btn_collapse_screen.setImageResource(R.drawable.ic_fullscreen);
				mVideoView.setScalableType(ScalableType.FIT_CENTER);
			}

			@Override
			public void onAnimationCancel(Animator animation) {

			}

			@Override
			public void onAnimationRepeat(Animator animation) {

			}
		});
		animator.start();
	}
}
