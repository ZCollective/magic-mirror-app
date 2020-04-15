package org.zcollective.mirrorconfiger.util.mirrordiscovery;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.lang.annotation.Retention;
import java.util.concurrent.Semaphore;

import static java.lang.annotation.RetentionPolicy.CLASS;

public class MirrorDiscoveryHelper extends Fragment {

//    private CountDownLatch cdLatch = new CountDownLatch(1);
//    private

    @IntDef(value = {
            DiscoveryState.NOT_READY,
            DiscoveryState.READY,
            DiscoveryState.STARTED,
            DiscoveryState.STOPPED
    })
    @Retention(CLASS)
    public @interface DiscoveryState {
        int NOT_READY = -1;
        int READY = 0;
        int STARTED = 1;
        int STOPPED = 2;
    }

    int getState() {
        return state;
    }

    public void pauseDiscovery() {
        stopDiscovery();
        state = DiscoveryState.STOPPED;
    }

//    public void setState(@DiscoveryState int newState) {
//        switch (newState) {
//            case DiscoveryState.NOT_READY:
//            default:
//                throw new IllegalStateException("Unexpected value: " + newState);
//            case DiscoveryState.READY:
//                stopDiscovery();
//                state = newState;
//                break;
//            case DiscoveryState.STARTED:
//                startDiscovery();
//                break;
//            case DiscoveryState.STOPPED:
//                break;
//        }
//    }

    public static final String TAG = "AvahiHelperFragment";

    private static final String SERVICE_NAME = "Magic Mirror Configurator";
    private static final String SERVICE_TYPE = "_http._tcp.";

    private final Semaphore resolveSemaphore = new Semaphore(1, true);
    private final Semaphore discoverySemaphore = new Semaphore(1, true);

    private MirrorResolveCallback mCallback;
    private NsdManager.DiscoveryListener discoveryListener;
    private NsdManager.ResolveListener resolveListener;
    private NsdManager nsdManager;

    @DiscoveryState
    private int state = DiscoveryState.NOT_READY;

    public MirrorDiscoveryHelper(@NonNull MirrorResolveCallback callback) {
        mCallback = callback;
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

        nsdManager = (NsdManager) context.getApplicationContext().getSystemService(Context.NSD_SERVICE);

        initializeResolveListener();
        initializeDiscoveryListener();

        state = DiscoveryState.READY;
        startDiscovery();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "MirrorDiscoveryHelper::onResume()");
        startDiscovery();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "MirrorDiscoveryHelper::onPause()");
//        stopDiscovery();
        pauseDiscovery();
    }

    public void startDiscovery() {
        Log.i(TAG, "MirrorDiscoveryHelper::startDiscovery()");

        switch(state) {
            case DiscoveryState.NOT_READY:
//                throw new IllegalStateException("NSD-Manager not initialized!");
                Log.e(TAG, "NSD-Manager not initialized!");
                break;
            case DiscoveryState.STARTED:
//                stopDiscovery();
                break;
            case DiscoveryState.READY:
            case DiscoveryState.STOPPED:
                state = DiscoveryState.STARTED;
                Log.i(TAG, "Start Mirror-Discovery");
                try {
                    discoverySemaphore.acquire();
                    nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
                    if (mCallback != null) mCallback.onDiscoveryStarted();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + state);
        }
    }

    public void stopDiscovery() {
        Log.i(TAG, "MirrorDiscoveryHelper::stopDiscovery()");

        switch(state) {
            case DiscoveryState.NOT_READY:
//                throw new IllegalStateException("NSD-Manager not initialized!");
                Log.e(TAG, "NSD-Manager not initialized!");
                break;
            case DiscoveryState.STARTED:
                Log.i(TAG, "Stop Mirror-Discovery");
                nsdManager.stopServiceDiscovery(discoveryListener);
//                discoverySemaphore.release();
//                state = DiscoveryState.STOPPED;
                state = DiscoveryState.READY;
                if (mCallback != null) mCallback.onDiscoveryStopped();
            case DiscoveryState.READY:
            case DiscoveryState.STOPPED:
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + state);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "MirrorDiscoveryHelper::onDetach()");
        mCallback = null;
        stopDiscovery();
    }

    private void initializeDiscoveryListener() {
        // Instantiate a new DiscoveryListener
        discoveryListener = new NsdManager.DiscoveryListener() {

            // Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
//                Log.d(TAG, "Service discovery started");
                Log.d(TAG, "Discovery started: " + regType);
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found! Do something with it.
                Log.d(TAG, "Service discovery success: { " + service + " }");
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
//                } else if (service.getServiceName().equals(SERVICE_NAME)) {
                    // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
//                    services.add(service);
//                    Log.d(TAG, "Same machine: " + SERVICE_NAME);
//                } else if (service.getServiceName().contains(SERVICE_NAME)) {//"NsdChat")){
                } else if (service.getServiceName().startsWith(SERVICE_NAME)) {//"NsdChat")){
                    Log.d(TAG, "New machine: " + SERVICE_NAME);
//                    if (nsdManager != null) nsdManager.resolveService(service, resolveListener);
                    resolveService(service);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost: " + service);
                if (mCallback != null) mCallback.onMirrorLost(service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
                discoverySemaphore.release();
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                if (nsdManager != null) nsdManager.stopServiceDiscovery(this);
                // TODO: check measures taken
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                if (nsdManager != null) nsdManager.stopServiceDiscovery(this);
            }
        };
    }

    // TODO: Start & Stop discovery based on program flow

    private void initializeResolveListener() {
        resolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed: code=" + errorCode + " device={ " + serviceInfo.toString() + " }");
                resolveSemaphore.release();
                Log.wtf(TAG, "Semaphore: count=" + resolveSemaphore.availablePermits() +
                             " , queue-size=" + resolveSemaphore.getQueueLength());
                // Called when the resolve fails. Use the error code to debug.
                if (mCallback != null) mCallback.onMirrorResolveFailed(serviceInfo, errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
                resolveSemaphore.release();
                Log.wtf(TAG, "Semaphore: count=" + resolveSemaphore.availablePermits() +
                             " , queue-size=" + resolveSemaphore
                        .getQueueLength());
                if (serviceInfo.getHost() == null) Log.wtf(TAG, "Host was null, will not resolve this service");
                else if (mCallback != null) mCallback.onMirrorResolved(serviceInfo);
            }
        };
    }

    private void resolveService(NsdServiceInfo service) {
        Log.i(TAG, "MirrorDiscoveryHelper::resolveService()");
//        AsyncTask.SERIAL_EXECUTOR#
        // TODO: Schedule this differently
        try {
            resolveSemaphore.acquire();
            Log.wtf(TAG, "Semaphore: count=" + resolveSemaphore.availablePermits() +
                         " , queue-size=" + resolveSemaphore
                    .getQueueLength());
            if (nsdManager != null) nsdManager.resolveService(service, resolveListener);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public interface MirrorResolveCallback {
        void onMirrorResolved(@NonNull NsdServiceInfo serviceInfo);
        void onMirrorResolveFailed(@NonNull NsdServiceInfo serviceInfo, int errorCode);
        void onMirrorLost(@NonNull NsdServiceInfo serviceInfo);
        void onDiscoveryStarted();
        void onDiscoveryStopped();
    }
}
