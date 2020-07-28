package org.zcollective.mirrorconfiger.mirrordevicelist;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.druk.rx2dnssd.BonjourService;

import org.zcollective.mirrorconfiger.R;
import org.zcollective.mirrorconfiger.webconfig.WebConfigActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceLineViewHolder> {

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

    private final OnDeviceTransitionCallback callback;
    private final List<BonjourService> serviceList = new ArrayList<>();
    private final Lock lock = new ReentrantLock(true);

    /*
     * ----------------------------------------------------------------------------------------------
     *  Declaring constructor
     * ----------------------------------------------------------------------------------------------
     */
    public DeviceAdapter(@NonNull OnDeviceTransitionCallback callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public DeviceLineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_device_row, parent, false);
        return new DeviceLineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceLineViewHolder holder, int position) {
        lock.lock();
        BonjourService element = serviceList.get(position);
        if (element != null) holder.updateDevice(element);
        lock.unlock();
    }

    @Override
    public int getItemCount() {
        lock.lock();
        int size = serviceList.size();
        lock.unlock();
        return size;
    }

    public boolean contains(BonjourService service) {
        if (service == null) return false;

        lock.lock();
        boolean contained = serviceList.contains(service);
        lock.unlock();
        return contained;
    }

    public boolean add(BonjourService service) {
        boolean wasAdded = false;
        lock.lock();
        if (!serviceList.contains(service)) {
            int position = serviceList.size();
            serviceList.add(service);
            notifyItemInserted(position);
            wasAdded = true;
        }
        lock.unlock();
        return wasAdded;
    }

    public void remove(BonjourService service) {
        int index;
        lock.lock();
        if ((index = serviceList.indexOf(service)) >= 0) {
            serviceList.remove(index);
            notifyItemRemoved(index);
        }
        lock.unlock();
    }

    public void clearAll() {
        lock.lock();
        serviceList.clear();
        notifyDataSetChanged();
        lock.unlock();
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
                    intent.putExtra(WebConfigActivity.EXTRA_WEB_PAGE, mirrorUrl);
                    DeviceAdapter.this.callback.startActivity(intent);
                }
            });
        }

        void updateDevice(@NonNull BonjourService service) {
            name.setText(service.getServiceName());
            String visibleName = "";
            for(Map.Entry<String, String> entry : service.getTxtRecords().entrySet()){
                if (entry.getKey().equals("devicename")){
                    visibleName = entry.getValue();
                    break;
                }
            }
            // TODO: service.getInetAddress() == null?
            //mirrorUrl = String.format("http://%s:%d", service.getInet4Address().getHostAddress(), service.getPort());
            String ip = "";
            if(service.getInet4Address() != null && service.getInet4Address().getHostAddress() != null) {
                ip = service.getInet4Address().getHostAddress();
            }
            name.setText(service.getServiceName());
            url.setText(visibleName + " - " + ip);
        }
    }
}
