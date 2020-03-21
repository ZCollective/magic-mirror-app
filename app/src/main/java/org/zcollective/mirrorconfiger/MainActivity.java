package org.zcollective.mirrorconfiger;

import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import org.zcollective.mirrorconfiger.qrscanner.QrScannerActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
            startActivity(new Intent(MainActivity.this, QrScannerActivity.class));
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void removeNetwork(View view) {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        List<WifiConfiguration> networks = wm.getConfiguredNetworks();
        boolean found = false;
        for (WifiConfiguration network : networks) {
            if (network.SSID.contentEquals("\"Mirror\"")) {
                found = true;
                wm.disconnect();
                boolean removed = wm.removeNetwork(network.networkId);
                Log.wtf("Main", "Removed Network " + network.SSID);
                Snackbar.make(view, "Removed Network " + network.SSID + ": " + removed, Snackbar.LENGTH_LONG)/*.setAction("Action", null)*/.show();
            }
        }
        if (!found) Snackbar.make(view, "Network unknown", Snackbar.LENGTH_LONG).show();

    }
}
