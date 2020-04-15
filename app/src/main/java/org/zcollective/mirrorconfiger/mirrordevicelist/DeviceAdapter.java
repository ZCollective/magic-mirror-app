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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import org.zcollective.mirrorconfiger.R;
import org.zcollective.mirrorconfiger.util.mirrordiscovery.MirrorDiscoveryHelper;
import org.zcollective.mirrorconfiger.webconfig.WebConfigActivity;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceLineViewHolder> implements MirrorDevicesHandler.DevicesChangedListener, MirrorDiscoveryHelper.MirrorResolveCallback {

    // TODO: what to do if url changed!
    // https://stackoverflow.com/questions/36956643/partial-update-of-recyclerview-viewholder/36957892

    private static final String LOG_TAG = "DeviceAdapter";

    private final MirrorDevicesHandler devices;
    private final OnDeviceTransitionCallback callback;

    // Remove and Add can occur at the same time
    private final AtomicInteger size;

    public DeviceAdapter(@NonNull OnDeviceTransitionCallback callback) {
        this.callback = callback;
        this.devices = new MirrorDevicesHandler(this);
        this.size = new AtomicInteger(0);
    }

//    private MirrorDevices devices = new MirrorDevices(this);


//    public AdapterView.OnItemClickListener listener = (parent, view, position, id) -> {
//        MirrorDevice mirror = devices.get(position);
//
//        if (mirror == null) return;
//
//        Intent intent = new Intent(parent.getContext(), WebConfigActivity.class);
//        intent.putExtra(WebConfigActivity.EXTRA_WEBPAGE, getWebServer(mirror));
//        DeviceAdapter.this.callback.startActivity(intent);
//    };

//    @Override
//    public void onBindViewHolder(@NonNull DeviceLineViewHolder holder, int position) {
//        holder.itemView.setOnClickListener(view -> {
//            if (holder.mirrorUrl == null) return;
//
//            Intent intent = new Intent(view.getContext(), WebConfigActivity.class);
//            intent.putExtra(WebConfigActivity.EXTRA_WEBPAGE, holder.mirrorUrl);
//            DeviceAdapter.this.callback.startActivity(intent);
//        });
//    }

    @NonNull
    @Override
    public DeviceLineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.recycler_device_row, parent, false);
        return new DeviceLineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceLineViewHolder holder, int position) {
        MirrorDevice element = devices.get(position);
        if (element != null) holder.updateDevice(element);
    }

    @Override
    public int getItemCount() {
//        return devices.size();
//        return size;
        return size.get();
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

    @Override
    public void onMirrorResolved(@NonNull NsdServiceInfo serviceInfo) {
        Log.i(LOG_TAG, "DeviceAdapter::onMirrorResolved()");

        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
            Log.i(LOG_TAG, "Starting to resolve service: { " + serviceInfo.toString() + " }");
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
        Log.wtf(LOG_TAG, "Mirror resolve failed: error=" + errorCode);

        try {
            devices.ageElement(new MirrorDevice(serviceInfo));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMirrorLost(@NonNull NsdServiceInfo serviceInfo) {
        Log.wtf(LOG_TAG, "Mirror lost");

        try {
            devices.removeElement(new MirrorDevice(serviceInfo));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDiscoveryStarted() {
        Log.i(LOG_TAG, "DeviceAdapter::onDiscoveryStarted()");

    }

    @Override
    public void onDiscoveryStopped() {
        Log.i(LOG_TAG, "DeviceAdapter::onDiscoveryStopped()");

        try {
            devices.ageElements();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void clearAll() {
        try {
            devices.clear();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private static boolean runOnUiThread(@NonNull Runnable r) {
        return new Handler(Looper.getMainLooper()).post(r);
    }

    @Override
    public void itemRangeRemoved(int position, int amount) {
        Log.i(LOG_TAG, "DeviceAdapter::itemRangeRemoved(position=" + position + ", amount=" + amount + ")");
        size.set(0);
        runOnUiThread(() -> notifyItemRangeRemoved(position, amount));
    }

    @Override
    public void itemRemoved(int position) {
        Log.i(LOG_TAG, "DeviceAdapter::itemRemoved(position=" + position + ")");
        size.getAndDecrement();
        runOnUiThread(() -> notifyItemRemoved(position));
    }

    @Override
    public void itemAdded(int position) {
        Log.i(LOG_TAG, "DeviceAdapter::itemAdded(position=" + position + ")");
        size.getAndIncrement();
        runOnUiThread(() -> notifyItemInserted(position));
    }

    static class MirrorDevice {
        final NsdServiceInfo serviceInfo;
        final AtomicBoolean markedForRemoval;

        MirrorDevice(@NonNull NsdServiceInfo info) {
            this.serviceInfo = info;
            this.markedForRemoval = new AtomicBoolean(false);
        }
    }

    class DeviceLineViewHolder extends RecyclerView.ViewHolder {

        private final TextView name;
        private final TextView url;
        private String mirrorUrl;

        DeviceLineViewHolder(@NonNull View view) {
            super(view);
            name = view.findViewById(R.id.rec_device_name);
            url = view.findViewById(R.id.rec_device_url);

            view.setOnClickListener(v -> {
                Log.i(LOG_TAG, "OnClick!");
                if (mirrorUrl != null) {
                    Intent intent = new Intent(itemView.getContext(), WebConfigActivity.class);
                    intent.putExtra(WebConfigActivity.EXTRA_WEBPAGE, mirrorUrl);
                    DeviceAdapter.this.callback.startActivity(intent);
                }
            });
        }

        void updateDevice(@Nullable MirrorDevice device) {
            if (device != null) {
                mirrorUrl = getWebServer(device);
                name.setText(device.serviceInfo.getServiceName());
                url.setText(mirrorUrl);
            }
        }
    }

    private static String getWebServer(@NonNull MirrorDevice device) {
        return "http://" + device.serviceInfo.getHost().getHostAddress() + ":" + device.serviceInfo.getPort();
    }

    public interface OnDeviceTransitionCallback {
        void startActivity(Intent intent);
    }
}
