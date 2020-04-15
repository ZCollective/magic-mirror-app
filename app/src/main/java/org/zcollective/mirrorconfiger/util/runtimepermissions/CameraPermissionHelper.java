package org.zcollective.mirrorconfiger.util.runtimepermissions;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class CameraPermissionHelper extends Fragment {

    public static final String TAG = "CameraPermissionFragment";
    private static final int REQUEST_CAMERA_PERMISSIONS = 10;

    private static volatile boolean cameraPermissionDenied;

    private CameraPermissionCallback mCallback;
//    private WeakReference<Activity> activity;

    public CameraPermissionHelper() {
        // Required empty public constructor
    }

    public static CameraPermissionHelper newInstance() {
        return new CameraPermissionHelper();
    }

    public static boolean isCameraPermissionDenied() {
        return cameraPermissionDenied;
    }

    public static void setCameraPermissionDenied(boolean cameraPermissionDenied) {
        CameraPermissionHelper.cameraPermissionDenied = cameraPermissionDenied;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof CameraPermissionCallback) {
            Log.wtf(TAG, "asdfasdfasdf");
            mCallback = (CameraPermissionCallback) context;
        } else {
            // TODO: change this
            throw new IllegalArgumentException("activity must extend BaseActivity and implement " +
                                               "LocationHelper.LocationCallback");
        }

//        if (context instanceof Activity) {
//            activity = new WeakReference<>((Activity) context);
//        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

    public void checkCameraPermissions(Activity activity) {
//        if (PermissionUtil.hasSelfPermission(getActivity(), new String[]{
        if (PermissionUtil.hasSelfPermission(activity, Manifest.permission.CAMERA)) {
            mCallback.onCameraPermissionGranted();
        } else {
            // UNCOMMENT TO SUPPORT ANDROID M RUNTIME PERMISSIONS
            if (!cameraPermissionDenied) {
                requestPermissions(new String[]{
                        Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSIONS);
            }
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSIONS) {
            if (PermissionUtil.verifyPermissions(grantResults)) {
                mCallback.onCameraPermissionGranted();
            } else {
                Log.i(TAG, "Permissions were NOT granted.");
                mCallback.onCameraPermissionDenied();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    public interface CameraPermissionCallback {
        void onCameraPermissionGranted();

        void onCameraPermissionDenied();
    }
}
