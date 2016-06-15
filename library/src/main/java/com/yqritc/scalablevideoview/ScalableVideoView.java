package com.yqritc.scalablevideoview;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.net.Uri;
import android.os.PowerManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;

import java.io.IOException;
import java.util.Map;

/**
 * Created by yqritc on 2015/06/11.
 */
public class ScalableVideoView extends TextureView implements MediaPlayerControl{
	private String TAG = "ScalableVideoView";
	// settable by the client
	private Uri         mUri;
	private Map<String, String> mHeaders;

	// all possible internal states
	private static final int STATE_ERROR              = -1;
	private static final int STATE_IDLE               = 0;
	private static final int STATE_PREPARING          = 1;
	private static final int STATE_PREPARED           = 2;
	private static final int STATE_PLAYING            = 3;
	private static final int STATE_PAUSED             = 4;
	private static final int STATE_PLAYBACK_COMPLETED = 5;

	// mCurrentState is a VideoView object's current state.
	// mTargetState is the state that a method caller intends to reach.
	// For instance, regardless the VideoView object's current state,
	// calling pause() intends to bring the object to a target state
	// of STATE_PAUSED.
	private int mCurrentState = STATE_IDLE;
	private int mTargetState  = STATE_IDLE;

	// All the stuff we need for playing and showing a video
	private SurfaceTexture mSurfaceTexture = null ;
	private MediaPlayer mMediaPlayer = null;
	private int         mAudioSession;
	private int         mVideoWidth;
	private int         mVideoHeight;
	private MediaController mMediaController;
	private OnCompletionListener mOnCompletionListener;
	private MediaPlayer.OnPreparedListener mOnPreparedListener;
	private int         mCurrentBufferPercentage;
	private OnErrorListener mOnErrorListener;
	private OnInfoListener mOnInfoListener;
	private int         mSeekWhenPrepared;  // recording the seek position while preparing
	private boolean     mCanPause;
	private boolean     mCanSeekBack;
	private boolean     mCanSeekForward;

	protected ScalableType mScalableType = ScalableType.NONE;
	private boolean looping = false ;

	public ScalableVideoView(Context context) {
		this(context, null);
	}

