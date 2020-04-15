package org.zcollective.mirrorconfiger.qrscanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.zcollective.mirrorconfiger.BuildConfig;
import org.zcollective.mirrorconfiger.R;
import org.zcollective.mirrorconfiger.util.runtimepermissions.CameraPermissionHelper;
import org.zcollective.mirrorconfiger.util.runtimepermissions.CameraPermissionHelper.CameraPermissionCallback;
import org.zcollective.mirrorconfiger.util.wifi.WifiDialogHelper;
import org.zcollective.mirrorconfiger.util.wifi.WifiDialogHelper.OnDialogCallback;
import org.zcollective.mirrorconfiger.webconfig.WebConfigActivity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Objects;

import static org.zcollective.mirrorconfiger.qrscanner.QrScannerActivity.QrFlowState.ERROR;
import static org.zcollective.mirrorconfiger.qrscanner.QrScannerActivity.QrFlowState.READY;

public class QrScannerActivity extends AppCompatActivity implements CameraPermissionCallback, OnDialogCallback {

    // TODO: manage states correctly

    @Override
    public void onCameraPermissionGranted() {
        scanningJob();
    }

    @Override
    public void onCameraPermissionDenied() {
        Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) wifiDialogHelper.showGrantPermissonsDialog();
    }

    @Override
    public void onProceedConnectingToWifi() {
        String[] wifiResult = result.split(";");
        String password = "";
        for (String asdf : wifiResult) {
            if (asdf.startsWith("P:")) {
                password = asdf.substring(2);
            }
        }
        connectToWifi("Mirror", password);
    }

    @Override
    public void onStopConnectingToWifi() {
        onSupportNavigateUp();
        wifiDialogHelper.dismissProceedToConnectDialog();
    }

    @Override
    public void onProceedToEnableWifi() {
//        if (state == QrFlowState.RECEIVED_QR_CODE) {
            // open settings screen
//            state = QrFlowState.REQUESTING_WIFI_ENABLE;
            startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), REQUEST_ENABLE_WIFI);
//        }
    }

    @Override
    public void onStopToEnableWifi() {
        Log.w(TAG, "User declined to enable Wifi. Stopping Activity.");
        transitionInternalState(true);
        finish();
    }

    @Override
    public void onProceedGrantingPermissions() {
        if (cameraPermissionHelper != null) {
            cameraPermissionHelper.checkCameraPermissions(QrScannerActivity.this);
        }
    }

    @Override
    public void onStopGrantingPermissions() {
        Log.d(TAG, "User doesn't want to grant permissions");
    }

    static class WifiBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.wtf(TAG, "Received a Broadcast!");
        }
    }

    private static final String TAG = "QrScanner";

    // Requests start at 1000
    private static final int REQUEST_ENABLE_WIFI = 1000;

    private volatile boolean connecting = false;

    private WifiDialogHelper wifiDialogHelper;
    private CameraPermissionHelper cameraPermissionHelper;
    private ConnectivityManager connMgr;
    private CodeScanner mCodeScanner;
    private ProgressBar bar;
    private String SSID;
    private String PW;
    private String result;
    private WifiManager wifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        bar = QrScannerActivity.this.findViewById(R.id.webconfig_loading);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

//        if (Build.VERSION.SDK_INT >= 26) {
            connMgr = (ConnectivityManager) getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);

