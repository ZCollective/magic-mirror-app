package org.zcollective.mirrorconfiger.util.mirrordiscovery;

import android.util.Log;

import org.zcollective.mirrorconfiger.util.mirrordiscovery.MirrorDiscoveryHelper.DiscoveryState;

public class NsdManager implements Runnable {

    private static final String NSD_MANAGER_LOG_TAG = "NSD-Manager";

    private MirrorDiscoveryHelper headlessMirrorDiscoveryHelper;

    public NsdManager(MirrorDiscoveryHelper helper) {
        headlessMirrorDiscoveryHelper = helper;
    }

    @Override
    public void run() {
        Log.i(NSD_MANAGER_LOG_TAG, "Scheduled NSD-Management woke up");

        if (headlessMirrorDiscoveryHelper == null) {
            Log.i(NSD_MANAGER_LOG_TAG, "No mirror-discovery-helper available!");
        } else {
            @DiscoveryState int state = headlessMirrorDiscoveryHelper.getState();

            switch (state) {

                default:
                    throw new IllegalStateException("Unexpected value: " + state);
                case DiscoveryState.NOT_READY:
                    Log.i(NSD_MANAGER_LOG_TAG, "State change: 'not_ready' -> 'not_ready'");
                    break;
                case DiscoveryState.READY:
                    Log.i(NSD_MANAGER_LOG_TAG, "State change: 'ready' -> 'started'");
                    headlessMirrorDiscoveryHelper.startDiscovery();
                    break;
                case DiscoveryState.STARTED:
                    Log.i(NSD_MANAGER_LOG_TAG, "State change: 'started' -> 'ready'");
                    headlessMirrorDiscoveryHelper.stopDiscovery();
                    break;
                case DiscoveryState.STOPPED:
                    Log.i(NSD_MANAGER_LOG_TAG, "State change: 'stopped' -> 'stopped'");
                    break;
            }
        }
    }
}
