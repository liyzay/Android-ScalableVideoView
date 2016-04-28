# Android-ScalableVideoView

Android Texture VideoView having a variety of scale types like the scale types of ImageView.

![Sample](/art/device.gif)

# Usage

### Set scale type in layout file
```
<com.yqritc.scalablevideoview.ScalableVideoView
  android:id="@+id/video_view"
  android:layout_width="match_parent"
  android:layout_height="match_parent"
  app:scalableType="centerCrop"/>
```
Please refere the following xml for the list of scalableType you can set.  
[attrs.xml](https://github.com/yqritc/Android-ScalableVideoView/blob/master/library/src/main/res/values/attrs.xml)

### Sample usage in source code
```
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
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

```
[ScalableVideoView](https://github.com/yqritc/Android-ScalableVideoView/blob/master/library/src/main/java/com/yqritc/scalablevideoview/ScalableVideoView.java) is extending TextureView to play video by using MediaPlayer.  
Basic functionalities are defined in this class to play and scale video.  
If you need to control more, extend this class and define your custom video view.  


# License
```
Copyright 2015 yqritc

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
