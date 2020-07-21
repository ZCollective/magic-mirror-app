package org.zcollective.mirrorconfiger.util.wifi.parser;

import android.net.wifi.WifiConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.zcollective.mirrorconfiger.util.wifi.parser.SchemeUtil.getParameters;

/**
 * Decodes/Encodes a Wifi-Configuration obtained as a string. Legal format is:
 * <code>WIFI:T:AUTHENTICATION;S:SSID;P:PSK;H:HIDDEN;</code>
 *
 * Taken from:
 * https://github.com/zxing/zxing/wiki/Barcode-Contents#wi-fi-network-config-android-ios-11
 */
public class WifiScheme {

    private static final String QUOTE_PATTERN = Pattern.compile("^\".+\"$").pattern();
    private static final String HEX_PATTERN = Pattern.compile("^[\\p{XDigit}]+$").pattern();

    private static final class Constants {
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
     * @return if SSID is hidden
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * @param value if SSID is hidden
     */
    private void setHidden(@Nullable final String value) {
        setHidden(Boolean.parseBoolean(value));
    }

    /**
     * @param hidden if SSID is hidden
     */
    private void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public enum Authentication {
        WEP("WEP"),
        WPA("WPA"),
        NOPASS("nopass");

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
            String str = label.trim();

            if (str.isEmpty()) {
                return Authentication.NOPASS;
            } else {
                return BY_LABEL.get(str);
            }
        }
    }

    // TODO: Special characters: \, ;, , and :
    // TODO: maybe do this in constant time, for now we might be vulnerable to side-channel-attacks
    public void parseSchema(String code) throws IllegalArgumentException, NullPointerException {
        if (code == null || !code.startsWith(Constants.WIFI_PROTOCOL_HEADER)) {
            throw new IllegalArgumentException("this is not a valid WIFI code: " + code);
        }

        int lastDelimiter = code.lastIndexOf(';');
        if (lastDelimiter <= Constants.WIFI_PROTOCOL_HEADER.length()) {
            throw new IllegalArgumentException("this is not a valid WIFI code: " + code);
        }

        String stringParams = code.substring(Constants.WIFI_PROTOCOL_HEADER.length(), lastDelimiter);
        Map<String, String> parameters = getParameters(stringParams, "(?<!\\\\);");

        String param;

        /*
         * Network SSID. Required. Enclose in double quotes if it is an ASCII name,
         * but could be interpreted as hex (i.e. "ABCD")
         */
        if (parameters.containsKey(Constants.SSID) && (param = parameters.get(Constants.SSID)) != null) {
            String ssid;

            if (param.matches(HEX_PATTERN)) {
                ssid = hexToAscii(param);
            } else {
                ssid = unescape(param);
                if (ssid.matches(QUOTE_PATTERN)) {
                    ssid = ssid.substring(1, ssid.length() - 1);
                }
            }

            // TODO: SSID can also be NULL-bytes, might not actually be printable or normal Strings
            if (ssid.length() > 32) {
                // SSID length constraint
                throw new IllegalArgumentException("SSID too long!");
            } else {
                setSsid(ssid);
            }
        } else {
            throw new IllegalArgumentException("No SSID specified!");
        }

        /*
         * Authentication type; can be WEP or WPA, or nopass for no password.
         * Or, omit for no password.
         * Enclose in double quotes if it is an ASCII name,
         * but could be interpreted as hex (i.e. "ABCD")
         */
        if (parameters.containsKey(Constants.AUTHENTICATION) && (param = parameters.get(Constants.AUTHENTICATION)) != null) {
            Authentication auth = Authentication.lookup(param);
            if (auth != null) {
                setAuthentication(auth);

                /*
                 * Password, ignored if T is nopass (in which case it may be omitted).
                 * Enclose in double quotes if it is an ASCII name, but could be interpreted as hex
                 * (i.e. "ABCD")
                 */
                if (auth == Authentication.NOPASS) {
                    setPsk(null);
                } else {
                    String psk;

                    if (parameters.containsKey(Constants.PSK) && (param = parameters.get(Constants.PSK)) != null) {
                        if (param.matches(HEX_PATTERN)) {
                            psk = hexToAscii(param);
                        } else {
                            psk = unescape(param);
                            if (psk.matches(QUOTE_PATTERN)) {
                                String pskExtract = psk.substring(1, psk.length() - 1);
                                if (pskExtract.matches(HEX_PATTERN)) psk = pskExtract;
                            }
                        }
                    } else {
                        throw new IllegalArgumentException("No PSK specified!");
                    }

                    int pskLength = psk.length();

                    if (auth == Authentication.WPA) {
                        if (pskLength < 8 || pskLength > 63) {
                            // WPA PSK length-constraint
                            throw new IllegalArgumentException("Illegal PSK-length: " + psk.length());
                        } else {
                            setPsk(psk);
                        }
                    } else if (auth == Authentication.WEP) {
                        // WEP-64/128/152/256 PSK-Length = WEP-Level - 24 Bits (IV)
                        if (pskLength == 5 || pskLength == 13 || pskLength == 16 || pskLength == 29) {
                            setPsk(psk);
                        } else {
                            // WEP PSK length-constraint
                            throw new IllegalArgumentException("Illegal PSK-length: " + pskLength);
                        }
                    }
                }
            } else {
                throw new IllegalArgumentException("No AUTHENTICATION specified!");
            }
        } else {
            setAuthentication(Authentication.NOPASS);
            setPsk(null);
        }

        /*
         * Optional. True if the network SSID is hidden.
         */
        if (parameters.containsKey(Constants.HIDDEN)) {
            setHidden(parameters.get(Constants.HIDDEN));
        }
    }

