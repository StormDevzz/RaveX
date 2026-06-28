package ravex.modules.misc;

import ravex.modules.client.Settings;

public class Proxy {
    public static final Proxy INSTANCE = new Proxy();

    private Proxy() {}

    public boolean getEnabled() {
        return Settings.INSTANCE != null && Settings.INSTANCE.proxyEnabled.getValue();
    }

    public String getType() {
        return Settings.INSTANCE != null ? Settings.INSTANCE.proxyType.getValue() : "SOCKS5";
    }

    public String getHost() {
        return Settings.INSTANCE != null ? Settings.INSTANCE.proxyHost.getValue() : "127.0.0.1";
    }

    public int getPort() {
        return Settings.INSTANCE != null ? Settings.INSTANCE.proxyPort.getValue().intValue() : 1080;
    }

    public boolean hasAuth() {
        return Settings.INSTANCE != null && Settings.INSTANCE.proxyAuth.getValue();
    }

    public String getUsername() {
        return Settings.INSTANCE != null ? Settings.INSTANCE.proxyUsername.getValue() : "";
    }

    public String getPassword() {
        return Settings.INSTANCE != null ? Settings.INSTANCE.proxyPassword.getValue() : "";
    }

    public void toggle() {
        if (Settings.INSTANCE != null) {
            boolean current = Settings.INSTANCE.proxyEnabled.getValue();
            Settings.INSTANCE.proxyEnabled.setValue(!current);
        }
    }
}
