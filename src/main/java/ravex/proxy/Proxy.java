package ravex.proxy;

public class Proxy {
    private static boolean enabled = false;
    private static String type = "SOCKS5";
    private static String host = "127.0.0.1";
    private static int port = 1080;
    private static boolean auth = false;
    private static String username = "";
    private static String password = "";

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean v) { enabled = v; }
    public static String getType() { return type; }
    public static void setType(String v) { type = v; }
    public static String getHost() { return host; }
    public static void setHost(String v) { host = v; }
    public static int getPort() { return port; }
    public static void setPort(int v) { port = v; }
    public static boolean hasAuth() { return auth; }
    public static void setAuth(boolean v) { auth = v; }
    public static String getUsername() { return username; }
    public static void setUsername(String v) { username = v; }
    public static String getPassword() { return password; }
    public static void setPassword(String v) { password = v; }
    public static void toggle() { enabled = !enabled; }
}
