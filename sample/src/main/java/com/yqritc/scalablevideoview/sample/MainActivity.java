package com.yqritc.scalablevideoview.sample;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.yqritc.scalablevideoview.ScalableType;
import com.yqritc.scalablevideoview.ScalableVideoView;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ScalableVideoView mVideoView;
	//Mute Audio
	private AudioManager mAudioManager ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

	    mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mVideoView = (ScalableVideoView) findViewById(R.id.video_view);
        findViewById(R.id.btn_next).setOnClickListener(this);
        findViewById(R.id.btn_next_anim).setOnClickListener(this);

	    mVideoView.setRawData(R.raw.landscape_sample);
	    mVideoView.setLooping(true);
	    mVideoView.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

	@Override
	protected void onStop() {
		super.onStop();
		mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC,false);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC,true);
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ScalableType scalableType;
        switch (item.getItemId()) {
            case R.id.scale_none:
                scalableType = ScalableType.NONE;
                break;

            case R.id.scale_fit_xy:
                scalableType = ScalableType.FIT_XY;
                break;
            case R.id.scale_fit_start:
                scalableType = ScalableType.FIT_START;
                break;
            case R.id.scale_fit_center:
                scalableType = ScalableType.FIT_CENTER;
                break;
            case R.id.scale_fit_end:
                scalableType = ScalableType.FIT_END;
                break;

            case R.id.scale_left_top:
                scalableType = ScalableType.LEFT_TOP;
                break;
            case R.id.scale_left_center:
                scalableType = ScalableType.LEFT_CENTER;
                break;
            case R.id.scale_left_bottom:
                scalableType = ScalableType.LEFT_BOTTOM;
                break;
            case R.id.scale_center_top:
                scalableType = ScalableType.CENTER_TOP;
                break;
            case R.id.scale_center:
                scalableType = ScalableType.CENTER;
                break;
            case R.id.scale_center_bottom:
                scalableType = ScalableType.CENTER_BOTTOM;
                break;
            case R.id.scale_right_top:
                scalableType = ScalableType.RIGHT_TOP;
                break;
            case R.id.scale_right_center:
                scalableType = ScalableType.RIGHT_CENTER;
                break;
            case R.id.scale_right_bottom:
                scalableType = ScalableType.RIGHT_BOTTOM;
                break;

            case R.id.scale_left_top_crop:
                scalableType = ScalableType.LEFT_TOP_CROP;
                break;
            case R.id.scale_left_center_crop:
                scalableType = ScalableType.LEFT_CENTER_CROP;
                break;
            case R.id.scale_left_bottom_crop:
                scalableType = ScalableType.LEFT_BOTTOM_CROP;
                break;
            case R.id.scale_center_top_crop:
                scalableType = ScalableType.CENTER_TOP_CROP;
                break;
            case R.id.scale_center_crop:
                scalableType = ScalableType.CENTER_CROP;
                break;
            case R.id.scale_center_bottom_crop:
                scalableType = ScalableType.CENTER_BOTTOM_CROP;
                break;
            case R.id.scale_right_top_crop:
                scalableType = ScalableType.RIGHT_TOP_CROP;
                break;
            case R.id.scale_right_center_crop:
                scalableType = ScalableType.RIGHT_CENTER_CROP;
                break;
            case R.id.scale_right_bottom_crop:
                scalableType = ScalableType.RIGHT_BOTTOM_CROP;
                break;

            case R.id.scale_start_inside:
                scalableType = ScalableType.START_INSIDE;
                break;
            case R.id.scale_center_inside:
                scalableType = ScalableType.CENTER_INSIDE;
                break;
            case R.id.scale_end_inside:
                scalableType = ScalableType.END_INSIDE;
                break;

            default:
                return super.onOptionsItemSelected(item);

        }

        mVideoView.setScalableType(scalableType);
        mVideoView.invalidate();
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_next:
                SampleActivity.startActivity(this);
                break;
	        case R.id.btn_next_anim:
		        VideoAnimActivity.callMe(this);
		        break;
            default:
                break;
        }
    }
}
