package ravex.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ProxyServer {
    private static ServerSocket serverSocket;
    private static ExecutorService pool;
    private static int localPort = -1;
    private static volatile boolean running = false;

    public static synchronized boolean start(String proxyType, String proxyHost, int proxyPort,
                                              String targetHost, int targetPort,
                                              String username, String password) {
        stop();

        try {
            serverSocket = new ServerSocket(0);
            localPort = serverSocket.getLocalPort();
            running = true;
            pool = Executors.newCachedThreadPool();

            String type = proxyType.toUpperCase();
            pool.submit(() -> {
                while (running && !serverSocket.isClosed()) {
                    try {
                        Socket client = serverSocket.accept();
                        pool.submit(() -> handle(client, type, proxyHost, proxyPort,
                                                  targetHost, targetPort, username, password));
                    } catch (IOException ignored) {
                    }
                }
            });

            return true;
        } catch (IOException e) {
            System.err.println("[ProxyServer] Failed to start: " + e.getMessage());
            stop();
            return false;
        }
    }

    public static synchronized void stop() {
        running = false;
        if (serverSocket != null) {
            try { serverSocket.close(); } catch (IOException ignored) {}
            serverSocket = null;
        }
        if (pool != null) {
            pool.shutdownNow();
            try { pool.awaitTermination(1, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
            pool = null;
        }
        localPort = -1;
    }

    public static synchronized int getLocalPort() { return localPort; }
    public static synchronized boolean isRunning() { return running; }

    private static void handle(Socket client, String type, String proxyHost, int proxyPort,
                                String targetHost, int targetPort, String username, String password) {
        try (Socket clientSock = client;
             Socket proxySock = new Socket()) {

            proxySock.connect(new InetSocketAddress(proxyHost, proxyPort), 5000);

            switch (type) {
                case "SOCKS5" -> socks5Handshake(proxySock, targetHost, targetPort, username, password);
                case "SOCKS4" -> socks4Handshake(proxySock, targetHost, targetPort, username);
                case "HTTP" -> httpHandshake(proxySock, targetHost, targetPort, username, password);
                case "SHADOWSOCKS" -> {
                    Shadowsocks ss = new Shadowsocks(password);
                    ss.relay(clientSock, proxySock, targetHost, targetPort);
                    return;
                }
                default -> {
                    return;
                }
            }

            pipe(clientSock, proxySock);
        } catch (Exception e) {
            System.err.println("[ProxyServer] " + e.getMessage());
        }
    }

    private static void socks5Handshake(Socket sock, String host, int port,
                                         String user, String pass) throws IOException {
        InputStream in = sock.getInputStream();
        OutputStream out = sock.getOutputStream();

        if (user.isEmpty()) {
            out.write(new byte[]{5, 1, 0});
        } else {
            out.write(new byte[]{5, 1, 2});
        }
        out.flush();

        byte[] hdr = readExact(in, 2);
        if (hdr[1] == 2) {
            byte[] auth = new byte[3 + user.length() + pass.length()];
            auth[0] = 1;
            auth[1] = (byte) user.length();
            System.arraycopy(user.getBytes(), 0, auth, 2, user.length());
            auth[2 + user.length()] = (byte) pass.length();
            System.arraycopy(pass.getBytes(), 0, auth, 3 + user.length(), pass.length());
            out.write(auth);
            out.flush();
            byte[] resp = readExact(in, 2);
            if (resp[1] != 0) throw new IOException("SOCKS5 auth failed");
        }

        byte[] hostBytes = host.getBytes();
        byte[] req = new byte[7 + hostBytes.length];
        req[0] = 5; req[1] = 1; req[2] = 0; req[3] = 3;
        req[4] = (byte) hostBytes.length;
        System.arraycopy(hostBytes, 0, req, 5, hostBytes.length);
        req[5 + hostBytes.length] = (byte) (port >> 8);
        req[6 + hostBytes.length] = (byte) port;
        out.write(req);
        out.flush();

        byte[] resp = readExact(in, 4);
        if (resp[1] != 0) throw new IOException("SOCKS5 connect failed: " + resp[1]);

        int addrLen = switch (resp[3]) {
            case 1 -> 4;
            case 4 -> 16;
            case 3 -> in.read() + 1;
            default -> 0;
        };
        if (addrLen > 0) readExact(in, addrLen + 2);
    }

    private static void socks4Handshake(Socket sock, String host, int port,
                                         String user) throws IOException {
        InputStream in = sock.getInputStream();
        OutputStream out = sock.getOutputStream();

        byte[] req;
        boolean socks4a = !host.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");

        if (socks4a) {
            req = new byte[9 + user.length() + 1 + host.length() + 1];
            req[0] = 4; req[1] = 1;
            req[2] = (byte) (port >> 8); req[3] = (byte) port;
            req[4] = 0; req[5] = 0; req[6] = 0; req[7] = 1;
            int off = 8;
            if (!user.isEmpty()) {
                System.arraycopy(user.getBytes(), 0, req, off, user.length());
                off += user.length();
            }
            req[off++] = 0;
            System.arraycopy(host.getBytes(), 0, req, off, host.length());
            off += host.length();
            req[off] = 0;
        } else {
            String[] parts = host.split("\\.");
            req = new byte[8 + user.length() + 1];
            req[0] = 4; req[1] = 1;
            req[2] = (byte) (port >> 8); req[3] = (byte) port;
            for (int i = 0; i < 4; i++) req[4 + i] = (byte) Integer.parseInt(parts[i]);
            int off = 8;
            if (!user.isEmpty()) {
                System.arraycopy(user.getBytes(), 0, req, off, user.length());
                off += user.length();
            }
            req[off] = 0;
        }

        out.write(req);
        out.flush();

        byte[] resp = readExact(in, 8);
        if (resp[1] != 0x5a) throw new IOException("SOCKS4 connect failed");
    }

    private static void httpHandshake(Socket sock, String host, int port,
                                       String user, String pass) throws IOException {
        OutputStream out = sock.getOutputStream();
        StringBuilder req = new StringBuilder();
        req.append("CONNECT ").append(host).append(":").append(port).append(" HTTP/1.1\r\n");
        req.append("Host: ").append(host).append(":").append(port).append("\r\n");
        if (!user.isEmpty()) {
            String encoded = Base64.getEncoder().encodeToString((user + ":" + pass).getBytes());
            req.append("Proxy-Authorization: Basic ").append(encoded).append("\r\n");
        }
        req.append("\r\n");
        out.write(req.toString().getBytes());
        out.flush();

        byte[] buf = new byte[4096];
        int n = sock.getInputStream().read(buf);
        String resp = new String(buf, 0, n);
        if (!resp.contains("200 Connection Established"))
            throw new IOException("HTTP proxy connect failed");
    }

    private static void pipe(Socket client, Socket proxy) throws IOException {
        byte[] buf = new byte[8192];
        InputStream ci = client.getInputStream();
        OutputStream co = client.getOutputStream();
        InputStream pi = proxy.getInputStream();
        OutputStream po = proxy.getOutputStream();

        Thread t1 = new Thread(() -> {
            try { transfer(ci, po, buf.clone()); } catch (IOException ignored) {}
        }, "proxy-c2p");
        Thread t2 = new Thread(() -> {
            try { transfer(pi, co, buf.clone()); } catch (IOException ignored) {}
        }, "proxy-p2c");
        t1.setDaemon(true);
        t2.setDaemon(true);
        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException ignored) {}
    }

    private static void transfer(InputStream in, OutputStream out, byte[] buf) throws IOException {
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
            out.flush();
        }
    }

    private static byte[] readExact(InputStream in, int len) throws IOException {
        byte[] buf = new byte[len];
        int off = 0;
        while (off < len) {
            int n = in.read(buf, off, len - off);
            if (n == -1) throw new IOException("Connection closed");
            off += n;
        }
        return buf;
    }
}
