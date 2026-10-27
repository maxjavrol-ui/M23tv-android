package com.m23tv.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class MainActivity extends Activity {
    private WebView webView;
    private FrameLayout root;
    private FrameLayout playerLayer;
    private PlayerView playerView;
    private ExoPlayer player;
    private TextView playerTitle;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        root = new FrameLayout(this);
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new LivePlayerBridge(), "M23Player");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // No sacar al usuario de M23 TV a una página externa.
                Uri uri = request.getUrl();
                String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
                return "http".equals(scheme) || "https".equals(scheme)
                    || "mailto".equals(scheme) || "tel".equals(scheme);
            }
        });
        root.addView(webView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        buildPlayerLayer();
        setContentView(root);
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void buildPlayerLayer() {
        playerLayer = new FrameLayout(this);
        playerLayer.setBackgroundColor(Color.BLACK);
        playerLayer.setVisibility(View.GONE);

        playerView = new PlayerView(this);
        playerView.setUseController(true);
        playerView.setKeepContentOnPlayerReset(true);
        playerLayer.addView(playerView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        playerTitle = new TextView(this);
        playerTitle.setTextColor(Color.WHITE);
        playerTitle.setTextSize(16);
        playerTitle.setGravity(Gravity.CENTER_VERTICAL);
        playerTitle.setPadding(18, 12, 18, 12);
        playerTitle.setBackgroundColor(0x99070B12);
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP | Gravity.START);
        titleParams.topMargin = 18;
        titleParams.leftMargin = 12;
        playerLayer.addView(playerTitle, titleParams);

        Button close = new Button(this);
        close.setText("Cerrar  ✕");
        close.setTextColor(Color.WHITE);
        close.setBackgroundColor(0xDD123E69);
        close.setOnClickListener(v -> hidePlayer());
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP | Gravity.END);
        closeParams.topMargin = 10;
        closeParams.rightMargin = 10;
        playerLayer.addView(close, closeParams);

        root.addView(playerLayer, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private final class LivePlayerBridge {
        @JavascriptInterface
        public void playStream(String url, String title) {
            runOnUiThread(() -> startStream(url, title));
        }
    }

    private void startStream(String rawUrl, String title) {
        final Uri uri;
        try {
            uri = Uri.parse(rawUrl);
        } catch (Exception e) {
            Toast.makeText(this, "Dirección de señal inválida.", Toast.LENGTH_LONG).show();
            return;
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        String host = uri.getHost();
        String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase();
        boolean validWebUrl = ("https".equals(scheme) || "http".equals(scheme))
            && host != null && !host.trim().isEmpty() && uri.getUserInfo() == null;
        boolean hls = path.contains(".m3u8")
            || (uri.getQuery() != null && uri.getQuery().toLowerCase().contains(".m3u8"));

        if (!validWebUrl || !hls) {
            Toast.makeText(this,
                "Este canal no tiene una dirección directa HLS disponible para reproducir.",
                Toast.LENGTH_LONG).show();
            return;
        }

        try {
            if (player == null) {
                player = new ExoPlayer.Builder(this).build();
                playerView.setPlayer(player);
                player.addListener(new Player.Listener() {
                    @Override
                    public void onPlayerError(PlaybackException error) {
                        Toast.makeText(MainActivity.this,
                            "La señal no respondió. Puede estar caída, restringida o requerir autorización.",
                            Toast.LENGTH_LONG).show();
                    }
                });
            }

            player.stop();
            player.clearMediaItems();
            playerTitle.setText(title == null || title.trim().isEmpty() ? "M23 TV · En vivo" : title);
            playerLayer.setVisibility(View.VISIBLE);

            MediaItem item = new MediaItem.Builder()
                .setUri(uri)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build();
            player.setMediaItem(item);
            player.prepare();
            player.play();
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo iniciar el reproductor integrado.", Toast.LENGTH_LONG).show();
        }
    }

    private void hidePlayer() {
        if (player != null) player.pause();
        if (playerLayer != null) playerLayer.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (playerLayer != null && playerLayer.getVisibility() == View.VISIBLE) {
            hidePlayer();
        } else if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        if (player != null) player.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        if (webView != null) {
            webView.removeJavascriptInterface("M23Player");
            webView.destroy();
        }
        super.onDestroy();
    }
}
