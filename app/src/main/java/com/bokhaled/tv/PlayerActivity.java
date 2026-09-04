package com.bokhaled.tv;

import android.app.Activity;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.util.HashMap;
import java.util.Map;

public class PlayerActivity extends Activity {
    private VideoView video;
    private ProgressBar progress;
    private TextView nameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        String url = getIntent().getStringExtra("url");
        String name = getIntent().getStringExtra("name");
        String referrer = getIntent().getStringExtra("referrer");

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        video = new VideoView(this);
        root.addView(video, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        progress = new ProgressBar(this);
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(80,80);
        pp.gravity = Gravity.CENTER;
        root.addView(progress, pp);

        nameView = new TextView(this);
        nameView.setText(name == null ? "" : name);
        nameView.setTextColor(Color.WHITE);
        nameView.setTextSize(26);
        nameView.setBackgroundColor(0x99000000);
        nameView.setPadding(24,14,24,14);
        FrameLayout.LayoutParams np = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        np.gravity = Gravity.TOP | Gravity.RIGHT;
        np.setMargins(20,20,20,20);
        root.addView(nameView, np);

        setContentView(root);

        try {
            Map<String,String> headers = new HashMap<>();
            headers.put("User-Agent", "Mozilla/5.0 (Android TV)");
            if (referrer != null && !referrer.isEmpty()) {
                headers.put("Referer", referrer);
            }
            video.setVideoURI(Uri.parse(url), headers);
            video.setOnPreparedListener(mp -> {
                progress.setVisibility(View.GONE);
                mp.setOnInfoListener((m, what, extra) -> {
                    if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) progress.setVisibility(View.VISIBLE);
                    if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) progress.setVisibility(View.GONE);
                    return false;
                });
                video.start();
                nameView.postDelayed(() -> nameView.setVisibility(View.GONE), 3500);
            });
            video.setOnErrorListener((mp, what, extra) -> {
                progress.setVisibility(View.GONE);
                Toast.makeText(PlayerActivity.this,
                        "القناة ما اشتغلت حالياً — جرّبها بعد شوي",
                        Toast.LENGTH_LONG).show();
                return true;
            });
            video.requestFocus();
        } catch (Exception e) {
            Toast.makeText(this, "تعذر تشغيل القناة", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            if (video.isPlaying()) video.pause(); else video.start();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (video != null) video.pause();
    }

    @Override
    protected void onDestroy() {
        if (video != null) video.stopPlayback();
        super.onDestroy();
    }
}