    public boolean isMirrorConfiguration() {
        return ssid != null && !ssid.isEmpty() && authentication != null && psk != null
                && ssid.startsWith("Mirror");
    }

    public WifiConfiguration generateWifiConfiguration() {
        if (this.ssid == null) {
            throw new IllegalArgumentException("SSID has to be non-null!");
        }

        WifiConfiguration wifiConfig = new WifiConfiguration();

        wifiConfig.SSID = String.format("\"%s\"", this.ssid);
        wifiConfig.hiddenSSID = this.hidden;
        wifiConfig.status = WifiConfiguration.Status.ENABLED;

        // Might only be required for Unit-Testing
        // TODO: this is still weird behaviour, we might want to investigate
        if (wifiConfig.allowedKeyManagement == null) {
            wifiConfig.allowedKeyManagement = new BitSet();
        }
        if (wifiConfig.allowedAuthAlgorithms == null) {
            wifiConfig.allowedAuthAlgorithms = new BitSet();
        }

        // Taken from WifiConfiguration#setSecurityParams()
        if (this.authentication == Authentication.WEP) {

            wifiConfig.preSharedKey = String.format("\"%s\"", this.psk);
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);

        } else if (this.authentication == Authentication.WPA) {

            wifiConfig.preSharedKey = String.format("\"%s\"", this.psk);
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

        } else {

            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }

        return wifiConfig;
    }

    @NonNull
    @Override
    public String toString() throws IllegalArgumentException {
        StringBuilder builder = new StringBuilder(Constants.WIFI_PROTOCOL_HEADER);

        if (getSsid() != null) {
            builder.append(Constants.SSID).append(":").append(escape(getSsid())).append(";");
        } else {
            throw new IllegalArgumentException("Network SSID is required!");
        }

        if (getAuthentication() == null || getPsk() == null) {
            builder.append(Constants.AUTHENTICATION).append(":").append("nopass").append(";");
        } else {
            builder.append(Constants.AUTHENTICATION).append(":").append(getAuthentication()).append(";");
            builder.append(Constants.PSK).append(":").append(escape(getPsk())).append(";");
        }

        if (isHidden()) {
            builder.append(Constants.HIDDEN).append(":").append("TRUE").append(";");
        }

        return builder.append(";").toString();
    }

    @NonNull
    public static WifiScheme parse(@NonNull final String wifiCode) throws NullPointerException {
        Objects.requireNonNull(wifiCode);

        WifiScheme wifi = new WifiScheme();
        wifi.parseSchema(wifiCode);
        return wifi;
    }

    @NonNull
    private static String hexToAscii(@NonNull String text) throws NullPointerException {
        Objects.requireNonNull(text);
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < text.length(); i += 2) {
            String str = text.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }

        return output.toString();
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
