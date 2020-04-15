package org.zcollective.mirrorconfiger.util.wifi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class WifiStateTrackerOreo extends WifiStateTracker {

    private static final String TAG = "WifiStateOreo";

    private NetworkCallbackManger ncManager;
    private ConnectivityManager connMgr;

    public WifiStateTrackerOreo(@NonNull WifiStateChangedCallback callback) {
        super(callback);
    }

    @Override
    public void destroy() {
        connMgr.unregisterNetworkCallback(ncManager);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroy();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "MirrorDiscoveryHelper::onCreate()");
        setRetainInstance(true);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.i(TAG, "MirrorDiscoveryHelper::onAttach()");

//        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);

//        if (context instanceof WifiStateChangedCallback) {
//            callback = (WifiStateChangedCallback) context;
//        } else {
//            Log.e(TAG, "Activity needs to implement WifiStateChangedCallback-Interface!");
//        }

//        if (Build.VERSION.SDK_INT >= 26) {
            connMgr = (ConnectivityManager) context.getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);
            initialize();
//        } else {
//            Log.wtf(TAG, "Cannot use this fragment for Wifi State-Tracking, as it is for API-Level>=26!");
//            // TODO: throw exception
//        }
    }

    private void initialize() {
        NetworkRequest.Builder request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

        ncManager = new NetworkCallbackManger();
        connMgr.requestNetwork(request.build(), ncManager);
    }

    private class NetworkCallbackManger extends ConnectivityManager.NetworkCallback {

        Network wifiNetwork;

        @Override
        public void onAvailable(Network network) {
            Log.i(TAG, "NetworkCallback::onAvailable");

            // TODO: for now, we just want to test binding to wifi only!
            connMgr.bindProcessToNetwork(network);

            wifiNetwork = network;
            callback.onAvailable();

//                    // Pinging webserver-host, so routing-table is filled with correct host
//                    try {
////                            InetAddress ping = network.getByName("http://192.168.12.1:8080/");
////                            InetAddress ping = network.getByName("192.168.12.1");
//                        InetAddress ping = network.getByName(BuildConfig.MIRROR_SETUP_IP);
//                        Log.wtf(TAG, "Ping is local address: " + ping.isAnyLocalAddress());
//                        long start = System.currentTimeMillis();
//                        Log.wtf(TAG, "Reachable? " + ping.isReachable(3000));
//                        Log.wtf(TAG, "Ping took: " + (System.currentTimeMillis() - start));
//                        Log.wtf(TAG, "IP: " + Arrays.toString(ping.getAddress()));
//                    } catch (Exception e) {
//                        Log.wtf(TAG, "WTF happened?");
//                        e.printStackTrace();
//                    }
//
//                    Intent intent = new Intent(QrScannerActivity.this, WebConfigActivity.class);
//                    intent.putExtra(WebConfigActivity.EXTRA_WEBPAGE, BuildConfig.MIRROR_SETUP_PAGE);
//
//                    runOnUiThread(() -> {
//                        Log.wtf(TAG, "Starting Webview!");
//                        bar.setVisibility(View.GONE);
//                        startActivity(intent);
//                    });
//                }
        }

        @Override
        public void onLosing(Network network, int maxMsToLive) {
            Log.i(TAG, "NetworkCallback::onLosing");
            callback.onLosing();
        }

        @Override
        public void onLost(Network network) {
            Log.i(TAG, "NetworkCallback::onLost");
            callback.onLost();
            wifiNetwork = null;
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            Log.i(TAG, "NetworkCallback::onCapabilitiesChanged");
            callback.onCapabilitiesChanged();
        }

        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            Log.i(TAG, "NetworkCallback::onLinkPropertiesChanged");
            callback.onLinkPropertiesChanged();
        }
    }
}
