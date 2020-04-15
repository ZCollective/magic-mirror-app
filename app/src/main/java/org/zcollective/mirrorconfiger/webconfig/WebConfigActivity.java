package org.zcollective.mirrorconfiger.webconfig;

import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import org.zcollective.mirrorconfiger.R;

import java.util.Objects;

public class WebConfigActivity extends AppCompatActivity {

    public static final String EXTRA_WEBPAGE = "webpage";

    private WebView configView;
    private FrameLayout webview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_config);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

//        configView = findViewById(R.id.webview);
        webview = findViewById(R.id.webview);

        SwipeRefreshLayout refreshLayout = findViewById(R.id.swipeContainer);

        configView = new WebView(this);
        refreshLayout.setOnRefreshListener(configView::reload);
        configView.setWebChromeClient(new WebChromeClient());
        configView.setWebViewClient(new WebViewClient() {

            private int running = 0; // Could be public if you want a timer to check.

            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, String urlNewString) {
                running++;
                webView.loadUrl(urlNewString);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                running = Math.max(running, 1); // First request move it to 1.
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if(--running == 0) { // just "running--;" if you add a timer.
                    // TODO: finished... if you want to fire a method.
                    refreshLayout.setRefreshing(false);
                }
            }


//            @Override
//            public void onPageFinished(WebView view, String url) {
//                // do your stuff here
//                refreshLayout.setRefreshing(false);
//            }
        });
//        configView.clearCache(true);
        configureWebView();
//        configView.getSettings().setJavaScriptEnabled(true);
//        configView.loadUrl("http://192.168.12.1:8080/");

//        configView.loadUrl(BuildConfig.MIRROR_SETUP_PAGE);
        configView.loadUrl(getIntent().getStringExtra(EXTRA_WEBPAGE));
        webview.addView(configView);
    }

    private void configureWebView() {
        WebSettings webSettings = configView.getSettings();
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowContentAccess(false);
        webSettings.setAllowFileAccess(false);
        webSettings.setAllowFileAccessFromFileURLs(false);
        webSettings.setAllowUniversalAccessFromFileURLs(false);
        webSettings.setAppCacheEnabled(false);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        configView.stopLoading();
//        configView.destroy();
        finish();
        onBackPressed();
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (configView != null) configView.resumeTimers();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (configView != null) configView.pauseTimers();
    }

    @Override
    protected void onDestroy() {
//        if (configView != null) configView.destroy();
//        configView = null;
        super.onDestroy();
        configView.clearHistory();
        configView.clearCache(true);
        configView.clearSslPreferences();
        webview.removeAllViews();
        configView.destroy();
        configView = null;
        webview = null;
    }
}
