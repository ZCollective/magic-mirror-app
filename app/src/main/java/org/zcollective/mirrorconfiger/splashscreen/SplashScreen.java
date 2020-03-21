package org.zcollective.mirrorconfiger.splashscreen;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.zcollective.mirrorconfiger.MainActivity;

/**
 * THis is needed to display the splash-screen on startup
 */
public class SplashScreen extends AppCompatActivity {

    private static final String LOG_TAG = "SplashScreen";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(LOG_TAG, "Running SplashScreen::onCreate()");

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
