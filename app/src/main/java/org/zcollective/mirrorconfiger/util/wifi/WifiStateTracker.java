package org.zcollective.mirrorconfiger.util.wifi;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public abstract class WifiStateTracker extends Fragment {

    public static final String TAG = "WifiStateTracker";

    public interface WifiStateChangedCallback {
        void onAvailable();
        void onLosing();
        void onLost();
        void onCapabilitiesChanged();
        void onLinkPropertiesChanged();
    }

    /**
     * Use this to only override methods you actually need
     */
    public static class SimpleWifiStateChangedCallback implements WifiStateChangedCallback {

        @Override
        public void onAvailable() {
//            Log.i(TAG, "Connected to new network: {" + network + "}");
        }

        @Override
        public void onLosing() {}

        @Override
        public void onLost() {}

        @Override
        public void onCapabilitiesChanged() {}

        @Override
        public void onLinkPropertiesChanged() {}
    }

    final WifiStateTrackerOreo.WifiStateChangedCallback callback;

    WifiStateTracker(@NonNull WifiStateTrackerOreo.WifiStateChangedCallback callback) {
        this.callback = callback;
    }

    public abstract void destroy();
}
