package com.metadapp.videoplayerapp;

import static com.google.android.exoplayer2.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING;

import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Rational;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.material.snackbar.Snackbar;
import com.metadapp.videoplayerapp.services.CustomOnScaleGestureListener;
import com.metadapp.videoplayerapp.services.PinchListener;
import com.metadapp.videoplayerapp.util.Widget;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private Context mContext;

    /// Player vars
    private SimpleExoPlayer player;
    private PlayerView playerView;
    private ActionBar ac;
    private static boolean floating = false;
    private TextView tv;
    private ImageView mExolock;
    private LinearLayout mExoControls1, mExoControls2, mExoControls3;
    private boolean locked = false;
    private ScaleGestureDetector scaleGestureDetector;

    /// Source data
    private String url, title;

    /// PIP Mode
    private BroadcastReceiver mReceiver;
    private static final int REQUEST_CODE = 101;

    private AppCompatSeekBar mVolumeSeek, mBrigSeek;
    private ImageView mVolImage, mBrigImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set context
        mContext = MainActivity.this;

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        zoomVideo();

        retrieveExtras();

        Toolbar toolbar = findViewById(R.id.toolbar);
        ImageView mExoPIP = findViewById(R.id.exo_pip);
        ImageView mExoRotate = findViewById(R.id.exo_rotate);

        playerView = findViewById(R.id.playerView);
        mExolock = findViewById(R.id.exo_lock);
        mExoControls1 = findViewById(R.id.exo_controls1);
        mExoControls2 = findViewById(R.id.dura_els);
        mExoControls3 = findViewById(R.id.prog_els);
        mVolumeSeek = findViewById(R.id.seekBarVol);
        mBrigSeek = findViewById(R.id.seekBarBrig);
        mVolImage = findViewById(R.id.vol_image);
        mBrigImage = findViewById(R.id.brig_image);

        configVolume();

        scaleGestureDetector = new ScaleGestureDetector(mContext, new CustomOnScaleGestureListener(
                new PinchListener() {
                    @Override
                    public void onZoomOut() {
                        if(!locked && playerView != null){
                            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                            showSnack(getString(R.string.zoom_out));
                        }
                    }

                    @Override
                    public void onZoomIn() {
                        if(!locked && playerView != null){
                            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
                            showSnack(getString(R.string.zoom_in));
                        }
                    }
                }
        ));

        mExoPIP.setOnClickListener(view -> {
            try {
                enterPIPMode();
            }catch (Exception err){}
        });

        mExoRotate.setOnClickListener(view -> {
            int orientation = getResources().getConfiguration().orientation;
            if(orientation == Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                mBrigSeek.setVisibility(View.VISIBLE);
                mBrigImage.setVisibility(View.VISIBLE);
                mVolImage.setVisibility(View.VISIBLE);
                mVolumeSeek.setVisibility(View.VISIBLE);
            }else{
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                mBrigSeek.setVisibility(View.GONE);
                mBrigImage.setVisibility(View.GONE);
                mVolImage.setVisibility(View.GONE);
                mVolumeSeek.setVisibility(View.GONE);
            }
        });

        // Set toolbar
        setSupportActionBar(toolbar);
        startPlaying(url);
    }

    private void configVolume(){
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        int volume_level = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        mVolumeSeek.setMax(max);
        mVolumeSeek.setProgress(volume_level);

        int progress = Widget.getSavedBrightness(mContext);
        mBrigSeek.setMax(255);
        mBrigSeek.setProgress(progress);

        settingBrigSeekImages(progress);
        settingVolSeekImages(volume_level);
        setBrigState(progress);

        mVolumeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setVolumeState(progress);
                settingVolSeekImages(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mBrigSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setBrigState(progress);
                settingBrigSeekImages(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void settingBrigSeekImages(int progressBrig){
        if (progressBrig <= 100)
            setImageDrawable(mBrigImage, R.drawable.ic_action_brig_low);
        else if (progressBrig <= 200)
            setImageDrawable(mBrigImage, R.drawable.ic_action_brig_med);
        else setImageDrawable(mBrigImage, R.drawable.ic_action_brig_high);
    }

    private void settingVolSeekImages(int progressVol){
        if (progressVol <= 3)
            setImageDrawable(mVolImage, R.drawable.ic_action_volume_mute);
        else if (progressVol <= 7)
            setImageDrawable(mVolImage, R.drawable.ic_action_volume_down);
        else setImageDrawable(mVolImage, R.drawable.ic_action_volume_up);
    }

    private void setImageDrawable(ImageView view, int drawable){
        view.setImageDrawable(ContextCompat.getDrawable(mContext, drawable));
    }

    private void setVolumeState(int progress){
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        am.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                progress,
                0);
    }

    private void setBrigState(int progress){
        float brightness = progress / (float)255;
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = brightness;
        getWindow().setAttributes(lp);
        Widget.putDataPref(mContext, "screen_cur_br", String.valueOf(progress));
    }

    private void showSnack(String msg){
        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator);
        Snackbar snackbar = Snackbar.make(coordinatorLayout, msg, Snackbar.LENGTH_SHORT);
        View view = snackbar.getView();
        view.setBackgroundColor(Color.parseColor("#65000000"));

        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) view.getLayoutParams();
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        view.setLayoutParams(params);
        snackbar.show();
    }

    private void startPlaying(String url){
        this.url = url;
        openPlayer();
    }

    private void retrieveExtras(){
        try {
            Uri data = getIntent().getData();
            url = data.toString();
            Cursor returnCursor = getContentResolver().query(data, null, null, null, null);
            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            returnCursor.moveToFirst();

            title = returnCursor.getString(nameIndex);
        }catch (Exception e){
            Toast.makeText(mContext, getString(R.string.error_ocurr), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void openPlayer(){
        playerView.setFastForwardIncrementMs(10000);
        playerView.setRewindIncrementMs(10000);

        mExolock.setOnClickListener(view -> {
            locked = !locked;
            mExolock.setImageDrawable(ContextCompat.getDrawable(mContext,
                    locked ? R.drawable.ic_unlock : R.drawable.ic_lock));

            lockPlayerControls(locked);
        });

       setActionBar();
       try {
           changeBarSize();
       }catch (Exception err){}

       init();
    }

    private void setActionBar(){
        ac = getSupportActionBar();
        if(ac != null)
            ac.setDisplayHomeAsUpEnabled(false);
    }

    private void changeBarSize(){
        tv = new TextView(mContext);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        tv.setLayoutParams(lp);
        tv.setText(getString(R.string.m_load));
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        tv.setMaxLines(1);
        tv.setEllipsize(TextUtils.TruncateAt.END);

        ac.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        ac.setCustomView(tv);
        ac.setDisplayHomeAsUpEnabled(true);
    }

    private void lockPlayerControls(boolean lock){
        mExoControls1.setVisibility(lock ? View.INVISIBLE : View.VISIBLE);
        mExoControls2.setVisibility(lock ? View.INVISIBLE : View.VISIBLE);
        mExoControls3.setVisibility(lock ? View.INVISIBLE : View.VISIBLE);
        showActionBar(!lock);
    }

    private void showActionBar(boolean show){
        if(ac != null)
            if (show){
                if(!ac.isShowing())
                    ac.show();
            }else if(ac.isShowing())
                ac.hide();
    }

    private void zoomVideo(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            Window window = getWindow();
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
    }

    private void init(){
        DefaultLoadControl.Builder bl = new DefaultLoadControl.Builder();
        bl.setBufferDurationsMs(3500,150000, 2500, 3000);

        SimpleExoPlayer.Builder builder = new SimpleExoPlayer.Builder(
                mContext,
                new DefaultRenderersFactory(mContext)
        ).setLoadControl(bl.createDefaultLoadControl());

        player = builder.build();
        player. setVideoScalingMode(VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        playerView.setPlayer(player);
        playerView.setOnTouchListener((view, motionEvent) -> {
            scaleGestureDetector.onTouchEvent(motionEvent);
            return false;
        });
        playerView.setKeepScreenOn(true);
        playerView.requestFocus();
        playerView.setControllerVisibilityListener(visibility -> {
            if (visibility == View.VISIBLE) {
                if (!locked)
                    showActionBar(true);
            }else showActionBar(false);
        });

        DefaultDataSourceFactory defaultDataSource =
                new DefaultDataSourceFactory(mContext, "Android");

        MediaSource mediaSource = new ProgressiveMediaSource.Factory(
                defaultDataSource
        ).createMediaSource(Uri.parse(url));

        player.prepare(mediaSource);
        player.setPlayWhenReady(true);

        MediaSessionCompat mediaSession = new MediaSessionCompat(mContext, getPackageName());
        MediaSessionConnector mediaSessionConnector = new MediaSessionConnector(mediaSession);
        mediaSessionConnector.setPlayer(player);
        mediaSession.setActive(true);

        tv.setText(title);
        if(!playerView.getUseController())
            playerView.setUseController(true);
    }

    private void releasePlayer(boolean finish){
        if(player != null){
            if (finish){
                player.release();
                playerView.setPlayer(null);
                finish();
            }else pausePlayer();
        }else finish();
    }

    private void enterPIPMode(){
        if(Widget.canPip(mContext))
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                Rational aspectRatio = new Rational(playerView.getWidth(), playerView.getHeight());
                PictureInPictureParams.Builder params = new PictureInPictureParams.Builder();
                params.setAspectRatio(aspectRatio);
                enterPictureInPictureMode(params.build());
            }else enterPictureInPictureMode();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus)
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
    }

    private void createPipAction(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            final ArrayList<RemoteAction> actions = new ArrayList<>();
            Intent actionIntent = new Intent("com.metadapp.videoplayerapp.PLAY_PAUSE");

            final PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext,
                    REQUEST_CODE, actionIntent, PendingIntent.FLAG_IMMUTABLE);

            Icon icon = Icon.createWithResource(mContext,
                    player != null && player.getPlayWhenReady() ? R.drawable.ic_pause : R.drawable.ic_play);

            RemoteAction remoteAction = new RemoteAction(icon, "Player", "Play", pendingIntent);

            actions.add(remoteAction);
            PictureInPictureParams params =
                    new PictureInPictureParams.Builder()
                    .setActions(actions)
                    .build();

            setPictureInPictureParams(params);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event){
        int keyCode = event.getKeyCode();
        if((keyCode == KeyEvent.KEYCODE_BACK ||
                keyCode == KeyEvent.KEYCODE_MENU ||
                keyCode == KeyEvent.KEYCODE_HOME) && locked) {
            return false;
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode,
                                              Configuration newConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        else super.onPictureInPictureModeChanged(isInPictureInPictureMode);

        if (isInPictureInPictureMode) {
            startPlayer();
            playerView.setUseController(false);
            floating = true;
            showActionBar(false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                IntentFilter filter = new IntentFilter();
                filter.addAction("com.metadapp.videoplayerapp.PLAY_PAUSE");
                mReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (player != null) {
                            boolean state = !player.getPlayWhenReady();
                            player.setPlayWhenReady(state);
                            createPipAction();
                        }
                    }
                };

                registerReceiver(mReceiver, filter);
                createPipAction();
            }
        } else {
            playerView.setUseController(true);
            floating = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mReceiver != null)
                unregisterReceiver(mReceiver);
        }
    }

    private void pausePlayer(){
        if(player != null){
            try {
                if(player.getPlaybackState() == Player.STATE_READY
                 && player.getPlayWhenReady())
                    player.setPlayWhenReady(false);
            }catch (Exception err){}
        }
    }

    private void startPlayer(){
        if(player != null){
            try {
                if(player.getPlaybackState() == Player.STATE_READY
                        && player.getPlayWhenReady())
                    player.setPlayWhenReady(true);
            }catch (Exception err){}
        }
    }

    @Override
    public void onPause(){
        showActionBar(true);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode() && floating)
            releasePlayer(false);
        else pausePlayer();
        super.onPause();
    }


    @Override
    public void onStop(){
        if(floating)
            releasePlayer(true);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode() && floating)
            releasePlayer(false);
        else pausePlayer();
        super.onStop();
    }

    @Override
    public void onDestroy(){
        releasePlayer(true);
        super.onDestroy();
    }

    @Override
    public void onResume(){
        super.onResume();
        startPlayer();
    }

    @Override
    public void onBackPressed(){
        releasePlayer(true);
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if(id == android.R.id.home){
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
