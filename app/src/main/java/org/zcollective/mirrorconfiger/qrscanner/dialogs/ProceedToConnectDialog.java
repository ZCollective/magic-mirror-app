package org.zcollective.mirrorconfiger.qrscanner.dialogs;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.lang.ref.WeakReference;

import io.reactivex.rxjava3.core.Single;

import static org.zcollective.mirrorconfiger.qrscanner.dialogs.ProceedToConnectDialog.ProceedToConnectDialogResult.START_CONNECT;
import static org.zcollective.mirrorconfiger.qrscanner.dialogs.ProceedToConnectDialog.ProceedToConnectDialogResult.STOP_CONNECT;

public class ProceedToConnectDialog extends Fragment {

    public static final String TAG = "ProceedToConnectDialog";

    public enum ProceedToConnectDialogResult {
        START_CONNECT, STOP_CONNECT
    }

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

    public Single<ProceedToConnectDialogResult> show() {
        return Single.create(emitter -> {
            dialog = new AlertDialog
                    .Builder(ctx.get())
                    .setCancelable(false)
                    .setMessage("Proceed to connect with Mirror?")
                    .setPositiveButton("OK", (__, ___) -> emitter.onSuccess(START_CONNECT))
                    .setNegativeButton("Cancel", (__, ___) -> emitter.onSuccess(STOP_CONNECT))
                    .show();
        });
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
    }
}
