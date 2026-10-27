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
import android.content.Intent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {
    private WebView webView;
    private FrameLayout root;
    private FrameLayout playerLayer;
    private PlayerView playerView;
    private ExoPlayer player;
    private TextView playerTitle;

    private static final Set<String> PUBLIC_STREAM_HOSTS = new HashSet<>(Arrays.asList(
        "live.tvpublica.ar",
        "live.telefe.com",
        "streaming.eltrece.com.ar",
        "stream-gtlc.telecentro.net.ar"
    ));

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
                Uri uri = request.getUrl();
                String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
                if ("http".equals(scheme) || "https".equals(scheme)) {
                    openExternal(uri);
                    return true;
                }
                if ("mailto".equals(scheme) || "tel".equals(scheme)) {
                    openExternal(uri);
                    return true;
                }
                return false;
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
            runOnUiThread(() -> startPublicStream(url, title));
        }
    }

    private void startPublicStream(String rawUrl, String title) {
        Uri uri;
        try {
            uri = Uri.parse(rawUrl);
        } catch (Exception e) {
            Toast.makeText(this, "Dirección de señal inválida", Toast.LENGTH_LONG).show();
            return;
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
        String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase();
        if (!"https".equals(scheme) || !PUBLIC_STREAM_HOSTS.contains(host) || !path.contains(".m3u8")) {
            Toast.makeText(this, "Esta fuente no está habilitada en el reproductor integrado.", Toast.LENGTH_LONG).show();
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
                            "No se pudo reproducir esta señal. Puede estar fuera de línea o restringida.",
                            Toast.LENGTH_LONG).show();
                    }
                });
            }
            playerTitle.setText(title == null || title.trim().isEmpty() ? "M23 TV · En vivo" : title);
            playerLayer.setVisibility(View.VISIBLE);
            player.setMediaItem(MediaItem.fromUri(uri));
            player.prepare();
            player.play();
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo iniciar el reproductor.", Toast.LENGTH_LONG).show();
        }
    }

    private void hidePlayer() {
        if (player != null) player.pause();
        if (playerLayer != null) playerLayer.setVisibility(View.GONE);
    }

    private void openExternal(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception ignored) {
            Toast.makeText(this, "No hay una aplicación disponible para abrir este enlace.", Toast.LENGTH_SHORT).show();
        }
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
