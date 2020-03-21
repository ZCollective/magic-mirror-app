package org.zcollective.mirrorconfiger.webconfig;

import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.zcollective.mirrorconfiger.R;

import java.util.Objects;

public class WebConfigActivity extends AppCompatActivity {//WifiBaseActivity {

//    @Override
//    protected int getSecondsTimeout() {
//        return 10;
//    }
//
//    @Override
//    protected String getWifiSSID() {
//        return ssid;
//    }
//
//    @Override
//    protected String getWifiPass() {
//        return pass;
//    }

//    private String ssid;
//    private String pass;

    private WebView configView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_config);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        ssid = getIntent().getStringExtra("SSID");
//        pass = getIntent().getStringExtra("PASS");
//        boolean hidden = getIntent().getBooleanExtra("HIDDEN", true);

        Objects.requireNonNull(getSupportActionBar());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

//        handleWIFI();

        configView = findViewById(R.id.webview);

        SwipeRefreshLayout refreshLayout = findViewById(R.id.swipeContainer);

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
        configView.clearCache(true);
        configView.getSettings().setJavaScriptEnabled(true);
        configView.loadUrl("http://192.168.12.1:8080/");
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
        if (configView != null) configView.destroy();
        configView = null;
        super.onDestroy();
    }
}
