package ravex.proxy;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

public class Shadowsocks {
    private static final int SALT_LEN = 16;
    private static final int KEY_LEN = 32;
    private static final int NONCE_LEN = 12;
    private static final int TAG_LEN = 16;

    private final byte[] sessionKey;

    public Shadowsocks(String password) {
        this.sessionKey = evpBytesToKey(password, KEY_LEN);
    }

    public void handshake(Socket sock, String targetHost, int targetPort) throws Exception {
        byte[] salt = new byte[SALT_LEN];
        new SecureRandom().nextBytes(salt);

        byte[] subKey = hkdfSha1(sessionKey, salt);

        byte[] addr = buildAddress(targetHost, targetPort);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(subKey, "AES"),
                new GCMParameterSpec(TAG_LEN * 8, new byte[NONCE_LEN]));

        byte[] encrypted = cipher.doFinal(addr);

        OutputStream out = sock.getOutputStream();
        out.write(salt);
        out.write(encrypted);
        out.flush();
    }

    public CipherStreams wrap(Socket sock, boolean isClient) throws Exception {
        InputStream in = sock.getInputStream();
        OutputStream out = sock.getOutputStream();

        byte[] salt;
        byte[] subKey;

        if (isClient) {
            salt = new byte[SALT_LEN];
            new SecureRandom().nextBytes(salt);
            subKey = hkdfSha1(sessionKey, salt);

            byte[] addr = buildAddress(sock.getInetAddress().getHostAddress(), sock.getPort());

            Cipher enc = Cipher.getInstance("AES/GCM/NoPadding");
            enc.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(subKey, "AES"),
                    new GCMParameterSpec(TAG_LEN * 8, new byte[NONCE_LEN]));
            byte[] encrypted = enc.doFinal(addr);

            out.write(salt);
            out.write(encrypted);
            out.flush();

            return new CipherStreams(in, out, subKey, 1, 1);
        } else {
            salt = readExact(in, SALT_LEN);
            subKey = hkdfSha1(sessionKey, salt);

            Cipher dec = Cipher.getInstance("AES/GCM/NoPadding");
            dec.init(Cipher.DECRYPT_MODE, new SecretKeySpec(subKey, "AES"),
                    new GCMParameterSpec(TAG_LEN * 8, new byte[NONCE_LEN]));

            int tagLen = readExact(in, 1)[0] & 0xFF;
            byte[] addrTag = readExact(in, tagLen - 1 + SALT_LEN);
            dec.update(addrTag, 0, addrTag.length - TAG_LEN);
            dec.doFinal(addrTag, addrTag.length - TAG_LEN, TAG_LEN);

            return new CipherStreams(in, out, subKey, 1, 1);
        }
    }

    public void relay(Socket clientSock, Socket proxySock, String targetHost, int targetPort) throws Exception {
        handshake(proxySock, targetHost, targetPort);
        relayData(clientSock, proxySock);
    }

    private static void relayData(Socket client, Socket proxy) throws IOException {
        byte[] buf = new byte[8192];
        InputStream ci = client.getInputStream();
        OutputStream co = client.getOutputStream();
        InputStream pi = proxy.getInputStream();
        OutputStream po = proxy.getOutputStream();

        Thread t1 = new Thread(() -> {
            try { transfer(ci, po, buf.clone()); } catch (IOException ignored) {}
        }, "ss-c2p");
        Thread t2 = new Thread(() -> {
            try { transfer(pi, co, buf.clone()); } catch (IOException ignored) {}
        }, "ss-p2c");
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

    private static byte[] buildAddress(String host, int port) throws IOException {
        boolean isIP = !host.matches(".*[^0-9.].*") && host.chars().filter(c -> c == '.').count() == 3;
        byte[] addrBytes;
        if (isIP) {
            String[] parts = host.split("\\.");
            addrBytes = new byte[4];
            for (int i = 0; i < 4; i++) addrBytes[i] = (byte) Integer.parseInt(parts[i]);
            byte[] buf = new byte[1 + 4 + 2];
            buf[0] = 1;
            System.arraycopy(addrBytes, 0, buf, 1, 4);
            buf[5] = (byte) (port >> 8);
            buf[6] = (byte) port;
            return buf;
        } else {
            byte[] nameBytes = host.getBytes("UTF-8");
            byte[] buf = new byte[1 + 1 + nameBytes.length + 2];
            buf[0] = 3;
            buf[1] = (byte) nameBytes.length;
            System.arraycopy(nameBytes, 0, buf, 2, nameBytes.length);
            buf[2 + nameBytes.length] = (byte) (port >> 8);
            buf[3 + nameBytes.length] = (byte) port;
            return buf;
        }
    }

    private static byte[] evpBytesToKey(String password, int keyLen) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] key = new byte[keyLen];
            byte[] hash = null;
            int offset = 0;
            while (offset < keyLen) {
                if (hash != null) md.update(hash);
                md.update(password.getBytes("UTF-8"));
                hash = md.digest();
                int copy = Math.min(keyLen - offset, hash.length);
                System.arraycopy(hash, 0, key, offset, copy);
                offset += copy;
            }
            return key;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] hkdfSha1(byte[] key, byte[] salt) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(salt, "SHA1"));
            byte[] prk = mac.doFinal(key);

            mac.init(new SecretKeySpec(prk, "SHA1"));
            byte[] result = new byte[KEY_LEN];
            byte[] t = new byte[0];
            int offset = 0;
            for (int i = 1; offset < KEY_LEN; i++) {
                mac.update(t);
                mac.update((byte) i);
                t = mac.doFinal();
                int copy = Math.min(KEY_LEN - offset, t.length);
                System.arraycopy(t, 0, result, offset, copy);
                offset += copy;
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    public static class CipherStreams {
        public final InputStream input;
        public final OutputStream output;
        public final byte[] subKey;
        public final long encNonce;
        public final long decNonce;

        public CipherStreams(InputStream input, OutputStream output,
                              byte[] subKey, long encNonce, long decNonce) {
            this.input = input;
            this.output = output;
            this.subKey = subKey;
            this.encNonce = encNonce;
            this.decNonce = decNonce;
        }
    }
}
