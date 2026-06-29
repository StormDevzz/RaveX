package ravex.utility.misc;

import java.io.*;
import java.net.URL;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class NativeLoader {
    private static boolean loaded = false;
    private static boolean nativeAvailable = false;

    private static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }

    private static String getLibName() {
        return isWindows() ? "ravex_jni.dll" : "libravex_jni.so";
    }

    private static String getTempPrefix() {
        return isWindows() ? "ravex_jni" : "libravex_jni";
    }

    private static String getTempSuffix() {
        return isWindows() ? ".dll" : ".so";
    }

    private static File getCacheDir() {
        return new File(System.getProperty("user.home"), ".ravex/natives");
    }

    private static void extractAllFromJar(File dir) {
        String prefix = "assets/ravex/natives/";
        String suffix = isWindows() ? ".dll" : ".so";
        try {
            java.net.URL jarUrl = NativeLoader.class.getProtectionDomain().getCodeSource().getLocation();
            if (jarUrl == null) return;
            String jarPath = jarUrl.getPath();
            if (jarPath == null || !jarPath.endsWith(".jar")) return;
            try (java.util.jar.JarFile jar = new java.util.jar.JarFile(new File(jarPath))) {
                java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    java.util.jar.JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(prefix) && name.endsWith(suffix) && !entry.isDirectory()) {
                        String fileName = name.substring(prefix.length());
                        File outFile = new File(dir, fileName);
                        if (outFile.exists() && outFile.length() == entry.getSize()) continue;
                        try (InputStream in = jar.getInputStream(entry);
                             FileOutputStream out = new FileOutputStream(outFile)) {
                            byte[] buf = new byte[8192];
                            int read;
                            while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
                        }
                        outFile.setExecutable(true);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private static File getOrDownloadLibrary(String libName, String tempPrefix, String tempSuffix) {
        File cacheDir = getCacheDir();
        if (!cacheDir.exists()) cacheDir.mkdirs();

        
        extractAllFromJar(cacheDir);

        File cachedFile = new File(cacheDir, libName);
        if (cachedFile.exists() && cachedFile.length() > 0) {
            return cachedFile;
        }

        String remoteUrl = "https://raw.githubusercontent.com/StormDevzz/RaveX/main/src/main/resources/assets/ravex/natives/" + libName;
        System.out.println("[RaveX] Downloading remote native: " + libName + " from " + remoteUrl);
        try {
            URL url = new URL(remoteUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                File tempDownload = new File(cacheDir, libName + ".tmp");
                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(tempDownload)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                if (tempDownload.renameTo(cachedFile)) {
                    return cachedFile;
                } else {
                    Files.copy(tempDownload.toPath(), cachedFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    tempDownload.delete();
                    return cachedFile;
                }
            } else {
                System.err.println("[RaveX] Failed to download native " + libName + ": HTTP code " + responseCode);
            }
        } catch (Throwable t) {
            System.err.println("[RaveX] Error downloading native " + libName + ": " + t.getMessage());
        }

        try {
            URL url = new URL(remoteUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            if (conn.getResponseCode() == 200) {
                File tempFile = File.createTempFile(tempPrefix, tempSuffix);
                tempFile.deleteOnExit();
                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                return tempFile;
            }
        } catch (Throwable t) {
            System.err.println("[RaveX] Fallback download failed: " + t.getMessage());
        }

        return null;
    }

    private static void loadDependencies(File dir) {
        
        String[] deps = {"libravex_optimize.so", "libravex_manager.so", "libravex_github_tools.so"};
        for (String dep : deps) {
            File f = new File(dir, dep);
            if (f.exists()) {
                try {
                    System.load(f.getAbsolutePath());
                } catch (Throwable ignored) {}
            }
        }
    }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        try {
            System.loadLibrary("ravex_jni");
            nativeAvailable = true;
        } catch (UnsatisfiedLinkError e) {
            String libName = getLibName();
            try {
                File cacheDir = getCacheDir();
                if (!cacheDir.exists()) cacheDir.mkdirs();
                extractAllFromJar(cacheDir);

                File cached = new File(cacheDir, libName);
                if (cached.exists() && cached.length() > 0) {
                    loadDependencies(cacheDir);
                    System.load(cached.getAbsolutePath());
                    nativeAvailable = true;
                    return;
                }

                InputStream in = NativeLoader.class.getResourceAsStream("/assets/ravex/natives/" + libName);
                if (in != null) {
                    File tempFile = File.createTempFile(getTempPrefix(), getTempSuffix());
                    tempFile.deleteOnExit();
                    try (FileOutputStream out = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                    System.load(tempFile.getAbsolutePath());
                    nativeAvailable = true;
                } else {
                    File file = getOrDownloadLibrary(libName, getTempPrefix(), getTempSuffix());
                    if (file != null) {
                        loadDependencies(file.getParentFile());
                        System.load(file.getAbsolutePath());
                        nativeAvailable = true;
                    } else {
                        System.err.println("[RaveX] JNI native library could not be loaded/downloaded: " + libName);
                    }
                }
            } catch (Throwable ex) {
                System.err.println("[RaveX] WARNING: Failed to dynamically load native library: " + ex.getMessage());
            }
        }
    }

    public static synchronized boolean loadLibrary(String name) {
        try {
            System.loadLibrary(name);
            return true;
        } catch (UnsatisfiedLinkError e) {
            String os = System.getProperty("os.name").toLowerCase();
            boolean isWin = os.contains("win");
            String libName = isWin ? name + ".dll" : "lib" + name + ".so";
            String tempPrefix = isWin ? name : "lib" + name;
            String tempSuffix = isWin ? ".dll" : ".so";
            try {
                File cacheDir = getCacheDir();
                if (!cacheDir.exists()) cacheDir.mkdirs();
                extractAllFromJar(cacheDir);

                File cached = new File(cacheDir, libName);
                if (cached.exists() && cached.length() > 0) {
                    System.load(cached.getAbsolutePath());
                    return true;
                }

                InputStream in = NativeLoader.class.getResourceAsStream("/assets/ravex/natives/" + libName);
                if (in != null) {
                    File tempFile = File.createTempFile(tempPrefix, tempSuffix);
                    tempFile.deleteOnExit();
                    try (FileOutputStream out = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                    System.load(tempFile.getAbsolutePath());
                    return true;
                } else {
                    File file = getOrDownloadLibrary(libName, tempPrefix, tempSuffix);
                    if (file != null) {
                        System.load(file.getAbsolutePath());
                        return true;
                    }
                }
            } catch (Throwable ex) {
                System.err.println("[RaveX] Failed to extract and load native library " + name + ": " + ex.getMessage());
            }
        }
        return false;
    }

    public static boolean isNativeAvailable() {
        return nativeAvailable;
    }
}
