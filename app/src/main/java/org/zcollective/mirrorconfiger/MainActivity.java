package org.zcollective.mirrorconfiger;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.nsd.NsdManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.zcollective.mirrorconfiger.mirrordevicelist.DeviceAdapter;
import org.zcollective.mirrorconfiger.qrscanner.QrScannerActivity;
import org.zcollective.mirrorconfiger.util.mirrordiscovery.NsdDiscoveryListener;
import org.zcollective.mirrorconfiger.util.wifi.WifiStateTrackerPreOreo;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements DeviceAdapter.OnDeviceTransitionCallback, WifiStateTrackerPreOreo.BroadcastReceiverRegistration {

    private static final String LOG_TAG = "DashboardActivity";

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    //private MirrorDiscoveryListener headlessDiscoveryHelper;
//    private WifiStateTracker headlessWifiStateTracker;
    private DeviceAdapter deviceAdapter;
    private NsdDiscoveryListener discoveryListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> startActivity(new Intent(MainActivity.this,
                QrScannerActivity.class)));

        // Recyclerview displaying found mirror-devices
        initializeRecyclerView();

        // NSD-Discovery Helper to find mirror-devices
        initializeMirrorDiscoveryHelper();

        // Helper for tracking changes on Wifi
        initializeWifiStateTracker();

        // Scheduling NSD-Discovery to change state every 5 seconds
        scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                discoveryListener.toggleDiscovery();
            }
        }, 10, 10, TimeUnit.SECONDS);

        // TODO: we should scan for 5 seconds every 10 seconds periodically
        // TODO: check wifi state!
        // TODO: check if device is reachable before connecting to it! Show error-message if not
        // TODO: destroy webview correctly on error
    }

    private ConnectivityManager.NetworkCallback ncManager;
    private ConnectivityManager connMgr;
    private Network wifiNetwork;

    private NetworkRequest buildNetworkRequest() {
        NetworkRequest.Builder request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        request.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET);
//        request.removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        return request.build();
    }

    private void initializeWifiStateTracker() {
        connMgr = (ConnectivityManager) getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);
        NetworkRequest.Builder request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

        ncManager = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Log.i(LOG_TAG, "NetworkCallback::onAvailable");

                // TODO: for now, we just want to test binding to wifi only!
                connMgr.bindProcessToNetwork(network);

                wifiNetwork = network;

                //Re-start discovery after network loss
                discoveryListener.startDiscovery();
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
            public void onLosing(@NonNull Network network, int maxMsToLive) {
                Log.i(LOG_TAG, "NetworkCallback::onLosing");
            }

            @Override
            public void onLost(@NonNull Network network) {
                Log.i(LOG_TAG, "NetworkCallback::onLost");
                wifiNetwork = null;
                discoveryListener.stopDiscovery();
                deviceAdapter.clearAll();
            }
        };
        connMgr.requestNetwork(request.build(), ncManager);
    }

//    private void initializeWifiStateTracker() {
//        headlessWifiStateTracker =
//                (WifiStateTracker) getSupportFragmentManager().findFragmentByTag(WifiStateTracker.TAG);
//
//        if (headlessWifiStateTracker == null) {
//            WifiStateTrackerOreo.WifiStateChangedCallback wifiChangedCallback =
//                    new SimpleWifiStateChangedCallback() {
//
//                        @Override
//                        public void onAvailable() {
//                            headlessDiscoveryHelper.startDiscovery();
//                        }
//
//                        @Override
//                        public void onLost() {
//                            headlessDiscoveryHelper.pauseDiscovery();
//                            deviceAdapter.clearAll();
//                        }
//                    };
//
////            if (Build.VERSION.SDK_INT >= 26) {
//            headlessWifiStateTracker = new WifiStateTrackerOreo(wifiChangedCallback);
////            } else {
////                headlessWifiStateTracker = new WifiStateTrackerPreOreo(wifiChangedCallback);
////            }
//
//            getSupportFragmentManager().beginTransaction()
//                                       .add(headlessWifiStateTracker, WifiStateTracker.TAG)
//                                       .commit();
//        }
//    }


    private void initializeMirrorDiscoveryHelper() {
        discoveryListener = (NsdDiscoveryListener) getSupportFragmentManager().findFragmentByTag(NsdDiscoveryListener.TAG);

        if (discoveryListener == null) {
            discoveryListener = new NsdDiscoveryListener(deviceAdapter);
            getSupportFragmentManager().beginTransaction()
                                       .add(discoveryListener, NsdDiscoveryListener.TAG)
                                       .commit();
        }
    }

    private void initializeRecyclerView() {
        deviceAdapter = new DeviceAdapter(this);
//        LayoutAnimationController animation =
//                AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_from_bottom);
        RecyclerView rView = findViewById(R.id.devices_list_view);
        rView.setHasFixedSize(false);
        rView.setLayoutManager(new LinearLayoutManager(this));
        rView.setAdapter(deviceAdapter);
//        rView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
//        rView.setLayoutAnimation(animation);
        // TODO: custom animation
    }

    @Override
    protected void onDestroy() {
        Log.i(LOG_TAG, "MainActivity::onDestroy()");
        // Making sure we clean references on destroy
        discoveryListener.stopDiscovery();
        discoveryListener = null;
//        headlessWifiStateTracker.destroy();
//        headlessWifiStateTracker = null;
        deviceAdapter = null;
//        connMgr.unregisterNetworkCallback(ncManager);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        Log.i(LOG_TAG, "MainActivity::onPause()");
        discoveryListener.stopDiscovery();
        connMgr.unregisterNetworkCallback(ncManager);
        super.onPause();
        // TODO: pause scheduler!
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(LOG_TAG, "MainActivity::onResume()");
        discoveryListener.startDiscovery();
        connMgr.requestNetwork(buildNetworkRequest(), ncManager);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    @Deprecated
    public void removeNetwork(View view) {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        boolean found = false;
        for (WifiConfiguration network : wm.getConfiguredNetworks()) {
            if (network.SSID.contentEquals("\"Mirror\"")) {
                found = true;
                wm.disconnect();
                boolean removed = wm.removeNetwork(network.networkId);
                Log.wtf("Main", "Removed Network " + network.SSID);
                Snackbar.make(view,
                        "Removed Network " + network.SSID + ": " + removed, Snackbar.LENGTH_LONG)
                        .show();
            }
        }

        if (!found) Snackbar.make(view, "Network unknown", Snackbar.LENGTH_LONG).show();
    }
}
