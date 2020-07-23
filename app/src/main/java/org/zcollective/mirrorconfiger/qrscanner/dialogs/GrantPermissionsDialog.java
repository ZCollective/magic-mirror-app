package org.zcollective.mirrorconfiger.qrscanner.dialogs;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.lang.ref.WeakReference;

import io.reactivex.rxjava3.core.Single;

import static org.zcollective.mirrorconfiger.qrscanner.dialogs.GrantPermissionsDialog.GrantPermissionsDialogResult.ALLOW_PERMISSION;
import static org.zcollective.mirrorconfiger.qrscanner.dialogs.GrantPermissionsDialog.GrantPermissionsDialogResult.DENY_PERMISSION;

public class GrantPermissionsDialog extends Fragment {

    public static final String TAG = "GrantPermissionsDialog";

    public enum GrantPermissionsDialogResult {
        ALLOW_PERMISSION, DENY_PERMISSION
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

    public Single<GrantPermissionsDialogResult> show() {
        return Single.create(emitter -> {
            dialog = new AlertDialog
                    .Builder(ctx.get())
                    .setCancelable(false)
                    .setMessage("You need to allow access permissions")
                    .setPositiveButton("OK", (__, ___) -> emitter.onSuccess(ALLOW_PERMISSION))
                    .setNegativeButton("Cancel", (__, ___) -> emitter.onSuccess(DENY_PERMISSION))
                    .show();
        });
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
    }
}
