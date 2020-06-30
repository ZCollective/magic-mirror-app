package org.zcollective.mirrorconfiger.splashscreen;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.zcollective.mirrorconfiger.MainActivity;

import timber.log.Timber;

/**
 * THis is needed to display the splash-screen on startup
 */
public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Timber.i("Running SplashScreen::onCreate()");

        startActivity(new Intent(this, MainActivity.class));
        finish();

        Timber.i("Finished SplashScreen::onCreate()");
    }
}
