package org.zcollective.mirrorconfiger;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.TrafficStats;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.druk.rx2dnssd.BonjourService;
import com.github.druk.rx2dnssd.Rx2Dnssd;
import com.github.druk.rx2dnssd.Rx2DnssdBindable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.zcollective.mirrorconfiger.mirrordevicelist.DeviceAdapter;
import org.zcollective.mirrorconfiger.qrscanner.QrScannerActivity;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements DeviceAdapter.OnDeviceTransitionCallback {

    private static final String LOG_TAG = "DashboardActivity";
    private static final String RX_TAG = "RxNSD_Chain";

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
//    private final List<Disposable> devicePingList = new ArrayList<>();

    private ConnectivityManager.NetworkCallback ncManager;
    private ConnectivityManager connMgr;
    private DeviceAdapter deviceAdapter;
    private Disposable browseDisposable;
    private Rx2Dnssd rxDNS_SD;
    private Snackbar networkSnackbar;
//    private Network wifiNetwork;
//    private boolean started = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> startActivity(new Intent(MainActivity.this,
                QrScannerActivity.class)
        ));

        // Recyclerview displaying found mirror-devices
        initializeRecyclerView();

        // NSD-Discovery Helper to find mirror-devices
        rxDNS_SD = new Rx2DnssdBindable(this);

        // Helper for tracking changes on Wifi
        initializeWifiStateTracker();

        // TODO: check wifi state!
        // TODO: check if device is reachable before connecting to it! Show error-message if not
    }

    private void stopRxBrowser() {
        if (browseDisposable != null) {
//            for (Disposable d : devicePingList) {
//                d.dispose();
//            }
//            devicePingList.clear();
            compositeDisposable.clear();
            browseDisposable.dispose();
            browseDisposable = null;
        }
    }

    private void startRxBrowser() {
        browseDisposable = rxDNS_SD
                .browse("_http._tcp", "local.")
                .subscribeOn(Schedulers.io())
                .filter(service -> service.getServiceName().startsWith("Mirror-")) // TODO: this might need refining!
                .compose(rxDNS_SD.resolve())
                .compose(rxDNS_SD.queryIPRecords())
                .filter(service -> {
                    if (!service.isLost()) {
                        return service.getInet4Address() != null;
                    }
                    return true;
                })
                .doOnSubscribe(___ -> Timber.tag(RX_TAG).d("Start chain"))
                .distinctUntilChanged((prev, next) -> {
                    // TODO: does not really work correctly - needs fixing!
                    if (!next.isLost() && prev.isLost() == next.isLost()) {
                        List<InetAddress> prevAddresses = prev.getInetAddresses();
                        List<InetAddress> nextAddresses = next.getInetAddresses();

                        for (InetAddress address : nextAddresses) {
                            if (!prevAddresses.contains(address)) {
                                return true;
                            }
                        }

                        // TODO: figure out if this is the culprit!
                        Map<String, String> prevTxt = prev.getTxtRecords();
                        Map<String, String> nextTxt = next.getTxtRecords();

                        if (prevTxt.equals(nextTxt)) {
                            return next.equals(prev);
                        }
                    }

                    return false;
                }).observeOn(AndroidSchedulers.mainThread())
                .doOnCancel(() -> {
                    Timber.tag(RX_TAG).d(String.format("Cancel chain. Main Thread? %B",
                            Looper.myLooper() == Looper.getMainLooper()));
                    // Wrapping just to be sure, otherwise we might crash!
                    runOnUiThread(() -> {
                        if (deviceAdapter != null) {
                            deviceAdapter.clearAll();
                        }
                    });
                }).subscribe(this::handleServiceEvent,
                        throwable -> Timber.tag(RX_TAG).e(throwable, "error"));
    }

    private void handleServiceEvent(@NonNull BonjourService service) {
        // TODO: on item changed still missing!

        Timber.tag(RX_TAG).d(String.format("lost=%b, { %s }, Main Thread? %B",
                service.isLost(), service.toString(), Looper.myLooper() == Looper.getMainLooper()));

        if (service.isLost()) {
            deviceAdapter.remove(service);
        } else if (deviceAdapter.add(service)) {
//            devicePingList.add(makeDevicePing(service));
            compositeDisposable.add(makeDevicePing(service));
        }
    }

    @NonNull
    private Disposable makeDevicePing(@NonNull BonjourService service) {
        String D_TAG = service.getServiceName();
        return Observable
                .interval(5, 20, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.trampoline())
                .takeUntil(___ -> browseDisposable == null || !deviceAdapter.contains(service))
                .map(___ -> {
                    Timber.tag(D_TAG).d(String.format("Pinging service: ip=%S, { %s }, Main Thread? %B",
                            service.getInet4Address(), service,
                            Looper.myLooper() == Looper.getMainLooper()));
                    return pingService(service);
                })
                .doOnComplete(() -> Timber.tag(D_TAG).d("Completed"))
                .doOnTerminate(() -> Timber.tag(D_TAG).d("terminated"))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pingable -> {
                    Timber.tag(D_TAG).d(String.format("Evaluating ping result. Main Thread? %B",
                            Looper.myLooper() == Looper.getMainLooper()));
                    if (!pingable) {
                        // Wrapping just to be sure, otherwise we might crash!
                        runOnUiThread(() -> deviceAdapter.remove(service));
                    }
                }, throwable -> Timber.tag(D_TAG).e(throwable, "error"));
    }

    private boolean pingService(@NonNull BonjourService service) {
//        String host;
        if (service.getInet4Address() == null) return false;
//        host = service.getInet4Address().getHostAddress();
//        if (wifiNetwork != null) {
        try {
            TrafficStats.setThreadStatsTag(42);//.setThreadSocketTag(42);
//                InetAddress ping = wifiNetwork.getByName(host + ":" + service.getPort());
//                return ping.isReachable(5000);
            return service.getInet4Address().isReachable(5000);
        } catch (Exception e) {
            Timber.tag(LOG_TAG).w(e, "Pinging failed, refer to stacktrace");
        }
//        }

        return false;
    }

    private NetworkRequest buildNetworkRequest() {
        NetworkRequest.Builder request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        request.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET);
        return request.build();
    }

    private void initializeWifiStateTracker() {
        connMgr = (ConnectivityManager) getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);
        NetworkRequest.Builder request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

        ncManager = new ConnectivityManager.NetworkCallback() {

            @Override
            public void onAvailable(@NonNull Network network) {
                Timber.tag(LOG_TAG).i("NetworkCallback::onAvailable");

                // TODO: for now, we just want to test binding to wifi only!
                connMgr.bindProcessToNetwork(network);

//                wifiNetwork = network;

                if (browseDisposable == null) startRxBrowser();

                //Re-start discovery after network loss
//                discoveryListener.startDiscovery();
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
                Timber.tag(LOG_TAG).i("NetworkCallback::onLosing");
            }

            @Override
            public void onLost(@NonNull Network network) {
                Timber.tag(LOG_TAG).i("NetworkCallback::onLost");
//                wifiNetwork = null;
                stopRxBrowser();
            }
        };
        connMgr.requestNetwork(request.build(), ncManager);
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
        Timber.tag(LOG_TAG).i("Running MainActivity::onDestroy()");

        compositeDisposable.dispose();

        if (deviceAdapter != null) {
            deviceAdapter.clearAll();
            deviceAdapter = null;
        }

        ncManager = null;
        rxDNS_SD = null;
        connMgr = null;

        if (browseDisposable != null) {
            browseDisposable.dispose();
            browseDisposable = null;
        }

        Timber.tag(LOG_TAG).i("Finished custom part of MainActivity::onDestroy()");

        super.onDestroy();
    }

    @Override
    protected void onPause() {
        // TODO: pause scheduler!
        Timber.tag(LOG_TAG).i("MainActivity::onPause()");
        if (connMgr != null && ncManager != null) {
            connMgr.unregisterNetworkCallback(ncManager);
        }
        stopRxBrowser();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Timber.tag(LOG_TAG).i("MainActivity::onResume()");
        connMgr.requestNetwork(buildNetworkRequest(), ncManager);
        new Handler().postDelayed(() -> {
            // Start service-discovery delayed, if not yet started for some reason
            if (browseDisposable == null) startRxBrowser();
        }, 1000);
    }

    @Deprecated
    public void removeNetwork(View view) {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            if (networkSnackbar != null) networkSnackbar.dismiss();
            networkSnackbar = Snackbar.make(view, "Manually enable Location-Services for this feature!", Snackbar.LENGTH_LONG);
            networkSnackbar.show();
            return;
        }

        boolean found = false;

        for (WifiConfiguration network : wm.getConfiguredNetworks()) {
            if (network.SSID.contentEquals("\"Mirror\"")) {
                found = true;
                wm.disconnect();
                boolean removed = wm.removeNetwork(network.networkId);
                Timber.tag("Main").wtf("Removed Network %s", network.SSID);
                Snackbar.make(view,
                        "Removed Network " + network.SSID + ": " + removed, Snackbar.LENGTH_LONG)
                        .show();
            }
        }

        if (!found) {
            if (networkSnackbar != null) networkSnackbar.dismiss();
            networkSnackbar = Snackbar.make(view, "Network unknown", Snackbar.LENGTH_LONG);
            networkSnackbar.show();
        }
    }
}
