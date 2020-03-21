package org.zcollective.mirrorconfiger.qrscanner;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.RouteInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.zcollective.mirrorconfiger.R;
import org.zcollective.mirrorconfiger.webconfig.WebConfigActivity;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class QrScannerActivity extends AppCompatActivity {

    static class WifiBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.wtf(TAG, "Received a Broadcast!");
        }
    }

    private static final String TAG = "QrScanner";

    // Permissions start at 0
    private static final int PERMISSION_CAMERA = 0;

    // Requests start at 1000
    private static final int REQUEST_ENABLE_WIFI = 1000;

    private CodeScanner mCodeScanner;

    private volatile boolean connecting = false;

    private ProgressBar bar;

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

//        WifiBroadcastReceiver br = new WifiBroadcastReceiver();
//        IntentFilter filter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);//ConnectivityManager.CONNECTIVITY_ACTION);
//        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
//        registerReceiver(br, filter);

        if (Build.VERSION.SDK_INT >= 26) {
            ConnectivityManager connMgr = (ConnectivityManager) getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);

            NetworkRequest.Builder request = new NetworkRequest.Builder();
            request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
//            request.addCapability(NET_CAPABILITY_NOT_SUSPENDED);

            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

            connMgr.requestNetwork(request.build(), new ConnectivityManager.NetworkCallback() {

                @Override
                public void onAvailable(Network network) {
                    Log.wtf(TAG, "NetworkCallback::onAvailable");
                    Log.d(TAG, "SSID: " + wifiManager.getConnectionInfo().getSSID());

//                    if (connecting) {
//                    if (Objects.equals(wifiManager.getConnectionInfo().getSSID(), "<unknown ssid>") || connecting) {
                    if (Objects.equals(wifiManager.getConnectionInfo().getSSID(), "\"Mirror\"") || connecting) {
//                        NetworkInfo networkInfo = connMgr.getNetworkInfo(network);
//                    if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
//                                                        isWifiConn |= networkInfo.isConnected();

                        runOnUiThread(() -> bar.setVisibility(View.VISIBLE));

                        connMgr.bindProcessToNetwork(network);


                        // Pinging webserver-host, so routing-table is filled with correct host
                        try {
//                            InetAddress ping = network.getByName("http://192.168.12.1:8080/");
                            InetAddress ping = network.getByName("192.168.12.1");
//                            Log.wtf(TAG, "Ping is local address: " + ping.isAnyLocalAddress());
                            long start = System.currentTimeMillis();
                            Log.wtf(TAG, "Reachable? " + ping.isReachable(3000));
                            Log.wtf(TAG, "Ping took: " + (System.currentTimeMillis() - start));
                            Log.wtf(TAG, "IP: " + Arrays.toString(ping.getAddress()));
                        } catch (Exception e) {
                            Log.wtf(TAG, "WTF happened?");
                            e.printStackTrace();
                        }
//                    }

//                        runOnUiThread(() -> Snackbar.make(QrScannerActivity.this.findViewById(R.id.content), "Starting Webview!", Snackbar.LENGTH_SHORT).show());

                        Intent intent = new Intent(QrScannerActivity.this, WebConfigActivity.class);
//                        intent.putExtra("SSID", "Mirror");
//                        intent.putExtra("PASS", "");
//                        intent.putExtra("HIDDEN", true);
//                                                    startActivity(new Intent(QrScannerActivity.this, WebConfigActivity.class));
                        runOnUiThread(() -> {
                            Log.wtf(TAG, "Starting Webview!");
                            bar.setVisibility(View.GONE);
                            startActivity(intent);
                        });
                    }
                }

                @Override
                public void onLosing(Network network, int maxMsToLive) {
                    Log.wtf(TAG, "NetworkCallback::onLosing");
                    Log.wtf(TAG, "Will be online for another " + maxMsToLive +" ms");
                }

                @Override
                public void onLost(Network network) {
                    Log.wtf(TAG, "NetworkCallback::onLost");
                }

                @Override
                public void onUnavailable() {
                    Log.wtf(TAG, "NetworkCallback::onUnavailable");
                }

                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                    Log.wtf(TAG, "NetworkCallback::onCapabilitiesChanged");

                    Log.wtf(TAG, "Is Wifi? " + networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) Log.wtf(TAG, "WIFI not suspended? " + networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED));
                    Log.wtf(TAG, "Up: " + networkCapabilities.getLinkUpstreamBandwidthKbps() / 1024 + " Mbps");
                    Log.wtf(TAG, "Down: " + networkCapabilities.getLinkDownstreamBandwidthKbps() / 1024 + " Mbps");
//                    if (connecting) {
////                        boolean wasConnecting = connecting;
////                        connecting = wasConnecting && !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
//                        connecting = !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
//                    }
                }

                @Override
                public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                    Log.wtf(TAG, "NetworkCallback::onLinkPropertiesChanged");
                    Log.wtf(TAG, "Domains: " + linkProperties.getDomains());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) Log.wtf(TAG, "Private DNS? " + linkProperties.isPrivateDnsActive());
                    Log.wtf(TAG, "Routes:\n" + linkProperties.getRoutes().stream().map(RouteInfo::toString).collect(Collectors.joining("\n")));
                }
            });//, 10000);
        }


        if(checkPermission()) {
            scanningJob();
        } else {
            requestPermission();
        }
    }

    private void scanningJob() {
        CodeScannerView scannerView = findViewById(R.id.scanner_view);

        mCodeScanner = new CodeScanner(this, scannerView);
        mCodeScanner.setDecodeCallback(result -> {
//            runOnUiThread(() -> {
//                Toast.makeText(QrScannerActivity.this, result.getText(), Toast.LENGTH_SHORT).show();
//            });

//            if (result.getText().matches("^WIFI:T:WPA;S:Mirror;P:((?=.*[a-z])(?=.*[A-Z])(?=.*\\d)" +
//                                         "(?=.*[#$^+=!*()@%&]).{8,64};)(H:true)?;{1,2}$")) {
//                if (result.getText().matches(
//                        "^(\")?WIFI:T:WPA;S:Mirror;P:((\\p{Lower}|\\p{Upper}|\\d|#|\\$|\\^|\\+|=|!|\\*|\\(|\\)|@|%|&)|(\\\\(\\|;|,|:|\"))){8,64};(H:true)?;{1,2}(\")?$")) {
//                        "^WIFI:T:WPA;S:Mirror;P:(\p{Lower}|){8,64};(H:true)?;{1,2}$")) {
                    runOnUiThread(() -> {
                                Toast.makeText(QrScannerActivity.this, result.getText(), Toast.LENGTH_SHORT).show();
                                new AlertDialog
                                        .Builder(QrScannerActivity.this)
                                        .setMessage("Proceed to connect with Mirror?")
                                        .setPositiveButton("OK", (dialog, which) -> {
                                               String[] wifiResult = result.getText().split(";");
                                               String password = "";
                                               for (String asdf : wifiResult) {
                                                   if (asdf.startsWith("P:")) {
                                                       password = asdf.substring(2);
                                                   }
                                               }
//                                               final String pass = password;
                                               connectToWifi("Mirror", password);

//                                            AsyncTask.SERIAL_EXECUTOR.execute(() -> {
////                                                try {
////                                                    Thread.sleep(10000);
////                                                } catch (InterruptedException e) {
////                                                    e.printStackTrace();
////                                                }
//
////                                                ConnectivityManager.TYPE_WIFI
//
////                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                                                ConnectivityManager connMgr = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
////                                                    NetworkCapabilities.TRANSPORT_WIFI
//                                                for (Network network : connMgr.getAllNetworks()) {
//                                                    NetworkInfo networkInfo = connMgr.getNetworkInfo(network);
//                                                    if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
////                                                        isWifiConn |= networkInfo.isConnected();
//                                                        connMgr.bindProcessToNetwork(network);
//                                                    }
////                                                    if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
////                                                        isMobileConn |= networkInfo.isConnected();
////                                                    }
//                                                }
////                                                } else {
////                                                    ConnectivityManager.setProcessDefaultNetwork(ConnectivityManager.TYPE_WIFI);
////                                                }
//
//
//                                                runOnUiThread(() -> {
//                                                    Intent intent = new Intent(QrScannerActivity.this, WebConfigActivity.class);
//                                                    intent.putExtra("SSID", "Mirror");
//                                                    intent.putExtra("PASS", pass);
//                                                    intent.putExtra("HIDDEN", true);
////                                                    startActivity(new Intent(QrScannerActivity.this, WebConfigActivity.class));
//                                                    startActivity(intent);
//                                                });
//                                            });
                                       })
                                       .setNegativeButton("Cancel", (dialog, which) -> {
                                           onSupportNavigateUp();
                                           dialog.dismiss();
                                       })
                                       .create()
                                       .show();
                            });
//            }
        });

        scannerView.setOnClickListener(view -> mCodeScanner.startPreview());
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
               PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();

                // main logic
                scanningJob();
            } else {
                Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkPermission()) {
                    showMessageOKCancel("You need to allow access permissions", (dialog, which) -> requestPermission());
                }
            }
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(QrScannerActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCodeScanner != null) mCodeScanner.startPreview();
    }

    @Override
    protected void onPause() {
        if (mCodeScanner != null) mCodeScanner.releaseResources();
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
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wifi.isWifiEnabled() || wifi.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
                connectToSpecificNetwork();
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

    private void showWifiDisabledDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage(getString(R.string.wifi_disabled))
                .setPositiveButton(getString(R.string.enable_wifi), (dialog, which) -> {
                    // open settings screen
                    Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    startActivityForResult(intent, REQUEST_ENABLE_WIFI);
                })
                .setNegativeButton(getString(R.string.exit_app), (dialog, which) -> finish())
                .show();
    }

    private String SSID;
    private String PW;

    private void connectToWifi(String ssid, String password) {

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        SSID = ssid;
        PW = password;

        if (!wifiManager.isWifiEnabled()) {
//            startActivityForResult(new Intent(Settings.ACTION_WIRELESS_SETTINGS), 123456);
//            startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), 123456);
            showWifiDisabledDialog();
        } else {
            connectToSpecificNetwork();
        }
    }

    private void connectToSpecificNetwork() {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", SSID);
        wifiConfig.preSharedKey = String.format("\"%s\"", PW);
        wifiConfig.hiddenSSID = false;//true;

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        int netId = wifiManager.addNetwork(wifiConfig);
        wifiManager.disconnect();
        connecting = true;
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
    }
}
