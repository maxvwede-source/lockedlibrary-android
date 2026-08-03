package com.lockedlibrary.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

/**
 * LockedLibrary — protected reading app.
 *
 * FLAG_SECURE is the core anti-screenshot layer: on Android it makes
 * screenshots and screen-recording render black inside this activity.
 * Copy/paste/share is blocked at the WebView level, and the reader page
 * also blocks right-click/print server-side.
 */
public class MainActivity extends Activity {

    private WebView webView;
    private ProgressBar progressBar;
    private android.widget.TextView errorText;
    private android.widget.Button retryButton;
    private android.widget.LinearLayout root;
    private String appUrl;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // === THE screenshot/recording blocker =============================
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        webView = new WebView(this);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);

        // Stack: progress bar on top, WebView filling the rest
        root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.addView(progressBar, new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(webView, new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        errorText = new android.widget.TextView(this);
        errorText.setText("Can't connect\nPlease check your internet and try again.");
        errorText.setTextColor(0xFFE2E8F0);
        errorText.setGravity(android.view.Gravity.CENTER);
        errorText.setTextSize(18);
        errorText.setPadding(48, 24, 48, 24);

        retryButton = new android.widget.Button(this);
        retryButton.setText("Retry");
        retryButton.setAllCaps(false);
        retryButton.setTextSize(16);
        android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
        btnBg.setColor(0xFF0EA5E9);
        btnBg.setCornerRadius(40);
        retryButton.setBackground(btnBg);
        retryButton.setTextColor(0xFFFFFFFF);
        android.widget.LinearLayout.LayoutParams btnLp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        btnLp.gravity = android.view.Gravity.CENTER;
        retryButton.setLayoutParams(btnLp);
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retryButton.setVisibility(View.GONE);
                errorText.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                webView.setVisibility(View.VISIBLE);
                webView.loadUrl(appUrl);
            }
        });
        retryButton.setVisibility(View.GONE);
        errorText.setVisibility(View.GONE);

        root.addView(errorText, new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(retryButton, new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));

        setContentView(root);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setAllowFileAccess(false);          // no local file reads
        s.setAllowContentAccess(false);       // no content:// reads
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Block text selection / copy context menu (anti-leak).
        // Long-press will no longer pop the copy/paste menu.
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true; // swallow the event -> no context menu
            }
        });

        // Keep login session cookies alive
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(5);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equals(scheme) || "https".equals(scheme)) {
                    return false; // let the WebView load it in-app
                }
                // Block everything else (intents, file:, etc.)
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        android.webkit.WebResourceError error) {
                // Only show a full error screen if the main frame fails.
                if (request.isForMainFrame()) {
                    showErrorScreen();
                }
            }
        });

        // Block downloads — no way to save book files to the phone
        webView.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }
        });

        webView.setDownloadListener(new android.webkit.DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimetype, long contentLength) {
                Toast.makeText(MainActivity.this,
                        "Downloads are disabled in this app.", Toast.LENGTH_SHORT).show();
            }
        });

        appUrl = getString(R.string.app_url);
        webView.loadUrl(appUrl);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    private void showErrorScreen() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                webView.setVisibility(View.GONE);
                errorText.setVisibility(View.VISIBLE);
                retryButton.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeAllViews();
            webView.destroy();
        }
        super.onDestroy();
    }
}
