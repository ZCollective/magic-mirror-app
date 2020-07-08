package org.zcollective.mirrorconfiger.WifiSchemeUnitTest;

import org.junit.Test;
import org.zcollective.mirrorconfiger.util.wifi.parser.WifiScheme;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;

public class WifiSchemeAuthenticationTest {

    @Test
    public void authentication_lookup_empty_string() {
        assertThat(WifiScheme.Authentication.lookup(""), is(WifiScheme.Authentication.NOPASS));
    }

    @Test
    public void authentication_lookup_non_existent_element() {
        assertThat(WifiScheme.Authentication.lookup("WHAT"), is(nullValue()));
    }

    @Test
    public void authentication_lookup_nopass() {
        assertThat(WifiScheme.Authentication.lookup("nopass"), is(WifiScheme.Authentication.NOPASS));
    }

    @Test
    public void authentication_lookup_wep() {
        assertThat(WifiScheme.Authentication.lookup("WEP"), is(WifiScheme.Authentication.WEP));
    }

    @Test
    public void authentication_lookup_wpa() {
        assertThat(WifiScheme.Authentication.lookup("WPA"), is(WifiScheme.Authentication.WPA));
    }
}