	public ScalableVideoView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ScalableVideoView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		if (attrs == null) {
			return;
		}

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.scaleStyle, 0, 0);
		if (a == null) {
			return;
		}

		int scaleType = a.getInt(R.styleable.scaleStyle_scalableType, ScalableType.NONE.ordinal());
		a.recycle();
		mScalableType = ScalableType.values()[scaleType];

		initVideoView();
	}

	private void scaleVideoSize(int videoWidth, int videoHeight) {
		if (videoWidth == 0 || videoHeight == 0) {
			return;
		}

		Size viewSize = new Size(getWidth(), getHeight());
		Size videoSize = new Size(videoWidth, videoHeight);
		ScaleManager scaleManager = new ScaleManager(viewSize, videoSize);
		Matrix matrix = scaleManager.getScaleMatrix(mScalableType);
		if (matrix != null) {
			setTransform(matrix);
		}
	}

	public void setScalableType(ScalableType scalableType) {
		mScalableType = scalableType;
		scaleVideoSize(mVideoWidth, mVideoHeight);
	}

	public void setRawData(int id) {
		String videoPath = "android.resource://"+getContext().getPackageName()+"/"+id ;
		setVideoURI(Uri.parse(videoPath));
	}

	public void setLooping(boolean looping) {
		this.looping = looping ;
	}

	public boolean isLooping() {
		return looping;
	}

	@Override
	public CharSequence getAccessibilityClassName() {
		return ScalableVideoView.class.getName();
	}

	private void initVideoView() {
		mVideoWidth = 0;
		mVideoHeight = 0;
		setSurfaceTextureListener(mSTListener);
		setFocusable(true);
		setFocusableInTouchMode(true);
		requestFocus();
		mCurrentState = STATE_IDLE;
		mTargetState  = STATE_IDLE;
	}


	/**
	 * Sets video path.
	 *
	 * @param path the path of the video.
	 */
	public void setVideoPath(String path) {
		setVideoURI(Uri.parse(path));
	}

	/**
	 * Sets video URI.
	 *
	 * @param uri the URI of the video.
	 */
	public void setVideoURI(Uri uri) {
		setVideoURI(uri, null);
	}

	/**
	 * Sets video URI using specific headers.
	 *
	 * @param uri     the URI of the video.
	 * @param headers the headers for the URI request.
	 *                Note that the cross domain redirection is allowed by default, but that can be
	 *                changed with key/value pairs through the headers parameter with
	 *                "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value
	 *                to disallow or allow cross domain redirection.
	 */
	public void setVideoURI(Uri uri, Map<String, String> headers) {
		mUri = uri;
		mHeaders = headers;
		mSeekWhenPrepared = 0;
		openVideo();
		requestLayout();
		invalidate();
	}

	public void stopPlayback() {
		if (mMediaPlayer != null) {
			mMediaPlayer.stop();
			mMediaPlayer.release();
			mMediaPlayer = null;
			mCurrentState = STATE_IDLE;
			mTargetState  = STATE_IDLE;
			AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
			am.abandonAudioFocus(null);
		}
	}

	private void openVideo() {
		if (mUri == null || mSurfaceTexture == null) {
			// not ready for playback just yet, will try again later
			return;
		}
		// we shouldn't clear the target state, because somebody might have
		// called start() previously
		release(false);

		AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
		am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

		try {
			mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setLooping(looping);

			if (mAudioSession != 0) {
				mMediaPlayer.setAudioSessionId(mAudioSession);
			} else {
				mAudioSession = mMediaPlayer.getAudioSessionId();
			}
			mMediaPlayer.setOnPreparedListener(mPreparedListener);
			mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
			mMediaPlayer.setOnCompletionListener(mCompletionListener);
			mMediaPlayer.setOnErrorListener(mErrorListener);
			mMediaPlayer.setOnInfoListener(mInfoListener);
			mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
			mCurrentBufferPercentage = 0;
			mMediaPlayer.setDataSource(getContext(), mUri, mHeaders);
			mMediaPlayer.setSurface(new Surface(mSurfaceTexture));
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//			mMediaPlayer.setScreenOnWhilePlaying(true);
			//keep screen on
			mMediaPlayer.setWakeMode(getContext(), PowerManager.SCREEN_BRIGHT_WAKE_LOCK);
			mMediaPlayer.prepareAsync();

			// we don't set the target state here either, but preserve the
			// target state that was there before.
			mCurrentState = STATE_PREPARING;
			attachMediaController();
		} catch (IOException ex) {
			Log.w(TAG, "Unable to open content: " + mUri, ex);
			mCurrentState = STATE_ERROR;
			mTargetState = STATE_ERROR;
			mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			return;
		} catch (IllegalArgumentException ex) {
			Log.w(TAG, "Unable to open content: " + mUri, ex);
			mCurrentState = STATE_ERROR;
			mTargetState = STATE_ERROR;
			mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
			return;
		} finally {

		}
	}

	public void setMediaController(MediaController controller) {
		if (mMediaController != null) {
			mMediaController.hide();
		}
		mMediaController = controller;
		attachMediaController();
	}

	private void attachMediaController() {
		if (mMediaPlayer != null && mMediaController != null) {
			mMediaController.setMediaPlayer(this);
			View anchorView = this.getParent() instanceof View ?
					(View)this.getParent() : this;
			mMediaController.setAnchorView(anchorView);
			mMediaController.setEnabled(isInPlaybackState());
		}
	}

	MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener() {
		public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
			scaleVideoSize(width,height);
		}
	};

	MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
		public void onPrepared(MediaPlayer mp) {
			mCurrentState = STATE_PREPARED;

			mCanPause = mCanSeekBack = mCanSeekForward = true;

			if (mOnPreparedListener != null) {
				mOnPreparedListener.onPrepared(mMediaPlayer);
			}
			if (mMediaController != null) {
				mMediaController.setEnabled(true);
			}
			mVideoWidth = mp.getVideoWidth();
			mVideoHeight = mp.getVideoHeight();

			int seekToPosition = mSeekWhenPrepared;  // mSeekWhenPrepared may be changed after seekTo() call
			if (seekToPosition != 0) {
				seekTo(seekToPosition);
			}
			// We don't know the video size yet, but should start anyway.
			// The video size might be reported to us later.
			if (mTargetState == STATE_PLAYING) {
				start();
			}
		}
	};

	private MediaPlayer.OnCompletionListener mCompletionListener =
			new MediaPlayer.OnCompletionListener() {
				public void onCompletion(MediaPlayer mp) {
					mCurrentState = STATE_PLAYBACK_COMPLETED;
					mTargetState = STATE_PLAYBACK_COMPLETED;
					if (mMediaController != null) {
						mMediaController.hide();
					}
					if (mOnCompletionListener != null) {
						mOnCompletionListener.onCompletion(mMediaPlayer);
					}
				}
			};

	private MediaPlayer.OnInfoListener mInfoListener =
			new MediaPlayer.OnInfoListener() {
				public  boolean onInfo(MediaPlayer mp, int arg1, int arg2) {
					if (mOnInfoListener != null) {
						mOnInfoListener.onInfo(mp, arg1, arg2);
					}
					return true;
				}
			};

	private MediaPlayer.OnErrorListener mErrorListener =
			new MediaPlayer.OnErrorListener() {
				public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
					Log.d(TAG, "Error: " + framework_err + "," + impl_err);
					mCurrentState = STATE_ERROR;
					mTargetState = STATE_ERROR;
					if (mMediaController != null) {
						mMediaController.hide();
					}

					if (mOnErrorListener != null) {
						if (mOnErrorListener.onError(mMediaPlayer, framework_err, impl_err)) {
							return true;
						}
					}

					/* Otherwise, pop up an error dialog so the user knows that
	         * something bad has happened. Only try and pop up the dialog
             * if we're attached to a window. When we're going away and no
             * longer have a window, don't bother showing the user an error.
             */
					if (getWindowToken() != null && getContext()!=null) {
						int messageId;

						messageId = R.string.VideoView_error_text_unknown;

						new AlertDialog.Builder(getContext())
								.setMessage(messageId)
								.setPositiveButton(R.string.VideoView_error_button,
										new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int whichButton) {
											}
										})
								.setCancelable(false)
								.show();
					}

					return true;
				}
			};

	private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
			new MediaPlayer.OnBufferingUpdateListener() {
				public void onBufferingUpdate(MediaPlayer mp, int percent) {
					mCurrentBufferPercentage = percent;
				}
			};

	/**
	 * Register a callback to be invoked when the media file
	 * is loaded and ready to go.
	 *
	 * @param l The callback that will be run
	 */
	public void setOnPreparedListener(MediaPlayer.OnPreparedListener l)
	{
		mOnPreparedListener = l;
	}

	/**
	 * Register a callback to be invoked when the end of a media file
	 * has been reached during playback.
	 *
	 * @param l The callback that will be run
	 */
	public void setOnCompletionListener(OnCompletionListener l)
	{
		mOnCompletionListener = l;
	}

	/**
	 * Register a callback to be invoked when an error occurs
	 * during playback or setup.  If no listener is specified,
	 * or if the listener returned false, VideoView will inform
	 * the user of any errors.
	 *
	 * @param l The callback that will be run
	 */
	public void setOnErrorListener(OnErrorListener l)
	{
		mOnErrorListener = l;
	}

	/**
	 * Register a callback to be invoked when an informational event
	 * occurs during playback or setup.
	 *
	 * @param l The callback that will be run
	 */
	public void setOnInfoListener(OnInfoListener l) {
		mOnInfoListener = l;
	}

	SurfaceTextureListener mSTListener = new SurfaceTextureListener() {
		@Override
		public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
			mSurfaceTexture = surface ;
			openVideo();
		}

		@Override
		public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
			boolean isValidState =  (mTargetState == STATE_PLAYING);
			boolean hasValidSize = (mVideoWidth == width && mVideoHeight == height);
			if (mMediaPlayer != null && isValidState && hasValidSize) {
				if (mSeekWhenPrepared != 0) {
					seekTo(mSeekWhenPrepared);
				}
				start();
			}
		}

		@Override
		public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
			mSurfaceTexture = null;
			if (mMediaController != null){
				mMediaController.hide();
			}
			release(false);

			return false;
		}

		@Override
		public void onSurfaceTextureUpdated(SurfaceTexture surface) {
			mSurfaceTexture = surface ;
		}
	};

	/*
	 * release the media player in any state
	 */
	private void release(boolean cleartargetstate) {
		if (mMediaPlayer != null) {
			//Maybe save the progress
			mSeekWhenPrepared = mMediaPlayer.getCurrentPosition();
			mMediaPlayer.reset();
			mMediaPlayer.release();
			mMediaPlayer = null;
			mCurrentState = STATE_IDLE;
			if (cleartargetstate) {
				mTargetState  = STATE_IDLE;
				mSeekWhenPrepared = 0 ;
			}
			AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
			am.abandonAudioFocus(null);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (isInPlaybackState() && mMediaController != null) {
			toggleMediaControlsVisiblity();
		}
		return false;
	}

	@Override
	public boolean onTrackballEvent(MotionEvent ev) {
		if (isInPlaybackState() && mMediaController != null) {
			toggleMediaControlsVisiblity();
		}
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK &&
				keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
				keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
				keyCode != KeyEvent.KEYCODE_VOLUME_MUTE &&
				keyCode != KeyEvent.KEYCODE_MENU &&
				keyCode != KeyEvent.KEYCODE_CALL &&
				keyCode != KeyEvent.KEYCODE_ENDCALL;
		if (isInPlaybackState() && isKeyCodeSupported && mMediaController != null) {
			if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
					keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
				if (mMediaPlayer.isPlaying()) {
					pause();
					mMediaController.show();
				} else {
					start();
					mMediaController.hide();
				}
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
				if (!mMediaPlayer.isPlaying()) {
					start();
					mMediaController.hide();
				}
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
					|| keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
				if (mMediaPlayer.isPlaying()) {
					pause();
					mMediaController.show();
				}
				return true;
			} else {
				toggleMediaControlsVisiblity();
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	private void toggleMediaControlsVisiblity() {
		if (mMediaController.isShowing()) {
			mMediaController.hide();
		} else {
			mMediaController.show();
		}
	}

	@Override
	public void start() {
		if (isInPlaybackState()) {
			mMediaPlayer.start();
			mCurrentState = STATE_PLAYING;
		}
		mTargetState = STATE_PLAYING;
	}

	@Override
	public void pause() {
		if (isInPlaybackState()) {
			if (mMediaPlayer.isPlaying()) {
				mMediaPlayer.pause();
				mCurrentState = STATE_PAUSED;
			}
		}
		mTargetState = STATE_PAUSED;
	}

	public void suspend() {
		release(false);
	}

	public void resume() {
		openVideo();
	}

	@Override
	public int getDuration() {
		if (isInPlaybackState()) {
			return mMediaPlayer.getDuration();
		}

		return -1;
	}

	@Override
	public int getCurrentPosition() {
		if (isInPlaybackState()) {
			return mMediaPlayer.getCurrentPosition();
		}
		return 0;
	}

	@Override
	public void seekTo(int msec) {
		if (isInPlaybackState()) {
			mMediaPlayer.seekTo(msec);
			mSeekWhenPrepared = 0;
		} else {
			mSeekWhenPrepared = msec;
		}
	}

	@Override
	public boolean isPlaying() {
		return isInPlaybackState() && mMediaPlayer.isPlaying();
	}

	@Override
	public int getBufferPercentage() {
		if (mMediaPlayer != null) {
			return mCurrentBufferPercentage;
		}
		return 0;
	}

	private boolean isInPlaybackState() {
		return (mMediaPlayer != null &&
				mCurrentState != STATE_ERROR &&
				mCurrentState != STATE_IDLE &&
				mCurrentState != STATE_PREPARING);
	}

	@Override
	public boolean canPause() {
		return mCanPause;
	}

	@Override
	public boolean canSeekBackward() {
		return mCanSeekBack;
	}

	@Override
	public boolean canSeekForward() {
		return mCanSeekForward;
	}

	@Override
	public int getAudioSessionId() {
		if (mAudioSession == 0) {
			MediaPlayer foo = new MediaPlayer();
			mAudioSession = foo.getAudioSessionId();
			foo.release();
		}
		return mAudioSession;
	}
}
