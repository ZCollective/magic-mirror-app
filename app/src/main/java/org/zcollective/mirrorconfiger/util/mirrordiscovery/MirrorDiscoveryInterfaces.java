package org.zcollective.mirrorconfiger.util.mirrordiscovery;

import android.net.nsd.NsdServiceInfo;

import androidx.annotation.NonNull;

public class MirrorDiscoveryInterfaces {
    public interface MirrorResolveCallback {
        void onMirrorResolved(@NonNull NsdServiceInfo serviceInfo);
        void onMirrorResolveFailed(@NonNull NsdServiceInfo serviceInfo, int errorCode);
        void onMirrorLost(@NonNull NsdServiceInfo serviceInfo);
        void onDiscoveryStarted();
        void onDiscoveryStopped();
    }
}
