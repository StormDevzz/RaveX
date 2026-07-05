package ravex.proxy;

public class ProxyConfig {
    private final String type;
    private final String host;
    private final int port;
    private final String targetHost;
    private final int targetPort;
    private final String username;
    private final String password;

    private ProxyConfig(Builder builder) {
        this.type = builder.type;
        this.host = builder.host;
        this.port = builder.port;
        this.targetHost = builder.targetHost;
        this.targetPort = builder.targetPort;
        this.username = builder.username;
        this.password = builder.password;
    }

    public String getType() { return type; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getTargetHost() { return targetHost; }
    public int getTargetPort() { return targetPort; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public boolean hasAuth() { return !username.isEmpty(); }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type = "SOCKS5";
        private String host = "127.0.0.1";
        private int port = 1080;
        private String targetHost = "";
        private int targetPort = 25565;
        private String username = "";
        private String password = "";

        public Builder type(String type) { this.type = type; return this; }
        public Builder host(String host) { this.host = host; return this; }
        public Builder port(int port) { this.port = port; return this; }
        public Builder targetHost(String targetHost) { this.targetHost = targetHost; return this; }
        public Builder targetPort(int targetPort) { this.targetPort = targetPort; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder password(String password) { this.password = password; return this; }

        public ProxyConfig build() {
            return new ProxyConfig(this);
        }
    }

    public Builder copy() {
        return new Builder()
            .type(type).host(host).port(port)
            .targetHost(targetHost).targetPort(targetPort)
            .username(username).password(password);
    }
}
