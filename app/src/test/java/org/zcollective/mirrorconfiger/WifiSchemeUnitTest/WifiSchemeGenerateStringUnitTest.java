package org.zcollective.mirrorconfiger.WifiSchemeUnitTest;

import org.junit.Test;
import org.zcollective.mirrorconfiger.util.wifi.parser.WifiScheme;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class WifiSchemeGenerateStringUnitTest {

    // TODO: extend String-Generation testing, if actually necessary

    @Test
    public void toString_transitive_correct_input() {
        WifiScheme scheme1 = WifiScheme.parse("WIFI:T:WPA;S:MIRROR-Network;P:P4S5W0RD;H:TRUE;;");
        WifiScheme scheme2 = WifiScheme.parse(scheme1.toString());

        assertThat(scheme2.getPsk(), is(scheme1.getPsk()));
        assertThat(scheme2.getAuthentication(), is(scheme1.getAuthentication()));
        assertThat(scheme2.getSsid(), is(scheme1.getSsid()));
        assertThat(scheme2.isHidden(), is(scheme1.isHidden()));
        assertThat(scheme2.isMirrorConfiguration(), is(scheme1.isMirrorConfiguration()));
        assertThat(scheme2.toString(), is(scheme1.toString()));
    }

    @Test
    public void toString_transitive_nopass() {
        WifiScheme scheme1 = WifiScheme.parse("WIFI:S:MIRROR-Network;;");
        WifiScheme scheme2 = WifiScheme.parse(scheme1.toString());

        assertThat(scheme2.getPsk(), is(scheme1.getPsk()));
        assertThat(scheme2.getAuthentication(), is(scheme1.getAuthentication()));
        assertThat(scheme2.getSsid(), is(scheme1.getSsid()));
        assertThat(scheme2.isHidden(), is(scheme1.isHidden()));
        assertThat(scheme2.isMirrorConfiguration(), is(scheme1.isMirrorConfiguration()));
        assertThat(scheme2.toString(), is(scheme1.toString()));
    }

    @Test
    public void toString_transitive_nopass_hidden() {
        WifiScheme scheme1 = WifiScheme.parse("WIFI:S:MIRROR-Network;H:TRUE;;");
        WifiScheme scheme2 = WifiScheme.parse(scheme1.toString());

        assertThat(scheme2.getPsk(), is(scheme1.getPsk()));
        assertThat(scheme2.getAuthentication(), is(scheme1.getAuthentication()));
        assertThat(scheme2.getSsid(), is(scheme1.getSsid()));
        assertThat(scheme2.isHidden(), is(scheme1.isHidden()));
        assertThat(scheme2.isMirrorConfiguration(), is(scheme1.isMirrorConfiguration()));
        assertThat(scheme2.toString(), is(scheme1.toString()));
    }

    @Test
    public void toString_transitive_wep() {
        WifiScheme scheme1 = WifiScheme.parse("WIFI:T:WEP;S:MIRROR-Network;P:P4S5W0RD;;");
        WifiScheme scheme2 = WifiScheme.parse(scheme1.toString());

        assertThat(scheme2.getPsk(), is(scheme1.getPsk()));
        assertThat(scheme2.getAuthentication(), is(scheme1.getAuthentication()));
        assertThat(scheme2.getSsid(), is(scheme1.getSsid()));
        assertThat(scheme2.isHidden(), is(scheme1.isHidden()));
        assertThat(scheme2.isMirrorConfiguration(), is(scheme1.isMirrorConfiguration()));
        assertThat(scheme2.toString(), is(scheme1.toString()));
    }

    @Test
    public void toString_transitive_wep_hidden() {
        WifiScheme scheme1 = WifiScheme.parse("WIFI:T:WPA;S:MIRROR-Network;P:P4S5W0RD;H:TRUE;;");
        WifiScheme scheme2 = WifiScheme.parse(scheme1.toString());

        assertThat(scheme2.getPsk(), is(scheme1.getPsk()));
        assertThat(scheme2.getAuthentication(), is(scheme1.getAuthentication()));
        assertThat(scheme2.getSsid(), is(scheme1.getSsid()));
        assertThat(scheme2.isHidden(), is(scheme1.isHidden()));
        assertThat(scheme2.isMirrorConfiguration(), is(scheme1.isMirrorConfiguration()));
        assertThat(scheme2.toString(), is(scheme1.toString()));
    }

    @Test
    public void toString_transitive_wpa() {
        WifiScheme scheme1 = WifiScheme.parse("WIFI:T:WPA;S:MIRROR-Network;P:P4S5W0RD;;");
        WifiScheme scheme2 = WifiScheme.parse(scheme1.toString());

        assertThat(scheme2.getPsk(), is(scheme1.getPsk()));
        assertThat(scheme2.getAuthentication(), is(scheme1.getAuthentication()));
        assertThat(scheme2.getSsid(), is(scheme1.getSsid()));
        assertThat(scheme2.isHidden(), is(scheme1.isHidden()));
        assertThat(scheme2.isMirrorConfiguration(), is(scheme1.isMirrorConfiguration()));
        assertThat(scheme2.toString(), is(scheme1.toString()));
    }

    @Test
    public void toString_transitive_wpa_hidden() {
        WifiScheme scheme1 = WifiScheme.parse("WIFI:T:WPA;S:MIRROR-Network;P:P4S5W0RD;H:TRUE;;");
        WifiScheme scheme2 = WifiScheme.parse(scheme1.toString());

        assertThat(scheme2.getPsk(), is(scheme1.getPsk()));
        assertThat(scheme2.getAuthentication(), is(scheme1.getAuthentication()));
        assertThat(scheme2.getSsid(), is(scheme1.getSsid()));
        assertThat(scheme2.isHidden(), is(scheme1.isHidden()));
        assertThat(scheme2.isMirrorConfiguration(), is(scheme1.isMirrorConfiguration()));
        assertThat(scheme2.toString(), is(scheme1.toString()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void toString_default_throws() {
        new WifiScheme().toString();
    }

    @Test
    public void toString_minimal_config() {
        WifiScheme scheme = WifiScheme.parse("WIFI:S:MIRROR;;");
        String wifiString = scheme.toString();

        assertThat(wifiString, is(notNullValue()));
        assertThat(wifiString, is("WIFI:S:MIRROR;;"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void toString_ssid_null() {
        fail();
    }

    @Test
    public void toString_authentication_null() {
        fail();
    }

    @Test
    public void toString_psk_null() {
        fail();
    }

    @Test
    public void toString_hidden() {
        fail();
    }

    @Test
    public void toString_not_hidden() {
        fail();
    }
}