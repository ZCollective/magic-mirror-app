package org.zcollective.mirrorconfiger.WifiSchemeUnitTest;

import org.junit.Test;
import org.zcollective.mirrorconfiger.util.wifi.parser.WifiScheme;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class WifiSchemeDetectMirrorNetworkTest {

    @Test
    public void isMirrorConfiguration_default_is_false() {
        WifiScheme scheme = new WifiScheme();
        assertThat(scheme.isMirrorConfiguration(), is(false));
    }

    @Test
    public void isMirrorConfiguration_wrong_ssid() {
        WifiScheme scheme = new WifiScheme();
        scheme.parseSchema("WIFI:T:WPA;S:Wrong-Network;P:P4S5W0RD;H:TRUE;;");
        assertThat(scheme.isMirrorConfiguration(), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void isMirrorConfiguration_no_authentication() {
        WifiScheme scheme = new WifiScheme();
        scheme.parseSchema("");
        assertThat(scheme.isMirrorConfiguration(), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void isMirrorConfiguration_no_psk() {
        WifiScheme scheme = new WifiScheme();
        scheme.parseSchema("");
        assertThat(scheme.isMirrorConfiguration(), is(false));
    }
}
