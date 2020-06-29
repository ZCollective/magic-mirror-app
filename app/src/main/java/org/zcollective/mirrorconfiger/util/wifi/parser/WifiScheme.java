package org.zcollective.mirrorconfiger.util.wifi.parser;

import android.net.wifi.WifiConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.zcollective.mirrorconfiger.util.wifi.parser.SchemeUtil.getParameters;

/**
 * Decodes/Encodes a Wifi-Configuration obtained as a string. Legal format is:
 * <code>WIFI:T:AUTHENTICATION;S:SSID;P:PSK;H:HIDDEN;</code>
 */
public class WifiScheme {

    private static class Constants {
        private static final String WIFI_PROTOCOL_HEADER = "WIFI:";
        private static final String AUTHENTICATION = "T";
        private static final String HIDDEN = "H";
        private static final String SSID = "S";
        private static final String PSK = "P";
    }

    private Authentication authentication;
    private String ssid;
    private String psk;
    private boolean hidden = false;

    public WifiScheme() {
        // empty public constructor
    }

    /**
     * @return the authentication
     */
    public Authentication getAuthentication() {
        return authentication;
    }

    /**
     * @param authentication the authentication to set
     */
    private void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    /**
     * @return the ssid
     */
    public String getSsid() {
        return ssid;
    }

    /**
     * @param ssid the ssid to set
     */
    private void setSsid(String ssid) {
        this.ssid = ssid;
    }

    /**
     * @return the psk
     */
    public String getPsk() {
        return psk;
    }

    /**
     * @param psk the psk to set
     */
    private void setPsk(String psk) {
        this.psk = psk;
    }

    /**
     * @return the hidden
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * @param value the hidden to set
     */
    private void setHidden(@Nullable final String value) {
        setHidden(Boolean.parseBoolean(value));
    }

    /**
     * @param hidden the hidden to set
     */
    private void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public enum Authentication {
        WEP("WEP"),
        WPA("WPA"),
        NOPASS("NOPASS");

        private static final Map<String, Authentication> BY_LABEL = new HashMap<>(3);

        static {
            for (Authentication e : values()) {
                BY_LABEL.put(e.label, e);
            }
        }

        private final String label;

        Authentication(String label) {
            this.label = label;
        }

        public static Authentication lookup(@NonNull String label) {
            if (label.trim().isEmpty()) {
                return Authentication.NOPASS;
            } else {
                return BY_LABEL.get(label.trim());
            }
        }
    }

    public void parseSchema(String code) throws IllegalArgumentException, NullPointerException {
        if (code == null || !code.startsWith(Constants.WIFI_PROTOCOL_HEADER)) {
            throw new IllegalArgumentException("this is not a valid WIFI code: " + code);
        }

        String stringParams = code.substring(Constants.WIFI_PROTOCOL_HEADER.length());
        Map<String, String> parameters = getParameters(stringParams, "(?<!\\\\);");

        String param;
        if (parameters.containsKey(Constants.SSID) && (param = parameters.get(Constants.SSID)) != null) {
            setSsid(unescape(param));
        } else {
            throw new IllegalArgumentException("No SSID specified!");
        }

        if (parameters.containsKey(Constants.AUTHENTICATION) && (param = parameters.get(Constants.AUTHENTICATION)) != null) {
            setAuthentication(Authentication.lookup(param.toUpperCase()));
        } else {
            throw new IllegalArgumentException("No AUTHENTICATION specified!");
        }

        if (parameters.containsKey(Constants.PSK) && (param = parameters.get(Constants.PSK)) != null) {
            setPsk(unescape(param));
        } else {
            throw new IllegalArgumentException("No PSK specified!");
        }

        if (parameters.containsKey(Constants.HIDDEN)) {
            setHidden(parameters.get(Constants.HIDDEN));
        }
    }

    public boolean isMirrorConfiguration() {
        return ssid != null && !ssid.isEmpty() && authentication != null && psk != null
                && ssid.startsWith("Mirror");
    }

    public WifiConfiguration generateWifiConfiguration() {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", this.ssid);
        wifiConfig.preSharedKey = String.format("\"%s\"", this.psk);
        wifiConfig.hiddenSSID = this.hidden;
        wifiConfig.status = WifiConfiguration.Status.ENABLED;

        // Taken from WifiConfiguration#setSecurityParams()
        if (this.authentication == Authentication.WEP) {

            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);

        } else if (this.authentication == Authentication.WPA) {

            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

        } else {

            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }

        return wifiConfig;
    }

    public String generateString() {
        StringBuilder builder = new StringBuilder(Constants.WIFI_PROTOCOL_HEADER);

        if (getSsid() != null) {
            builder.append(Constants.SSID).append(":").append(escape(getSsid())).append(";");
        }

        if (getAuthentication() != null) {
            builder.append(Constants.AUTHENTICATION).append(":").append(getAuthentication()).append(";");
        }

        if (getPsk() != null) {
            builder.append(Constants.PSK).append(":").append(escape(getPsk())).append(";");
        }

        builder.append(Constants.HIDDEN).append(":").append(isHidden()).append(";");
        return builder.toString();
    }

    @NonNull
    @Override
    public String toString() {
        return generateString();
    }

    @NonNull
    public static WifiScheme parse(@NonNull final String wifiCode) throws NullPointerException {
        Objects.requireNonNull(wifiCode);

        WifiScheme wifi = new WifiScheme();
        wifi.parseSchema(wifiCode);
        return wifi;
    }

    @NonNull
    private static String escape(@NonNull final String text) throws NullPointerException {
        Objects.requireNonNull(text);
        return text.replace("\\", "\\\\").replace(",", "\\,")
                .replace(";", "\\;").replace(".", "\\.")
                .replace("\"", "\\\"").replace("'", "\\'");
    }

    @NonNull
    private static String unescape(@NonNull final String text) throws NullPointerException {
        Objects.requireNonNull(text);
        return text.replace("\\\\", "\\").replace("\\,", ",")
                .replace("\\;", ";").replace("\\.", ".")
                .replace("\\\"", "\"").replace("\\'", "'");
    }
}
