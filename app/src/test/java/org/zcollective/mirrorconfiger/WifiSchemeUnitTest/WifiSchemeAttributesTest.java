package org.zcollective.mirrorconfiger.WifiSchemeUnitTest;

import org.junit.Test;
import org.zcollective.mirrorconfiger.util.wifi.parser.WifiScheme;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;

public class WifiSchemeAttributesTest {

    @Test
    public void getAuthentication_default() {
        WifiScheme scheme = new WifiScheme();
        assertThat(scheme.getAuthentication(), is(nullValue()));
    }

    @Test
    public void getAuthentication_reproducible() {
        WifiScheme scheme = new WifiScheme();
        assertThat(scheme.getAuthentication(), is(nullValue()));
        assertThat(scheme.getAuthentication(), is(nullValue()));
    }

    @Test
    public void getSsid_default() {
        WifiScheme scheme = new WifiScheme();
        assertThat(scheme.getSsid(), is(nullValue()));
    }

    @Test
    public void getSsid_reproducible() {
        WifiScheme scheme = new WifiScheme();
        assertThat(scheme.getSsid(), is(nullValue()));
        assertThat(scheme.getSsid(), is(nullValue()));
    }

    @Test
    public void getPsk_default() {
        WifiScheme scheme = new WifiScheme();
        assertThat(scheme.getPsk(), is(nullValue()));
    }

    @Test
    public void getPsk_reproducible() {
        WifiScheme scheme = new WifiScheme();
        assertThat(scheme.getPsk(), is(nullValue()));
        assertThat(scheme.getPsk(), is(nullValue()));
    }

    @Test
    public void isHidden_default() {
        WifiScheme scheme = new WifiScheme();
        assertThat(scheme.isHidden(), is(false));
    }

    @Test
    public void isHidden_reproducible() {
        WifiScheme scheme = new WifiScheme();
        assertThat(scheme.isHidden(), is(false));
        assertThat(scheme.isHidden(), is(false));
    }
}