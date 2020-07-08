package org.zcollective.mirrorconfiger.WifiSchemeUnitTest;

import android.net.wifi.WifiConfiguration;

import org.junit.BeforeClass;
import org.junit.Test;
import org.zcollective.mirrorconfiger.util.wifi.parser.WifiScheme;

import java.util.BitSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

public class WifiSchemeGenerateWifiConfigurationTest {

    private static BitSet OPEN_SHARED = new BitSet();
    private static BitSet WPA_PSK = new BitSet();
    private static BitSet NONE = new BitSet();

    @BeforeClass
    public static void setUp() {
        WPA_PSK.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        NONE.set(WifiConfiguration.KeyMgmt.NONE);
        OPEN_SHARED.set(WifiConfiguration.AuthAlgorithm.OPEN);
        OPEN_SHARED.set(WifiConfiguration.AuthAlgorithm.SHARED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateWifiConfiguration_default() {
        WifiScheme scheme = new WifiScheme();
        scheme.generateWifiConfiguration();
    }

    @Test
    public void generateWifiConfiguration_correct_input() {
        WifiScheme scheme = WifiScheme.parse("WIFI:T:WPA;S:MIRROR-Network;P:P4S5W0RD;;");
        WifiConfiguration config = scheme.generateWifiConfiguration();
        assertThat(config, is(notNullValue()));
        assertThat(config.SSID, is("\"MIRROR-Network\""));
        assertThat(config.preSharedKey, is("\"P4S5W0RD\""));
    }

    @Test
    public void generateWifiConfiguration_parameters_position_agnostic() {
        // TODO: DO
        WifiScheme scheme = WifiScheme.parse("WIFI:T:WPA;S:MIRROR-Network;P:P4S5W0RD;;");
        WifiConfiguration config = scheme.generateWifiConfiguration();
        assertThat(config, is(notNullValue()));
        assertThat(config.SSID, is("\"MIRROR-Network\""));
        assertThat(config.preSharedKey, is("\"P4S5W0RD\""));
    }

    @Test
    public void generateWifiConfiguration_nopass() {
        WifiScheme scheme = WifiScheme.parse("WIFI:T:nopass;S:MIRROR-Network;P:P4S5W0RD;H:FALSE;;");
        WifiConfiguration config = scheme.generateWifiConfiguration();
        assertThat(config, is(notNullValue()));
        assertThat(config.SSID, is("\"MIRROR-Network\""));
        assertThat(config.preSharedKey, is(nullValue()));
        assertThat(config.allowedKeyManagement, is(NONE));
        assertThat(config.hiddenSSID, is(false));
    }

    @Test
    public void generateWifiConfiguration_nopass_hidden() {
        WifiScheme scheme = WifiScheme.parse("WIFI:T:nopass;S:MIRROR-Network;P:P4S5W0RD;H:TRUE;;");
        WifiConfiguration config = scheme.generateWifiConfiguration();
        assertThat(config, is(notNullValue()));
        assertThat(config.SSID, is("\"MIRROR-Network\""));
        assertThat(config.preSharedKey, is(nullValue()));
        assertThat(config.allowedKeyManagement, is(NONE));
        assertThat(config.hiddenSSID, is(true));
    }

    @Test
    public void generateWifiConfiguration_wep() {
        WifiScheme scheme = WifiScheme.parse("WIFI:T:WEP;S:MIRROR-Network;P:P4S5W0RD;H:FALSE;;");
        WifiConfiguration config = scheme.generateWifiConfiguration();
        assertThat(config, is(notNullValue()));
        assertThat(config.SSID, is("\"MIRROR-Network\""));
        assertThat(config.preSharedKey, is("\"P4S5W0RD\""));
        assertThat(config.allowedKeyManagement, is(NONE));
        assertThat(config.allowedAuthAlgorithms, is(OPEN_SHARED));
        assertThat(config.hiddenSSID, is(false));
    }

    @Test
    public void generateWifiConfiguration_wep_hidden() {
        WifiScheme scheme = WifiScheme.parse("WIFI:T:WEP;S:MIRROR-Network;P:P4S5W0RD;H:TRUE;;");
        WifiConfiguration config = scheme.generateWifiConfiguration();
        assertThat(config, is(notNullValue()));
        assertThat(config.SSID, is("\"MIRROR-Network\""));
        assertThat(config.preSharedKey, is("\"P4S5W0RD\""));
        assertThat(config.allowedKeyManagement, is(NONE));
        assertThat(config.allowedAuthAlgorithms, is(OPEN_SHARED));
        assertThat(config.hiddenSSID, is(true));
    }

    @Test
    public void generateWifiConfiguration_wpa() {
        WifiScheme scheme = WifiScheme.parse("WIFI:T:WPA;S:MIRROR-Network;P:P4S5W0RD;H:FALSE;;");
        WifiConfiguration config = scheme.generateWifiConfiguration();
        assertThat(config, is(notNullValue()));
        assertThat(config.SSID, is("\"MIRROR-Network\""));
        assertThat(config.preSharedKey, is("\"P4S5W0RD\""));
        assertThat(config.allowedKeyManagement, is(WPA_PSK));
        assertThat(config.hiddenSSID, is(false));
    }

    @Test
    public void generateWifiConfiguration_wpa_hidden() {
        WifiScheme scheme = WifiScheme.parse("WIFI:T:WPA;S:MIRROR-Network;P:P4S5W0RD;H:TRUE;;");
        WifiConfiguration config = scheme.generateWifiConfiguration();
        assertThat(config, is(notNullValue()));
        assertThat(config.SSID, is("\"MIRROR-Network\""));
        assertThat(config.preSharedKey, is("\"P4S5W0RD\""));
        assertThat(config.allowedKeyManagement, is(WPA_PSK));
        assertThat(config.hiddenSSID, is(true));
    }
}