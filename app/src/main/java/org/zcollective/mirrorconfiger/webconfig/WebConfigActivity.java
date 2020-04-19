package org.zcollective.mirrorconfiger.webconfig;

import android.os.Bundle;

import androidx.annotation.Nullable;
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
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_config);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        webview = findViewById(R.id.webview);

        SwipeRefreshLayout refreshLayout = findViewById(R.id.swipeContainer);

        configView = new WebView(this);
        refreshLayout.setOnRefreshListener(configView::reload);
        configView.setWebChromeClient(new WebChromeClient());
        configView.setWebViewClient(new WebViewClient());
        configureWebView();
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
        finish();
        onBackPressed();
        return true;
    }

    @Override
    public void onResume() {
        if (configView != null) configView.resumeTimers();
        super.onResume();
    }

    @Override
    public void onPause() {
        if (configView != null) configView.pauseTimers();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (configView != null) {
            configView.stopLoading();
            configView.clearHistory();
            configView.clearCache(true);
            configView.clearSslPreferences();
            configView.destroy();
            configView = null;
        }

        webview.removeAllViews();
        webview = null;
        super.onDestroy();
    }
}
