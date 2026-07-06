package ravex.manager;

import ravex.proxy.ProxyConfig;
import ravex.proxy.ProxyServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyManager {
    private static final ProxyManager INSTANCE = new ProxyManager();
    private static final Pattern PROXY_URL_PATTERN =
        Pattern.compile("^(socks5|socks4|http)://(?:([^:@]+)(?::([^@]*))?@)?([^:]+):(\\d+)$");

    private ProxyConfig config;
    private boolean running;
    private String lastError;

    private ProxyManager() {}

    public static ProxyManager getInstance() {
        return INSTANCE;
    }

    public boolean start(ProxyConfig config) {
        if (config == null || config.getHost().isEmpty()) {
            lastError = "Config is null or host is empty";
            return false;
        }
        stop();
        this.config = config;
        boolean ok = ProxyServer.start(
            config.getType(),
            config.getHost(),
            config.getPort(),
            config.getTargetHost(),
            config.getTargetPort(),
            config.getUsername(),
            config.getPassword()
        );
        if (ok) {
            running = true;
            lastError = null;
        } else {
            lastError = "Proxy server failed to start";
        }
        return ok;
    }

    public void stop() {
        if (running) {
            ProxyServer.stop();
            running = false;
        }
    }

    public boolean restart() {
        if (config == null) return false;
        return start(config);
    }

    public int getLocalPort() {
        return ProxyServer.getLocalPort();
    }

    public boolean isRunning() {
        return running && ProxyServer.isRunning();
    }

    public String getLastError() {
        return lastError;
    }

    public ProxyConfig getConfig() {
        return config;
    }

    public InetSocketAddress connect(ProxyConfig targetConfig) {
        if (targetConfig == null || targetConfig.getTargetHost().isEmpty()) {
            lastError = "Target config is null or empty";
            return null;
        }
        ProxyConfig fullConfig = config != null
            ? config.copy().targetHost(targetConfig.getTargetHost())
                       .targetPort(targetConfig.getTargetPort()).build()
            : targetConfig;
        if (!start(fullConfig)) return null;
        return new InetSocketAddress("127.0.0.1", getLocalPort());
    }

    public InetSocketAddress connect(String targetHost, int targetPort) {
        return connect(ProxyConfig.builder()
            .targetHost(targetHost).targetPort(targetPort)
            .build());
    }

    public InetSocketAddress connect(InetSocketAddress original) {
        return connect(original.getHostString(), original.getPort());
    }

    public static ProxyConfig parseUrl(String url) {
        Matcher m = PROXY_URL_PATTERN.matcher(url.trim());
        if (!m.matches()) {
            try {
                URI uri = new URI(url);
                String scheme = uri.getScheme();
                if (scheme == null) scheme = "socks5";
                String userInfo = uri.getUserInfo();
                String host = uri.getHost();
                int port = uri.getPort();
                if (host == null || port <= 0) return null;
                ProxyConfig.Builder builder = ProxyConfig.builder()
                    .type(scheme.toUpperCase()).host(host).port(port);
                if (userInfo != null && !userInfo.isEmpty()) {
                    String[] parts = userInfo.split(":", 2);
                    builder.username(parts[0]);
                    if (parts.length > 1) builder.password(parts[1]);
                }
                return builder.build();
            } catch (URISyntaxException e) {
                return null;
            }
        }
        String type = m.group(1).toUpperCase();
        String username = m.group(2);
        String password = m.group(3);
        String host = m.group(4);
        int port = Integer.parseInt(m.group(5));
        ProxyConfig.Builder builder = ProxyConfig.builder()
            .type(type).host(host).port(port);
        if (username != null) {
            builder.username(username);
            if (password != null) builder.password(password);
        }
        return builder.build();
    }
}
