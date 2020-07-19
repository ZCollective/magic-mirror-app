package org.zcollective.mirrorconfiger.mirrordevicelist;

import android.content.Intent;
import android.net.nsd.NsdServiceInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.zcollective.mirrorconfiger.R;
import org.zcollective.mirrorconfiger.util.mirrordiscovery.MirrorDiscoveryInterfaces;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceLineViewHolder> implements MirrorDevicesHandler.DevicesChangedListener, MirrorDiscoveryInterfaces.MirrorResolveCallback {

    /*
     * ----------------------------------------------------------------------------------------------
     *  Declaring public Interface
     * ----------------------------------------------------------------------------------------------
     */
    public interface OnDeviceTransitionCallback {
        void startActivity(Intent intent);
    }


    /*
     * ----------------------------------------------------------------------------------------------
     *  Declaring constants
     * ----------------------------------------------------------------------------------------------
     */
    private static final String LOG_TAG = "DeviceAdapter";

    private final MirrorDevicesHandler devices;
    private final OnDeviceTransitionCallback callback;

    // Remove and Add can occur at the same time
    private final AtomicInteger size;

    /*
     * ----------------------------------------------------------------------------------------------
     *  Declaring constructor
     * ----------------------------------------------------------------------------------------------
     */
    public DeviceAdapter(@NonNull OnDeviceTransitionCallback callback) {
        this.callback = callback;
        this.devices = new MirrorDevicesHandler(this);
        this.size = new AtomicInteger(0);
    }


    /*
     * ----------------------------------------------------------------------------------------------
     *  Methods derived from RecyclerView.Adapter
     * ----------------------------------------------------------------------------------------------
     */

    @NonNull
    @Override
    public DeviceLineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_device_row, parent, false);
        return new DeviceLineViewHolder(view, callback);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceLineViewHolder holder, int position) {
        MirrorDevice element = devices.get(position);
        if (element != null) holder.updateDevice(element);
    }

    @Override
    public int getItemCount() {
        return size.get();
    }

    /*
    * ----------------------------------------------------------------------------------------------
    *  Methods derived from DevicesChangedListener
    * ----------------------------------------------------------------------------------------------
     */

    @Override
    public void itemRangeRemoved(int position, int amount) {
        Log.i(LOG_TAG, "::itemRangeRemoved(position=" + position + ", amount=" + amount + ")");
        size.set(0);
        runOnUiThread(() -> notifyItemRangeRemoved(position, amount));
    }

    @Override
    public void itemRemoved(int position) {
        Log.i(LOG_TAG, "::itemRemoved(position=" + position + ")");
        size.getAndDecrement();
        runOnUiThread(() -> notifyItemRemoved(position));
    }

    @Override
    public void itemAdded(int position) {
        Log.i(LOG_TAG, "::itemAdded(position=" + position + ")");
        size.getAndIncrement();
        runOnUiThread(() -> notifyItemInserted(position));
    }


    /*
     * ----------------------------------------------------------------------------------------------
     *  Methods derived from DevicesChangedListener
     * ----------------------------------------------------------------------------------------------
     */

    @Override
    public void onMirrorResolved(@NonNull NsdServiceInfo serviceInfo) {
        Log.i(LOG_TAG, "::onMirrorResolved(service=" + serviceInfo.toString() + ")");
        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
            Log.i(LOG_TAG, "Trying to ping Host " + serviceInfo.getHost() + ":" + serviceInfo.getPort() + " for 5 seconds");
            boolean open = isPortOpen(serviceInfo.getHost().getHostAddress(), serviceInfo.getPort());
            Log.i(LOG_TAG, "Port is " + (open ? "" : "not ") + "open for service: { " + serviceInfo.toString() + " }");

            // TODO: if wifi was disabled all elements should immediately be inactive -> they will be regarded as "lost services", so nvmd

            try {
                MirrorDevice device = new MirrorDevice(serviceInfo);
                if (open) {
                    devices.addElement(device);
                } else {
                    devices.ageElement(device);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onMirrorResolveFailed(@NonNull NsdServiceInfo serviceInfo, int errorCode) {
        Log.wtf(LOG_TAG, "Mirror resolve failed: error=" + errorCode + " Service: " + serviceInfo.toString());
        try {
            devices.ageElement(new MirrorDevice(serviceInfo));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMirrorLost(@NonNull NsdServiceInfo serviceInfo) {
        Log.i(LOG_TAG, "Mirror lost: " + serviceInfo.toString());
        try {
            devices.removeElement(new MirrorDevice(serviceInfo));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDiscoveryStarted() {
        Log.i(LOG_TAG, "DeviceAdapter::onDiscoveryStarted()");
        // Not used right now. Might be useful later for loading icons maybe?
    }

    @Override
    public void onDiscoveryStopped() {
        Log.i(LOG_TAG, "::onDiscoveryStopped()");
        try {
            devices.ageElements();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    /*
     * ----------------------------------------------------------------------------------------------
     *  Common methods used multiple times
     * ----------------------------------------------------------------------------------------------
     */
    private static boolean runOnUiThread(@NonNull Runnable r) {
        return new Handler(Looper.getMainLooper()).post(r);
    }

    private static boolean isPortOpen(final String ip, final int port) {
        Log.i(LOG_TAG, "Trying to connect to server: ip=" + ip + " port=" + port);

        boolean reachable = false;

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 5000);
            reachable = true;
        } catch(ConnectException | SocketTimeoutException e) {
            Log.i(LOG_TAG, "Failed connecting to server: " + e.getLocalizedMessage());
        } catch (Exception ex) {
            Log.e(LOG_TAG, "Error connecting to server: " + ex.getLocalizedMessage());
        }

        return reachable;
    }

    public void clearAll() {
        try {
            devices.clear();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
