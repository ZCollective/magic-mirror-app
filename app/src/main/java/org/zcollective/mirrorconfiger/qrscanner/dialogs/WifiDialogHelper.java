package org.zcollective.mirrorconfiger.qrscanner.dialogs;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.Fragment;

import org.zcollective.mirrorconfiger.R;

public class WifiDialogHelper extends Fragment {

    // TODO: make this a Dialog-Thingy

    public static String TAG = "WifiDialogFragment";

    private Context ctx;
    private OnDialogCallback callback;
    private ConnectWifiDialogCallback callbackAlt;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        ctx = context;

        if (context instanceof OnDialogCallback) {
            callback = (OnDialogCallback) context;
        } else if (context instanceof ConnectWifiDialogCallback) {
            callbackAlt = (ConnectWifiDialogCallback) context;
        } else {
            throw new IllegalArgumentException("activity must extend BaseActivity and implement LocationHelper.LocationCallback");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ctx = null;
        callback = null;
    }

    private AppCompatDialog proceedToConnectDialog;

    public void showProceedToConnectDialog() {
        proceedToConnectDialog = new AlertDialog
                .Builder(ctx)
                .setMessage("Proceed to connect with Mirror?")
                .setPositiveButton("OK", (dialog, which) -> {
                    if (callback != null) callback.onProceedConnectingToWifi();
                    if (callbackAlt != null) callbackAlt.onProceedConnectingToWifi();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    if (callback != null) callback.onStopConnectingToWifi();
                    if (callbackAlt != null) callbackAlt.onStopConnectingToWifi();
                })
                .create();

        proceedToConnectDialog.show();
    }

    public void dismissProceedToConnectDialog() {
        if (proceedToConnectDialog != null) proceedToConnectDialog.dismiss();
    }

    public void showEnableWifiDialog() {
        new AlertDialog
                .Builder(ctx)
                .setCancelable(false)
                .setMessage(getString(R.string.wifi_disabled))
                .setPositiveButton(getString(R.string.enable_wifi), (dialog, which) -> callback.onProceedToEnableWifi())
                .setNegativeButton(getString(R.string.exit_app), (dialog, which) -> callback.onStopToEnableWifi())
                .show();
    }

    public void showGrantPermissionsDialog() {
        new AlertDialog.Builder(ctx)
                .setMessage("You need to allow access permissions")
                .setPositiveButton("OK", (dialog, which) -> callback.onProceedGrantingPermissions())
                .setNegativeButton("Cancel", (dialog, which) -> callback.onStopGrantingPermissions())
                .create()
                .show();
    }

    public interface OnDialogCallback {
        void onProceedConnectingToWifi();
        void onStopConnectingToWifi();
        void onProceedToEnableWifi();
        void onStopToEnableWifi();
        void onProceedGrantingPermissions();
        void onStopGrantingPermissions();
    }

    public interface ConnectWifiDialogCallback {
        void onProceedConnectingToWifi();
        void onStopConnectingToWifi();
    }
}
