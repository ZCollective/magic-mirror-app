package org.zcollective.mirrorconfiger.splashscreen;

import android.app.Application;
import android.os.Build;
import android.util.Log;

/**
 * Created by Andreas Knipl <Andreas.Knipl@medicospeaker.com>
 */
public class App extends Application {

    private static final String LOG_TAG = "App";

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(LOG_TAG, "Running App::onCreate()");

        Log.d(LOG_TAG, "Android-Version:\nBASE_OS: " + Build.VERSION.BASE_OS +
                       "\nCODENAME: " + Build.VERSION.CODENAME +
                       "\nRELEASE: " + Build.VERSION.RELEASE +
                       "\nSECURITY_PATCH: " + Build.VERSION.SECURITY_PATCH +
                       "\nSDK_INT: " + Build.VERSION.SDK_INT);
    }
}
