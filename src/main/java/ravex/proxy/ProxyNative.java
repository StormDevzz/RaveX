package ravex.proxy;

import ravex.utility.nativelib.NativeLibrary;

public class ProxyNative {
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_proxy");
    private static int localPort = -1;

    static {
        NATIVE.load();
    }

    public static native int startLocalProxy(String proxyType, String proxyHost, int proxyPort,
                                              String targetHost, int targetPort,
                                              String username, String password,
                                              int listenPort);
    public static native void stopLocalProxy();

    public static synchronized boolean start(String proxyType, String proxyHost, int proxyPort,
                                              String targetHost, int targetPort,
                                              String username, String password) {
        if (!NATIVE.isLoaded()) {
            System.err.println("[ProxyNative] Failed to load ravex_proxy");
            return false;
        }

        stop();
        int port = startLocalProxy(proxyType, proxyHost, proxyPort,
                                    targetHost, targetPort,
                                    username, password, 0);
        if (port <= 0) return false;
        localPort = port;
        return true;
    }

    public static synchronized void stop() {
        if (localPort > 0) {
            stopLocalProxy();
            localPort = -1;
        }
    }

    public static synchronized int getLocalPort() { return localPort; }
    public static synchronized boolean isRunning() { return localPort > 0; }
}
