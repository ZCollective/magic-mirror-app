package org.zcollective.mirrorconfiger.WifiSchemeUnitTest;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.zcollective.mirrorconfiger.util.wifi.parser.WifiScheme;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

public class WifiSchemeParserTest {

    @Test
    public void parse_correct_code() {
        WifiScheme scheme = WifiScheme.parse("WIFI:T:WPA;S:MIRROR-Network;P:P4S5W0RD;H:TRUE;;");

        assertThat(scheme, is(notNullValue()));
        assertThat(scheme.getAuthentication(), is(WifiScheme.Authentication.WPA));
        assertThat(scheme.getSsid(), is("MIRROR-Network"));
        assertThat(scheme.getPsk(), is("P4S5W0RD"));
        assertThat(scheme.isHidden(), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_illegal_protocol_header() {
        WifiScheme.parse("WHAT:T:WPA;S:MIRROR-Network;P:P4S5W0RD;H:TRUE;;");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_empty_protocol_header() {
        WifiScheme.parse(":T:WPA;S:MIRROR-Network;P:P4S5W0RD;H:TRUE;;");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_missing_protocol_header() {
        WifiScheme.parse("T:WPA;S:MIRROR-Network;P:P4S5W0RD;H:TRUE;;");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_illegal_authentication() {
        WifiScheme.parse("WIFI:T:WHAT;S:MIRROR-Network;P:P4S5W0RD;H:TRUE;;");
    }

    @Test
    public void parse_empty_authentication() {
        WifiScheme scheme = WifiScheme.parse("WIFI:;S:MIRROR-Network;P:P4S5W0RD;H:TRUE;;");

        assertThat(scheme, is(notNullValue()));
        assertThat(scheme.getAuthentication(), is(WifiScheme.Authentication.NOPASS));
        assertThat(scheme.getPsk(), is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_empty_ssid() {
        WifiScheme.parse("WIFI:T:WPA;S:;P:P4S5W0RD;H:TRUE;;");
    }

    @Test
    public void parse_nopass_missing_password() {
        WifiScheme scheme = WifiScheme.parse("WIFI:T:nopass;S:MIRROR-Network;H:TRUE;;");

        assertThat(scheme, is(notNullValue()));
        assertThat(scheme.getAuthentication(), is(WifiScheme.Authentication.NOPASS));
        assertThat(scheme.getPsk(), is(nullValue()));
    }

    @Test
    public void parse_nopass_omit_password() {
        WifiScheme scheme = WifiScheme.parse("WIFI:T:nopass;S:MIRROR-Network;P:P4S5W0RD;H:TRUE;;");

        assertThat(scheme, is(notNullValue()));
        assertThat(scheme.getAuthentication(), is(WifiScheme.Authentication.NOPASS));
        assertThat(scheme.getPsk(), is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_wep_missing_password() {
        WifiScheme.parse("WIFI:T:WEP;S:MIRROR-Network;H:TRUE;;");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_wpa_missing_password() {
        WifiScheme.parse("WIFI:T:WPA;S:MIRROR-Network;H:TRUE;;");
    }

    @Test
    public void parse_missing_authentication() {
        WifiScheme scheme = WifiScheme.parse("WIFI:S:MIRROR-Network;P:P4S5W0RD;H:TRUE;;");

        assertThat(scheme, is(notNullValue()));
        assertThat(scheme.getAuthentication(), is(WifiScheme.Authentication.NOPASS));
        assertThat(scheme.getPsk(), is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_explicit_authentication_missing_password() {
        WifiScheme.parse("WIFI:T:WPA;S:MIRROR-Network;H:TRUE;;");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_explicit_authentication_empty_password() {
        WifiScheme.parse("WIFI:T:WPA;S:MIRROR-Network;P:;H:TRUE;;");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_illegal_hidden() {
        WifiScheme.parse("WIFI:T:WHAT;S:MIRROR-Network;P:P4S5W0RD;H:WHAT;;");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_empty_hidden() {
        WifiScheme.parse("WIFI:T:WHAT;S:MIRROR-Network;P:P4S5W0RD;H:;;");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_missing_hidden() {
        WifiScheme.parse("WIFI:T:WHAT;S:MIRROR-Network;P:P4S5W0RD;;");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_illegal_token_limiter() {
        WifiScheme.parse("WIFI:T:WPA|S:MIRROR-Network|P:P4S5W0RD||");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_missing_token_limiter() {
        WifiScheme.parse("WIFI:T:WPAS:MIRROR-NetworkP:P4S5W0RD");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_ssid_to_long() {
        WifiScheme.parse("WIFI:T:WPA;S:MIRROR-NetworkMIRROR-NetworkMIRRO;P:P4S5W0RD;H:TRUE;;");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_wpa_psk_too_short() {
        WifiScheme.parse("WIFI:T:WPA;S:MIRROR-Network;P:P4S5;H:TRUE;;");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_wpa_psk_too_long() {
        WifiScheme.parse("WIFI:T:WPA;S:MIRROR-Network;P:P4S5W0RDP4S5W0RDP4S5W0RDP4S5W0RDP4S5W0RDP4S5W0RDP4S5W0RDP4S5W0RD;H:TRUE;;");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_wep64_psk_too_short() {
        WifiScheme.parse("WIFI:T:WEP;S:MIRROR-Network;P:P4S5;H:TRUE;;");
    }

    @Test
    public void parse_wep64_psk_correct() {
        WifiScheme scheme = WifiScheme.parse("WIFI:T:WEP;S:MIRROR-Network;P:P4S5W;H:TRUE;;");

        assertThat(scheme, is(notNullValue()));
        assertThat(scheme.getPsk(), is("P4S5W"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_wep64_psk_too_long() {
        WifiScheme.parse("WIFI:T:WEP;S:MIRROR-Network;P:P4S5W0;H:TRUE;;");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_wep128_psk_too_short() {
        WifiScheme.parse("WIFI:T:WEP;S:MIRROR-Network;P:P4S5W0RDP4S5;H:TRUE;;");
    }

    @Test
    public void parse_wep128_psk_correct() {
        WifiScheme scheme = WifiScheme.parse("WIFI:T:WEP;S:MIRROR-Network;P:P4S5W0RDP4S5W;H:TRUE;;");

        assertThat(scheme, is(notNullValue()));
        assertThat(scheme.getPsk(), is("P4S5W0RDP4S5W"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_wep128_psk_too_long() {
        WifiScheme.parse("WIFI:T:WEP;S:MIRROR-Network;P:P4S5W0RDP4S5W0;H:TRUE;;");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_wep152_psk_too_short() {
        WifiScheme.parse("WIFI:T:WEP;S:MIRROR-Network;P:P4S5W0RDP4S5W0R;H:TRUE;;");
    }

    @Test
    public void parse_wep152_psk_correct() {
        WifiScheme scheme = WifiScheme.parse("WIFI:T:WEP;S:MIRROR-Network;P:P4S5W0RDP4S5W0RD;H:TRUE;;");

        assertThat(scheme, is(notNullValue()));
        assertThat(scheme.getPsk(), is("P4S5W0RDP4S5W0RD"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_wep152_psk_too_long() {
        WifiScheme.parse("WIFI:T:WEP;S:MIRROR-Network;P:P4S5W0RDP4S5W0RDP;H:TRUE;;");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_wep256_psk_too_short() {
        WifiScheme.parse("WIFI:T:WEP;S:MIRROR-Network;P:P4S5W0RDP4S5W0RDP4S5W0RDP4S5;H:TRUE;;");
    }

    @Test
    public void parse_wep256_psk_correct() {
        WifiScheme scheme = WifiScheme.parse("WIFI:T:WEP;S:MIRROR-Network;P:P4S5W0RDP4S5W0RDP4S5W0RDP4S5W;H:TRUE;;");

        assertThat(scheme, is(notNullValue()));
        assertThat(scheme.getPsk(), is("P4S5W0RDP4S5W0RDP4S5W0RDP4S5W"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_wep256_psk_too_long() {
        WifiScheme.parse("WIFI:T:WEP;S:MIRROR-Network;P:P4S5W0RDP4S5W0RDP4S5W0RDP4S5W0;H:TRUE;;");
    }


    @Test
    public void parse_escaped_special_characters() {
        WifiScheme scheme = WifiScheme.parse("WIFI:T:WPA;S:MIRROR-Network\\;;P:P4S5W0RD;;");
        assertThat(scheme, is(notNullValue()));
        assertThat(scheme.getSsid(), is("MIRROR-Network;"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_unescaped_special_characters() {
        WifiScheme.parse("WIFI:T:WPA;S:MIRROR-,Network;P:P4S5W0RD;;");
    }

    @Test
    public void parse_ssid_hex() {
        String asciiSSID = "MIRROR";
        WifiScheme scheme = WifiScheme.parse("WIFI:T:WPA;S:" + asciiToHex(asciiSSID) + ";P:P4S5W0RD;;");

        assertThat(scheme.getSsid(), is(asciiSSID));
    }

    @Test
    public void parse_ssid_hex_quoted() {
        String ssid = asciiToHex("MIRROR");
        WifiScheme scheme = WifiScheme.parse("WIFI:T:WPA;S:\"" + ssid + "\";P:P4S5W0RD;;");

        assertThat(scheme.getSsid(), is(ssid));
    }

    @Test
    public void parse_psk_hex() {
        String asciiPSK = "P4S5W0RD";
        WifiScheme scheme = WifiScheme.parse("WIFI:T:WPA;S:MIRROR-Network;P:" + asciiToHex(asciiPSK) + ";;");

        assertThat(scheme.getPsk(), is(asciiPSK));
    }

    @Test
    public void parse_psk_hex_quoted() {
        String psk = asciiToHex("P4S5W0RD");
        WifiScheme scheme = WifiScheme.parse("WIFI:T:WPA;S:MIRROR-Network;P:\"" + psk + "\";;");

        assertThat(scheme.getPsk(), is(psk));
    }

    @NonNull
    private static String asciiToHex(@NonNull String asciiStr) {
        char[] chars = asciiStr.toCharArray();
        StringBuilder hex = new StringBuilder();
        for (char ch : chars) {
            hex.append(Integer.toHexString((int) ch));
        }
        return hex.toString();
    }
}