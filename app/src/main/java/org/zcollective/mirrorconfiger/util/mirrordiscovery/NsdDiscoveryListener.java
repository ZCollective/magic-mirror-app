package org.zcollective.mirrorconfiger.util.mirrordiscovery;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.util.Log;

import org.zcollective.mirrorconfiger.util.mirrordiscovery.MirrorDiscoveryInterfaces.MirrorResolveCallback;

import java.util.concurrent.atomic.AtomicBoolean;

public class NsdDiscoveryListener extends Fragment{

    /*
    ----------------------------------------------------------------------------------------------------------------
    * Constant declaration
    ----------------------------------------------------------------------------------------------------------------
    */

    private static final String SERVICE_NAME = "Mirror-";
    private static final String SERVICE_TYPE = "_http._tcp.";
    public static final String TAG = "NsdDiscoveryListener";

    /*
    ----------------------------------------------------------------------------------------------------------------
    * Object field declaration
    ----------------------------------------------------------------------------------------------------------------
    */

    private AtomicBoolean nsdState = new AtomicBoolean(false);
    private AtomicBoolean shouldToggle = new AtomicBoolean(false);
    private Listener discoveryListener;
    private NsdManager nsdManager;
    private MirrorResolveCallback resolveBus;

    /*
    ----------------------------------------------------------------------------------------------------------------
    * Constructor
    ----------------------------------------------------------------------------------------------------------------
    */

    public NsdDiscoveryListener(MirrorResolveCallback resolveBus) {
        this.resolveBus = resolveBus;
    }
    /*
    ----------------------------------------------------------------------------------------------------------------
    * Methods overridden from Fragment!
    ----------------------------------------------------------------------------------------------------------------
    */

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        setRetainInstance(true);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.i(TAG, "onAttach()");
        nsdManager = (NsdManager) context.getApplicationContext().getSystemService(Context.NSD_SERVICE);
        startDiscovery();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");
        startDiscovery();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");
        stopDiscovery();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");
        stopDiscovery();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "onDetach()");
        resolveBus = null;
        stopDiscovery();
    }
    /*
    ----------------------------------------------------------------------------------------------------------------
    * Methods derived from DiscoveryListener
    ----------------------------------------------------------------------------------------------------------------
    */



    /*
    ----------------------------------------------------------------------------------------------------------------
    * Methods as common functions
    ----------------------------------------------------------------------------------------------------------------
    */

    public synchronized void startDiscovery() {
        if(nsdManager != null) {
            if(nsdState.get()){
                Log.i(TAG, "Service has already been started!");
            } else {
                discoveryListener = new Listener();
                nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
                nsdState.set(true);
            }
        } else {
            Log.wtf(TAG, "StartDiscovery was called, but no NSDManager is available!");
        }
    }
    public synchronized void stopDiscovery() {
        if(nsdManager != null) {
            if(nsdState.get()){
                nsdManager.stopServiceDiscovery(discoveryListener);
                nsdState.set(false);
            } else {
                Log.i(TAG, "Service has already been stopped!");
            }
        } else {
            Log.wtf(TAG, "StartDiscovery was called, but no NSDManager is available!");
        }
    }

    public synchronized  void toggleDiscovery() {
        if(nsdManager != null) {
            if(nsdState.get()){
                shouldToggle.set(true);
                nsdManager.stopServiceDiscovery(discoveryListener);
                nsdState.set(false);
            } else {
                Log.i(TAG, "Service has already been stopped! Cannot toggle!");
            }
        } else {
            Log.wtf(TAG, "StartDiscovery was called, but no NSDManager is available!");
        }
    }

    private class Listener implements NsdManager.DiscoveryListener{
        // Called as soon as service discovery begins.
        @Override
        public void onDiscoveryStarted(String regType) {
            Log.d(TAG, "Discovery started: " + regType);
            if (resolveBus != null) {
                resolveBus.onDiscoveryStarted();
            } else {
                Log.wtf(TAG, "NO RESOLVE BUS AVAILABLE");
            }
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            // A service was found! Do something with it.
            Log.d(TAG, "Service discovery success: { " + service + " }");
            Log.d(TAG, "Service name: " + service.getServiceName());
            if (!service.getServiceType().equals(SERVICE_TYPE)) {
                // double-checking for correct service type
                Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
            } else if (service.getServiceName().startsWith(SERVICE_NAME)) {//"NsdChat")){
                Log.d(TAG, "New machine: " + service.getServiceName());
                if(nsdManager != null) {
                    nsdManager.resolveService(service, new ResolveListener());
                } else {
                    Log.wtf(TAG, "NsdService is null, but just gave us the service: " + service.toString());
                }
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo service) {
            // When the network service is no longer available.
            Log.e(TAG, "service lost: " + service);
            if (resolveBus != null) {
                resolveBus.onMirrorLost(service);
            } else {
                Log.wtf(TAG, "NO RESOLVE BUS AVAILABLE");
            }
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.i(TAG, "Discovery stopped: " + serviceType);
            if (resolveBus != null) {
                resolveBus.onDiscoveryStopped();
            } else {
                Log.wtf(TAG, "NO RESOLVE BUS AVAILABLE");
            }

            //If we want to re-start discovery
            if (shouldToggle.get()) {
                shouldToggle.set(false);
                startDiscovery();
            }
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            //If startDiscovery failed, we retry
            if (nsdManager != null) startDiscovery();
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            // If stopping failed, we retry stopping
            if (nsdManager != null) stopDiscovery();
        }
    }

    class ResolveListener implements NsdManager.ResolveListener{
        public static final String TAG = "ResolveListener";
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "Error: " + errorCode + " -- Failed Service Resolve for " + serviceInfo.toString());
            if (errorCode == NsdManager.FAILURE_ALREADY_ACTIVE) {
                Log.i(TAG, "Resolving is already active! We should wait and see what happens. Ususally happens when a device just booted.");
                nsdManager.resolveService(serviceInfo, new ResolveListener());
            } else {
                if (resolveBus != null) {
                    resolveBus.onMirrorResolveFailed(serviceInfo, errorCode);
                } else {
                    Log.wtf(TAG, "NO RESOLVE BUS AVAILABLE");
                }
            }
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.d(TAG, "Service successfully resolved: " + serviceInfo.toString());
            if (resolveBus != null) {
                resolveBus.onMirrorResolved(serviceInfo);
            } else {
                Log.wtf(TAG, "NO RESOLVE BUS AVAILABLE");
            }
        }
    }
}
