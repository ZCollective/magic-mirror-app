package org.zcollective.mirrorconfiger.qrscanner.dialogs;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import org.zcollective.mirrorconfiger.R;

import java.lang.ref.WeakReference;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.PublishSubject;

import static org.zcollective.mirrorconfiger.qrscanner.dialogs.EnableWifiDialog.WifiDialogResult.PROCEED_ENABLE_WIFI;
import static org.zcollective.mirrorconfiger.qrscanner.dialogs.EnableWifiDialog.WifiDialogResult.STOP_ENABLE_WIFI;

public class EnableWifiDialog extends Fragment {

    public static final String TAG = "EnableWifiDialog";

    public enum WifiDialogResult {
        PROCEED_ENABLE_WIFI, STOP_ENABLE_WIFI
    }

    private PublishSubject<WifiDialogResult> resultSubject = PublishSubject.create();
    private AlertDialog dialog;
    private WeakReference<Context> ctx;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDetach() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        ctx.clear();
        ctx = null;
        super.onDetach();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        ctx = new WeakReference<>(context);
    }

    public Single<WifiDialogResult> show() {
        return Single.create(emitter -> {
            dialog = new AlertDialog
                    .Builder(ctx.get())
                    .setCancelable(false)
                    .setMessage(R.string.wifi_disabled)
                    .setOnDismissListener(___ -> emitter.onSuccess(STOP_ENABLE_WIFI))
                    .setPositiveButton(getString(R.string.enable_wifi), (__, ___) -> emitter.onSuccess(PROCEED_ENABLE_WIFI))
                    .setNegativeButton(getString(R.string.exit_app), (__, ___) -> emitter.onSuccess(STOP_ENABLE_WIFI))
                    .show();
        });
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
    }
}
