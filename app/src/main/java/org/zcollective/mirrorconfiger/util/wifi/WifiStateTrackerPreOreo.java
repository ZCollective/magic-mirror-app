package org.zcollective.mirrorconfiger.util.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public class WifiStateTrackerPreOreo extends WifiStateTracker {

    private static final String TAG = "WifiStatePreOreo";

    private BroadcastReceiverRegistration registration;

    public WifiStateTrackerPreOreo(@NonNull WifiStateTrackerOreo.WifiStateChangedCallback callback) {
        super(callback);
    }

    @Override
    public void destroy() {
        if (registration != null) registration.unregisterReceiver(receiver);
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

    public interface BroadcastReceiverRegistration {
        Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter);
        void unregisterReceiver(BroadcastReceiver receiver);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.i(TAG, "MirrorDiscoveryHelper::onAttach()");

        if (context instanceof BroadcastReceiverRegistration) {
            registration = ((BroadcastReceiverRegistration) context);
            registration.registerReceiver(receiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        } else {
            Log.e(TAG, "Needs to be of BroadcastReceiverRegistration!");
            // TODO: throw exception
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(Objects.equals(action, WifiManager.NETWORK_STATE_CHANGED_ACTION)){
                AsyncTask.SERIAL_EXECUTOR.execute(() -> {

                    NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

                    switch (info.getDetailedState()) {
                        case IDLE:
                        case CAPTIVE_PORTAL_CHECK:
                        case VERIFYING_POOR_LINK:
                        case BLOCKED:
                        case FAILED:
                        case SUSPENDED:
                        case OBTAINING_IPADDR:
                        case AUTHENTICATING:
                        case CONNECTING:
                        case SCANNING:
                            Log.d(TAG, "Not what we were looking for");
                            break;
                        case CONNECTED:
                            callback.onAvailable();
                            break;
                        case DISCONNECTING:
                            callback.onLosing();
                            break;
                        case DISCONNECTED:
                            callback.onLost();
                            break;
                    }
                });
            }
        }
    };
}
