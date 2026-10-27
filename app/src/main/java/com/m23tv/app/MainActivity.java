package com.m23tv.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MainActivity extends Activity {
    private WebView webView;
    private FrameLayout root, playerLayer;
    private PlayerView playerView;
    private ExoPlayer player;
    private TextView playerTitle;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        root = new FrameLayout(this);
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new LivePlayerBridge(), "M23Player");
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
                return "http".equals(scheme) || "https".equals(scheme) || "mailto".equals(scheme) || "tel".equals(scheme);
            }
        });
        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));
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
        playerLayer.addView(playerView, new FrameLayout.LayoutParams(-1, -1));
        playerTitle = new TextView(this);
        playerTitle.setTextColor(Color.WHITE);
        playerTitle.setTextSize(16);
        playerTitle.setGravity(Gravity.CENTER_VERTICAL);
        playerTitle.setPadding(18, 12, 18, 12);
        playerTitle.setBackgroundColor(0x99070B12);
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        titleParams.topMargin = 18; titleParams.leftMargin = 12;
        playerLayer.addView(playerTitle, titleParams);
        Button close = new Button(this);
        close.setText("Cerrar  ✕");
        close.setTextColor(Color.WHITE);
        close.setBackgroundColor(0xDD123E69);
        close.setOnClickListener(v -> hidePlayer());
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        closeParams.topMargin = 10; closeParams.rightMargin = 10;
        playerLayer.addView(close, closeParams);
        root.addView(playerLayer, new FrameLayout.LayoutParams(-1, -1));
    }

    private final class LivePlayerBridge {
        @JavascriptInterface public void playStream(String url, String title) {
            runOnUiThread(() -> startStream(url, title));
        }
        @JavascriptInterface public void loadPlaylist(String rawUrl) {
            new Thread(() -> {
                HttpURLConnection connection = null;
                try {
                    Uri parsed = Uri.parse(rawUrl);
                    String scheme = parsed.getScheme() == null ? "" : parsed.getScheme().toLowerCase(Locale.ROOT);
                    if (!(scheme.equals("https") || scheme.equals("http")) || parsed.getHost() == null || parsed.getUserInfo() != null)
                        throw new IllegalArgumentException("La URL de la lista no es válida.");
                    connection = (HttpURLConnection) new URL(rawUrl).openConnection();
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(25000);
                    connection.setInstanceFollowRedirects(true);
                    connection.setRequestProperty("User-Agent", "M23TV/1.0 (Android playlist reader)");
                    connection.setRequestProperty("Accept", "audio/x-mpegurl, application/vnd.apple.mpegurl, text/plain, */*");
                    int status = connection.getResponseCode();
                    if (status < 200 || status >= 300) throw new Exception("El servidor respondió HTTP " + status);
                    JSONArray items = new JSONArray();
                    String name = null, group = "Otros", logo = "";
                    try (InputStream stream = connection.getInputStream();
                         BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            if (line.isEmpty()) continue;
                            if (line.startsWith("#EXTINF")) {
                                int comma = line.indexOf(',');
                                name = comma >= 0 ? line.substring(comma + 1).trim() : "Canal";
                                group = attribute(line, "group-title");
                                logo = attribute(line, "tvg-logo");
                                if (group.isEmpty()) group = "Otros";
                                if (name.isEmpty()) name = "Canal";
                            } else if (!line.startsWith("#") && name != null) {
                                Uri streamUri = Uri.parse(line);
                                String streamScheme = streamUri.getScheme() == null ? "" : streamUri.getScheme().toLowerCase(Locale.ROOT);
                                if ((streamScheme.equals("http") || streamScheme.equals("https")) && streamUri.getHost() != null && streamUri.getUserInfo() == null) {
                                    JSONObject item = new JSONObject();
                                    item.put("name", name); item.put("group", group); item.put("logo", logo); item.put("url", line);
                                    items.put(item);
                                }
                                name = null; group = "Otros"; logo = "";
                            }
                        }
                    }
                    final String json = items.toString();
                    runOnUiThread(() -> webView.evaluateJavascript("window.onPlaylistLoaded(" + JSONObject.quote(json) + ")", null));
                } catch (Exception e) {
                    final String message = e.getMessage() == null ? "No se pudo leer la lista." : e.getMessage();
                    runOnUiThread(() -> webView.evaluateJavascript("window.onPlaylistError(" + JSONObject.quote(message) + ")", null));
                } finally {
                    if (connection != null) connection.disconnect();
                }
            }, "m23-playlist-loader").start();
        }
    }

    private static String attribute(String line, String key) {
        String marker = key + "=";
        int at = line.toLowerCase(Locale.ROOT).indexOf(marker.toLowerCase(Locale.ROOT));
        if (at < 0) return "";
        int start = at + marker.length();
        if (start >= line.length()) return "";
        char quote = line.charAt(start);
        if (quote != '"' && quote != 39) return "";
        int finish = line.indexOf(quote, start + 1);
        return finish > start ? line.substring(start + 1, finish).trim() : "";
    }

    private void startStream(String rawUrl, String title) {
        try {
            Uri uri = Uri.parse(rawUrl);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost();
            String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
            boolean valid = (scheme.equals("https") || scheme.equals("http")) && host != null && !host.trim().isEmpty() && uri.getUserInfo() == null;
            if (!valid) throw new IllegalArgumentException("Dirección de señal inválida.");
            String lower = rawUrl.toLowerCase(Locale.ROOT);
            boolean hls = path.contains(".m3u8") || lower.contains(".m3u8?");
            MediaItem.Builder builder = new MediaItem.Builder().setUri(uri);
            if (hls) builder.setMimeType(MimeTypes.APPLICATION_M3U8);
            if (player == null) {
                player = new ExoPlayer.Builder(this).build();
                playerView.setPlayer(player);
                player.addListener(new Player.Listener() {
                    @Override public void onPlayerError(PlaybackException error) {
                        Toast.makeText(MainActivity.this, "La señal no respondió. Puede estar caída, restringida o requerir autorización.", Toast.LENGTH_LONG).show();
                    }
                });
            }
            player.stop();
            player.clearMediaItems();
            playerTitle.setText(title == null || title.trim().isEmpty() ? "M23 TV · En vivo" : title);
            playerLayer.setVisibility(View.VISIBLE);
            player.setMediaItem(builder.build());
            player.prepare();
            player.play();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage() == null ? "No se pudo iniciar la señal." : e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void hidePlayer() { if (player != null) player.pause(); if (playerLayer != null) playerLayer.setVisibility(View.GONE); }
    @Override public void onBackPressed() {
        if (playerLayer != null && playerLayer.getVisibility() == View.VISIBLE) hidePlayer();
        else if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
    @Override protected void onPause() { if (player != null) player.pause(); super.onPause(); }
    @Override protected void onDestroy() {
        if (player != null) { player.release(); player = null; }
        if (webView != null) { webView.removeJavascriptInterface("M23Player"); webView.destroy(); }
        super.onDestroy();
    }
}