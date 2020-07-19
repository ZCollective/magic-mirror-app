package org.zcollective.mirrorconfiger.splashscreen;

import android.app.Application;
import android.os.Build;
import android.os.StrictMode;

import org.zcollective.mirrorconfiger.BuildConfig;

import timber.log.Timber;

public class App extends Application {

    @Override
    public void onCreate() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());

            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectResourceMismatches()
                    .detectCustomSlowCalls()
                    .detectNetwork()
//                    .detectAll()  // Disabled because Read/Write floods Samsung Devices for no reason
                    .penaltyLog() //Logs a message to LogCat
                    .build());

            if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {
                StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(StrictMode.getThreadPolicy())
                        .detectUnbufferedIo()
                        .build());
            }

            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }

        Timber.i("Running App::onCreate()");
        Timber.d("Android-Version:\nBASE_OS: %S\nCODENAME: %S\nRELEASE: %S\nSECURITY_PATCH: %S\nSDK_INT: %d",
                Build.VERSION.BASE_OS, Build.VERSION.CODENAME, Build.VERSION.RELEASE,
                Build.VERSION.SECURITY_PATCH, Build.VERSION.SDK_INT
        );

        super.onCreate();
    }
}
