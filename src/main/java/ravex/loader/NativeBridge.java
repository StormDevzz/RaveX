package ravex.loader;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;

public class NativeBridge {
    private static boolean loaded = false;
    private static String loadError;

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static String getLibName() {
        return isWindows() ? "ravex_loader.dll" : "libravex_loader.so";
    }

    private static String getTempSuffix() {
        return isWindows() ? ".dll" : ".so";
    }

    static {
        try {
            System.loadLibrary("ravex_loader");
            loaded = true;
        } catch (UnsatisfiedLinkError e) {
            loadError = e.getMessage();
            try {
                String libName = getLibName();
                java.io.InputStream is = NativeBridge.class.getResourceAsStream(
                    "/assets/ravex/natives/" + libName);
                if (is != null) {
                    java.nio.file.Path tmp = java.nio.file.Files.createTempFile("ravex_loader", getTempSuffix());
                    java.nio.file.Files.copy(is, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    if (verifySha256(tmp.toFile(), getExpectedLoaderHash())) {
                        System.load(tmp.toAbsolutePath().toString());
                        tmp.toFile().deleteOnExit();
                        loaded = true;
                        loadError = null;
                    }
                }
            } catch (Throwable e2) {
                loadError = e2.getMessage();
            }
        }
    }

    public static synchronized boolean load() {
        if (loaded) return true;
        try {
            String libName = getLibName();


            java.io.File localDev = new java.io.File("src/main/resources/assets/ravex/natives/" + libName);
            if (localDev.exists() && localDev.length() > 0) {
                System.load(localDev.getAbsolutePath());
                loaded = true;
                loadError = null;
                return true;
            }


            java.io.File cacheDir = new java.io.File(System.getProperty("user.home"), ".ravex/natives");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            java.io.File cachedFile = new java.io.File(cacheDir, libName);
            String expectedHash = getExpectedLoaderHash();

            if (cachedFile.exists() && cachedFile.length() > 0) {
                if (verifySha256(cachedFile, expectedHash)) {
                    System.load(cachedFile.getAbsolutePath());
                    loaded = true;
                    loadError = null;
                    return true;
                }
                cachedFile.delete();
            }

            java.io.File buildNative = new java.io.File("build/native/loader/" + libName);
            if (!buildNative.exists()) {
                buildNative = new java.io.File("build/native/launcher/windows/" + libName);
            }
            if (buildNative.exists() && buildNative.length() > 0) {
                try {
                    java.nio.file.Files.copy(buildNative.toPath(), cachedFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    if (verifySha256(cachedFile, expectedHash)) {
                        System.load(cachedFile.getAbsolutePath());
                        loaded = true;
                        loadError = null;
                        return true;
                    }
                    cachedFile.delete();
                } catch (Exception ignored) {}
            }

            try {
                java.io.InputStream is = NativeBridge.class.getResourceAsStream("/assets/ravex/natives/" + libName);
                if (is != null) {
                    java.nio.file.Files.copy(is, cachedFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    cachedFile.setExecutable(true);
                    if (verifySha256(cachedFile, expectedHash)) {
                        System.load(cachedFile.getAbsolutePath());
                        loaded = true;
                        loadError = null;
                        return true;
                    }
                    cachedFile.delete();
                }
            } catch (Throwable ignored) {}

            String remoteUrl = "https://github.com/StormDevzz/RaveX/releases/latest/download/" + libName;
            System.out.println("[RaveX-Loader] Downloading " + libName + " from " + remoteUrl);
            RaveXLoader.updateWindowStatus("Downloading " + libName + "...", 10);

            java.net.URL url = new java.net.URL(remoteUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                java.io.File tempDownload = new java.io.File(cacheDir, libName + ".tmp");
                try (java.io.InputStream in = conn.getInputStream();
                     java.io.FileOutputStream out = new java.io.FileOutputStream(tempDownload)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                if (!tempDownload.renameTo(cachedFile)) {
                    java.nio.file.Files.copy(tempDownload.toPath(), cachedFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    tempDownload.delete();
                }
                if (verifySha256(cachedFile, expectedHash)) {
                    System.load(cachedFile.getAbsolutePath());
                    loaded = true;
                    loadError = null;
                    return true;
                }
                cachedFile.delete();
            } else {
                throw new java.io.IOException("HTTP response code: " + responseCode);
            }
        } catch (Throwable e3) {
            loadError = e3.getMessage();
            System.err.println("[RaveX-Loader] Failed to download/load remote native loader: " + e3.getMessage() + ". If you see this, please report it on our Discord: https://discord.gg/n9HPbgN7S");


            try {
                String libName = getLibName();
                String remoteUrl = "https://github.com/StormDevzz/RaveX/releases/latest/download/" + libName;
                java.net.URL url = new java.net.URL(remoteUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                if (conn.getResponseCode() == 200) {
                    java.nio.file.Path tmp = java.nio.file.Files.createTempFile("ravex_loader", getTempSuffix());
                    try (java.io.InputStream in = conn.getInputStream()) {
                        java.nio.file.Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                    if (verifySha256(tmp.toFile(), getExpectedLoaderHash())) {
                        System.load(tmp.toAbsolutePath().toString());
                        tmp.toFile().deleteOnExit();
                        loaded = true;
                        loadError = null;
                        return true;
                    }
                }
            } catch (Throwable e4) {
                loadError = e4.getMessage();
            }
        }
        return loaded;
    }

    public static boolean isLoaded() { return loaded; }
    public static String getLoadError() { return loadError; }

    private static String sha256Hex(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] data = Files.readAllBytes(file.toPath());
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean verifySha256(File file, String expected) {
        String actual = sha256Hex(file);
        return actual != null && actual.equals(expected);
    }

    private static final String LOADER_SHA256_LINUX = "91df4b7a1f307a7467a31e89ee8153d262cda4b0ca3bad8d0c482e56c13de505";
    private static final String LOADER_SHA256_WINDOWS = "84f9b122fa98ab81bcb2cc498dc4f3e29f56d7b683108dd3f6f1af9ecb2fd092";

    private static String getExpectedLoaderHash() {
        return isWindows() ? LOADER_SHA256_WINDOWS : LOADER_SHA256_LINUX;
    }

    public static native String runChecks();
    public static native String optimize();
    public static native int trimMemory();
    public static native int setHighPriority();
    public static native String getSystemInfo();
    public static native int getScore();
}
