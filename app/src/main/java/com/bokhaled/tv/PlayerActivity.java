package com.bokhaled.tv;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PlayerActivity extends Activity {

    private ExoPlayer player;
    private PlayerView playerView;
    private DefaultHttpDataSource.Factory httpFactory;

    private ArrayList<String> names;
    private ArrayList<String> urls;
    private ArrayList<String> referrers;
    private int currentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        names = getIntent().getStringArrayListExtra("names");
        urls = getIntent().getStringArrayListExtra("urls");
        referrers = getIntent().getStringArrayListExtra("referrers");
        currentIndex = getIntent().getIntExtra("index", 0);

        if (urls == null || urls.isEmpty()) {
            Toast.makeText(this, "قائمة القنوات غير موجودة", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (names == null) names = new ArrayList<>();
        if (referrers == null) referrers = new ArrayList<>();

        if (currentIndex < 0 || currentIndex >= urls.size()) currentIndex = 0;

        playerView = new PlayerView(this);
        playerView.setUseController(false);
        playerView.setKeepScreenOn(true);
        setContentView(playerView);

        initPlayer();
        playCurrent(false);
    }

    private void initPlayer() {
        if (player != null) return;

        httpFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Android TV)");

        DefaultMediaSourceFactory mediaSourceFactory =
                new DefaultMediaSourceFactory(httpFactory);

        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(mediaSourceFactory)
                .build();

        playerView.setPlayer(player);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                Toast.makeText(
                        PlayerActivity.this,
                        "القناة ما اشتغلت حالياً — جرّب فوق أو تحت",
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void playCurrent(boolean showName) {
        if (urls == null || urls.isEmpty()) return;

        String url = urls.get(currentIndex);
        String referrer = currentIndex < referrers.size()
                ? referrers.get(currentIndex)
                : "";

        Map<String, String> headers = new HashMap<>();
        if (referrer != null && !referrer.trim().isEmpty()) {
            headers.put("Referer", referrer);
        }
        httpFactory.setDefaultRequestProperties(headers);

        MediaItem item = MediaItem.fromUri(url);
        player.setMediaItem(item);
        player.prepare();
        player.play();

        if (showName) {
            String name = currentIndex < names.size()
                    ? names.get(currentIndex)
                    : "القناة";
            Toast.makeText(this, name, Toast.LENGTH_SHORT).show();
        }
    }

    private void nextChannel() {
        if (urls == null || urls.isEmpty()) return;
        currentIndex++;
        if (currentIndex >= urls.size()) currentIndex = 0;
        playCurrent(true);
    }

    private void previousChannel() {
        if (urls == null || urls.isEmpty()) return;
        currentIndex--;
        if (currentIndex < 0) currentIndex = urls.size() - 1;
        playCurrent(true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }

        // فوق = القناة السابقة
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            previousChannel();
            return true;
        }

        // تحت = القناة التالية
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            nextChannel();
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_ENTER) {
            if (player != null) {
                if (player.isPlaying()) player.pause();
                else player.play();
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (player == null && urls != null && !urls.isEmpty()) {
            initPlayer();
            playCurrent(false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (player != null) {
            player.release();
            player = null;
        }
    }
}
