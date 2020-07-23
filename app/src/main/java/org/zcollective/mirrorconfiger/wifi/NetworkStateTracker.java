package org.zcollective.mirrorconfiger.wifi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.internal.disposables.CancellableDisposable;

import static android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI;
import static org.zcollective.mirrorconfiger.wifi.NetworkStateTracker.NetworkState.LOSING;
import static org.zcollective.mirrorconfiger.wifi.NetworkStateTracker.NetworkState.LOST;
import static org.zcollective.mirrorconfiger.wifi.NetworkStateTracker.NetworkState.NONE;
import static org.zcollective.mirrorconfiger.wifi.NetworkStateTracker.NetworkState.ON_BLOCKED_STATUS_CHANGED;
import static org.zcollective.mirrorconfiger.wifi.NetworkStateTracker.NetworkState.AVAILABLE;

public class NetworkStateTracker {

    public static Flowable<NetworkStateObject> trackCurrentNetwork(@NonNull Context context) {
        ConnectivityManager connMgr =
                (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connMgr == null) throw new IllegalStateException();

        return Flowable.create(emitter -> {
            Scheduler.Worker worker = AndroidSchedulers.mainThread().createWorker();
            worker.schedule(() -> emitter.onNext(new NetworkStateObject(NONE, null)));

            NetworkRequest request = new NetworkRequest.Builder()
                                                       .addTransportType(TRANSPORT_WIFI)
                                                       .addCapability(NET_CAPABILITY_NOT_METERED)
                                                       .build();

            ConnectivityManager.NetworkCallback networkCallback =
                    new ConnectivityManager.NetworkCallback() {

                        @Override
                        public void onAvailable(@NonNull Network network) {
                            worker.schedule(() -> emitter.onNext(
                                    new NetworkStateObject(AVAILABLE, network)));
                        }

                        @Override
                        public void onLosing(@NonNull Network network, int maxMsToLive) {
                            worker.schedule(() -> emitter.onNext(new NetworkStateObject(LOSING,
                                    network)));
                        }

                        @Override
                        public void onLost(@NonNull Network network) {
                            worker.schedule(() -> emitter.onNext(new NetworkStateObject(LOST,
                                    network)));
                        }

                        @Override
                        public void onBlockedStatusChanged(@NonNull Network network,
                                                           boolean blocked) {
                            worker.schedule(() -> emitter.onNext(
                                    new NetworkStateObject(ON_BLOCKED_STATUS_CHANGED, network)));
                        }
                    };

            connMgr.requestNetwork(request, networkCallback);

            emitter.setDisposable(new CompositeDisposable(worker,
                    new CancellableDisposable(() -> connMgr.unregisterNetworkCallback(
                            networkCallback))));
        }, BackpressureStrategy.LATEST);
    }

    public enum NetworkState {
//        ERROR,
//        READY,
//        SCANNING,
//        RECEIVED_QR_CODE,
//        REQUESTING_WIFI_ENABLE,
//        CONNECTING_TO_WIFI,
//        STARTING_PORTAL;
//        WIFI_CONNECTED;
        NONE, AVAILABLE, LOSING, LOST, ON_BLOCKED_STATUS_CHANGED,
    }

    public static class NetworkStateObject {
        public final NetworkState state;
        public final Network network;

        NetworkStateObject(NetworkState state, Network network) {
            this.network = network;
            this.state = state;
        }
    }
}
