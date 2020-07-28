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

import timber.log.Timber;

public class WebConfigActivity extends AppCompatActivity {

    public static final String EXTRA_WEB_PAGE = "webpage";

    private FrameLayout webView;
    private WebView configView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Timber.i("Running WebConfigActivity::onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_config);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            Timber.d("SupportActionBar was null");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        webView = findViewById(R.id.webview);

        SwipeRefreshLayout refreshLayout = findViewById(R.id.swipeContainer);

        configView = new WebView(this);
        refreshLayout.setOnRefreshListener(configView::reload);
        configView.setWebChromeClient(new WebChromeClient());
        configView.setWebViewClient(new WebViewClient());
        configureWebView();

        // Load designated web-page
        String url = getIntent().getStringExtra(EXTRA_WEB_PAGE);
        Timber.d("Loading URL=%S", url);
        configView.loadUrl(url);

        // Make WebView visible
        webView.addView(configView);

        Timber.i("Finished WebConfigActivity::onCreate()");
    }

    private void configureWebView() {
        configView.setFitsSystemWindows(true);
        WebView.setWebContentsDebuggingEnabled(false);

        WebSettings webSettings = configView.getSettings();
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowContentAccess(false);
        webSettings.setAllowFileAccess(false);
        webSettings.setAllowFileAccessFromFileURLs(false);
        webSettings.setAllowUniversalAccessFromFileURLs(false);
        webSettings.setAppCacheEnabled(false);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        Timber.d("WebSettings:");
        Timber.d("JavaScript can open Windows automatically: %B\n", webSettings.getJavaScriptCanOpenWindowsAutomatically());
        Timber.d("JavaScript enabled: %B\n", webSettings.getJavaScriptEnabled());
        Timber.d("Allow Content-Access: %B\n", webSettings.getAllowContentAccess());
        Timber.d("Allow File-Access: %B\n", webSettings.getAllowFileAccess());
        Timber.d("Allow File-Access from File-URLs: %B\n", webSettings.getAllowFileAccessFromFileURLs());
        Timber.d("Allow Universal-Access from File-URLs: %B\n", webSettings.getAllowUniversalAccessFromFileURLs());
        Timber.d("Cache-Mode: %d\n", webSettings.getCacheMode());
    }

    @Override
    public boolean onSupportNavigateUp() {
        configView.stopLoading();
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
        Timber.i("Running WebConfigActivity::onDestroy()");

        if (configView != null) {
            /*
             * Purge everything about this web-session, so we don't get problems later-on
             * because of cached elements
             */
            configView.stopLoading();
            configView.clearHistory();
            configView.clearCache(true);
            configView.clearSslPreferences();
            configView.destroy();
            configView = null;
        }

        if (webView != null) {
            webView.removeAllViews();
            webView = null;
        }

        Timber.i("Finished custom part of WebConfigActivity::onDestroy()");

        super.onDestroy();
    }
}
