package org.zcollective.mirrorconfiger.mirrordevicelist;

import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

class MirrorDevice {
    final String LOG_TAG = "MirrorDevice";
    final NsdServiceInfo serviceInfo;
    final AtomicBoolean markedForRemoval;
    String visibleName;
    MirrorDevice(@NonNull NsdServiceInfo info) {
        this.serviceInfo = info;
        this.markedForRemoval = new AtomicBoolean(false);
        visibleName = "";
        for(Map.Entry<String, byte[]> entry : info.getAttributes().entrySet()){
            Log.d(LOG_TAG, "TXTRecord: " + entry.getKey() + " Value: " + new String(entry.getValue()));
            if (entry.getKey().equals("devicename")){
                visibleName = new String(entry.getValue());
                break;
            }
        }
    }
    @Override
    public String toString() {
        return "VisibleName: " + visibleName + " | ServiceName: " + serviceInfo.getServiceName();
    }
}
