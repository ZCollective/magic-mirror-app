package org.zcollective.mirrorconfiger.mirrordevicelist;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import org.zcollective.mirrorconfiger.R;
import org.zcollective.mirrorconfiger.webconfig.WebConfigActivity;
import org.zcollective.mirrorconfiger.mirrordevicelist.DeviceAdapter.OnDeviceTransitionCallback;

class DeviceLineViewHolder extends RecyclerView.ViewHolder {

    private static final String LOG_TAG = "DeviceLineViewHolder";
    private final TextView name;
    private final TextView url;
    private String mirrorUrl;

    DeviceLineViewHolder(@NonNull View view, OnDeviceTransitionCallback onClick) {
        super(view);
        name = view.findViewById(R.id.rec_device_name);
        url = view.findViewById(R.id.rec_device_url);

        view.setOnClickListener(v -> {
            Log.i(LOG_TAG, "OnClick!");
            if (mirrorUrl != null) {
                Intent intent = new Intent(itemView.getContext(), WebConfigActivity.class);
                intent.putExtra(WebConfigActivity.EXTRA_WEBPAGE, mirrorUrl);

                onClick.startActivity(intent);
            }
        });
    }

    void updateDevice(@Nullable MirrorDevice device) {
        if (device != null) {
            mirrorUrl = getWebServer(device);
            name.setText(device.serviceInfo.getServiceName());
            Log.d(LOG_TAG, "Hostinfo: " + device.serviceInfo.getHost().toString());
            url.setText(device.visibleName + " (" + device.serviceInfo.getHost().getHostAddress() + ")");
        }
    }

    private static String getWebServer(@NonNull MirrorDevice device) {
        return "http://" + device.serviceInfo.getHost().getHostAddress() + ":" + device.serviceInfo.getPort();
    }
}