//            NetworkRequest.Builder request = new NetworkRequest.Builder();
//            request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
//
//            connMgr.requestNetwork(request.build(), new ConnectivityManager.NetworkCallback() {
//
//                @Override
//                public void onAvailable(Network network) {
//                    Log.wtf(TAG, "NetworkCallback::onAvailable");
//                    Log.d(TAG, "SSID: " + wifiManager.getConnectionInfo().getSSID());
//
////                    if (connecting) {
////                    if (Objects.equals(wifiManager.getConnectionInfo().getSSID(), "<unknown ssid>") || connecting) {
//                    if (Objects.equals(wifiManager.getConnectionInfo().getSSID(), "\"Mirror\"") ||
//                        connecting) {
//
//                        runOnUiThread(() -> bar.setVisibility(View.VISIBLE));
//                        connecting = false;
//
//                        connMgr.bindProcessToNetwork(network);
//
//
//                        // Pinging webserver-host, so routing-table is filled with correct host
//                        try {
//                            InetAddress ping = network.getByName(BuildConfig.MIRROR_SETUP_IP);
//                            Log.wtf(TAG, "Ping is local address: " + ping.isAnyLocalAddress());
//                            long start = System.currentTimeMillis();
//                            Log.wtf(TAG, "Reachable? " + ping.isReachable(3000));
//                            Log.wtf(TAG, "Ping took: " + (System.currentTimeMillis() - start));
//                            Log.wtf(TAG, "IP: " + Arrays.toString(ping.getAddress()));
//                        } catch (Exception e) {
//                            Log.wtf(TAG, "WTF happened?");
//                            e.printStackTrace();
//                        }
//
//                        Intent intent = new Intent(QrScannerActivity.this, WebConfigActivity.class);
//                        intent.putExtra(WebConfigActivity.EXTRA_WEBPAGE, BuildConfig.MIRROR_SETUP_PAGE);
//
//                        runOnUiThread(() -> {
//                            Log.wtf(TAG, "Starting Webview!");
//                            bar.setVisibility(View.GONE);
//                            startActivity(intent);
//                        });
//                    }
//                }
//
//                @Override
//                public void onLosing(Network network, int maxMsToLive) {
//                    Log.wtf(TAG, "NetworkCallback::onLosing");
//                    Log.wtf(TAG, "Will be online for another " + maxMsToLive + " ms");
//                }
//
//                @Override
//                public void onLost(Network network) {
//                    Log.wtf(TAG, "NetworkCallback::onLost");
//                    if (connecting) {
//                        wifiManager.enableNetwork(netId, true);
//                        wifiManager.reconnect();
//                    }
//                }
//            });
//        }
        initializeWifiStateTracker();

        // Fragments
        initializeCameraPermissionHelper();
        initializeWifiDialogHelper();
    }

    private NetworkRequest buildNetworkRequest() {
        NetworkRequest.Builder request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
//        request.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET);
//        request.removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        return request.build();
    }

    @IntDef({
            ERROR,
            READY,
            QrFlowState.SCANNING,
            QrFlowState.RECEIVED_QR_CODE,
            QrFlowState.REQUESTING_WIFI_ENABLE,
            QrFlowState.CONNECTING_TO_WIFI,
            QrFlowState.WIFI_CONNECTED,
            QrFlowState.STARTING_PORTAL,
    })
    @Retention(value = RetentionPolicy.SOURCE)
    @interface QrFlowState {
        int ERROR = -1;
        int READY = 0;
        int SCANNING = 1;
        int RECEIVED_QR_CODE = 2;
        int REQUESTING_WIFI_ENABLE = 3;
        int CONNECTING_TO_WIFI = 4;
        int WIFI_CONNECTED = 5;
        int STARTING_PORTAL = 6;
    }

    @QrFlowState
    private int state = READY;
    private ConnectivityManager.NetworkCallback ncManager;

    private void initializeWifiStateTracker() {
        ncManager = new ConnectivityManager.NetworkCallback() {

            @Override
            public void onAvailable(@NonNull Network network) {
                Log.wtf(TAG, "NetworkCallback::onAvailable");
                Log.d(TAG, "SSID: " + wifiManager.getConnectionInfo().getSSID());

//                    if (connecting) {
//                    if (Objects.equals(wifiManager.getConnectionInfo().getSSID(), "<unknown ssid>") || connecting) {
                if (Objects.equals(wifiManager.getConnectionInfo().getSSID(), "\"Mirror\"") ||
                    state == QrFlowState.CONNECTING_TO_WIFI) {
                    state = QrFlowState.WIFI_CONNECTED;

                    runOnUiThread(() -> bar.setVisibility(View.VISIBLE));

                    connMgr.bindProcessToNetwork(network);

                    // Pinging webserver-host, so routing-table is filled with correct host
                    try {
                        InetAddress ping = network.getByName(BuildConfig.MIRROR_SETUP_IP);
                        Log.wtf(TAG, "Ping is local address: " + ping.isAnyLocalAddress());
                        long start = System.currentTimeMillis();
                        Log.wtf(TAG, "Reachable? " + ping.isReachable(3000));
                        Log.wtf(TAG, "Ping took: " + (System.currentTimeMillis() - start));
                        Log.wtf(TAG, "IP: " + Arrays.toString(ping.getAddress()));
                    } catch (Exception e) {
                        Log.wtf(TAG, "WTF happened?", e);
                        state = ERROR;
                        return;
                    }

                    state = QrFlowState.STARTING_PORTAL;

                    Intent intent = new Intent(QrScannerActivity.this, WebConfigActivity.class);
                    intent.putExtra(WebConfigActivity.EXTRA_WEBPAGE, BuildConfig.MIRROR_SETUP_PAGE);

                    runOnUiThread(() -> {
                        Log.wtf(TAG, "Starting Webview!");
                        bar.setVisibility(View.GONE);
                        startActivity(intent);
                    });
                }
            }

            @Override
            public void onLosing(@NonNull Network network, int maxMsToLive) {
                Log.wtf(TAG, "NetworkCallback::onLosing");
                Log.wtf(TAG, "Will be online for another " + maxMsToLive + " ms");
            }

            @Override
            public void onLost(@NonNull Network network) {
                Log.wtf(TAG, "NetworkCallback::onLost");
                if (state == QrFlowState.CONNECTING_TO_WIFI) {
                    wifiManager.enableNetwork(netId, true);
                    wifiManager.reconnect();
                }
            }
        };

        connMgr.requestNetwork(buildNetworkRequest(), ncManager);
    }

    private void transitionInternalState(boolean error) {
        if (error) {
            // TODO: how can we tackle this?
//            Log.i(TAG, "State change: ERROR -> ERROR");
            state = ERROR;
            return;
        }

        switch (state) {
            case QrFlowState.ERROR:
                Log.i(TAG, "State change: ERROR -> ERROR");
                break;
            case QrFlowState.READY:
                Log.i(TAG, "State change: READY -> SCANNING");
                break;
            case QrFlowState.SCANNING:
                Log.i(TAG, "State change: SCANNING -> RECEIVED_QR_CODE");
                break;
            case QrFlowState.RECEIVED_QR_CODE:
                Log.i(TAG, "State change: RECEIVED_QR_CODE -> REQUESTING_WIFI_ENABLE");
                break;
            case QrFlowState.REQUESTING_WIFI_ENABLE:
                Log.i(TAG, "State change: REQUESTING_WIFI_ENABLE -> CONNECTING_TO_WIFI");
                break;
            case QrFlowState.CONNECTING_TO_WIFI:
                Log.i(TAG, "State change: CONNECTING_TO_WIFI -> WIFI_CONNECTED");
                break;
            case QrFlowState.WIFI_CONNECTED:
                Log.i(TAG, "State change: WIFI_CONNECTED -> STARTING_PORTAL");
                break;
            case QrFlowState.STARTING_PORTAL:
                Log.i(TAG, "State change: STARTING_PORTAL -> STARTING_PORTAL");
                break;
        }
    }

    private void initializeWifiDialogHelper() {
        wifiDialogHelper =
                (WifiDialogHelper) getSupportFragmentManager().findFragmentByTag(WifiDialogHelper.TAG);

        if (wifiDialogHelper == null) {
            wifiDialogHelper = new WifiDialogHelper();
            getSupportFragmentManager().beginTransaction()
                                       .add(wifiDialogHelper, WifiDialogHelper.TAG)
                                       .commitNow();
        }
    }

    private void initializeCameraPermissionHelper() {
        cameraPermissionHelper =
                (CameraPermissionHelper) getSupportFragmentManager().findFragmentByTag(CameraPermissionHelper.TAG);

        if (cameraPermissionHelper == null) {
            cameraPermissionHelper = CameraPermissionHelper.newInstance();
            getSupportFragmentManager().beginTransaction()
                                       .add(cameraPermissionHelper, CameraPermissionHelper.TAG)
                                       .commitNow();
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // This starts the whole process
        cameraPermissionHelper.checkCameraPermissions(this);
    }

    private void scanningJob() {
        CodeScannerView scannerView = findViewById(R.id.scanner_view);

        mCodeScanner = new CodeScanner(this, scannerView);
        mCodeScanner.setDecodeCallback(result -> runOnUiThread(() -> {
            if (state == QrFlowState.SCANNING) {
                // TODO: check if text from qr-code is correct
//                state = QrFlowState.RECEIVED_QR_CODE;
                transitionInternalState(false);
                Toast.makeText(QrScannerActivity.this, result.getText(), Toast.LENGTH_SHORT).show();
                QrScannerActivity.this.result = result.getText();
                wifiDialogHelper.showProceedToConnectDialog();
            }
        }));

        transitionInternalState(false);
//        state = QrFlowState.SCANNING;

        scannerView.setOnClickListener(view -> mCodeScanner.startPreview());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Making sure we clean references on destroy
        cameraPermissionHelper = null;
//        connMgr.unregisterNetworkCallback(ncManager);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCodeScanner != null) mCodeScanner.startPreview();
        connMgr.requestNetwork(buildNetworkRequest(), ncManager);
    }

    @Override
    protected void onPause() {
        if (mCodeScanner != null) mCodeScanner.releaseResources();
        connMgr.unregisterNetworkCallback(ncManager);
        super.onPause();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);

        if (requestCode == REQUEST_ENABLE_WIFI && resultCode == 0) {
            if (wifiManager.isWifiEnabled() || wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
                connectToSpecificNetwork();
                return;
            }
        }
        finish();
    }

    private void connectToWifi(String ssid, String password) {
        SSID = ssid;
        PW = password;

        transitionInternalState(false);

        if (!wifiManager.isWifiEnabled()) {
            wifiDialogHelper.showEnableWifiDialog();
        } else {
            connectToSpecificNetwork();
        }
    }

    private int netId;

    private void connectToSpecificNetwork() {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", SSID);
        wifiConfig.preSharedKey = String.format("\"%s\"", PW);
        wifiConfig.hiddenSSID = false;//true;

        netId = wifiManager.addNetwork(wifiConfig);
//        connecting = true;
        wifiManager.disconnect();
        transitionInternalState(false);
    }
}
