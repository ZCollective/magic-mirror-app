package org.zcollective.mirrorconfiger.WifiSchemeUnitTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
        WifiSchemeAttributesTest.class,
        WifiSchemeAuthenticationTest.class,
        WifiSchemeDetectMirrorNetworkTest.class,
        WifiSchemeGenerateWifiConfigurationTest.class,
        WifiSchemeParserTest.class,
        WifiSchemeGenerateStringUnitTest.class
})
public class WifiSchemeUnitTest {
}
