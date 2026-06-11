package ravex.utility.misc;

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

    private static java.io.File getOrDownloadLibrary(String libName, String tempPrefix, String tempSuffix) {
        java.io.File cacheDir = new java.io.File(System.getProperty("user.home"), ".ravex/natives");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        java.io.File cachedFile = new java.io.File(cacheDir, libName);
        if (cachedFile.exists() && cachedFile.length() > 0) {
            return cachedFile;
        }

        String remoteUrl = "https://raw.githubusercontent.com/StormDevzz/RaveX/main/src/main/resources/assets/ravex/natives/" + libName;
        System.out.println("[RaveX] Downloading remote native: " + libName + " from " + remoteUrl);
        try {
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
                if (tempDownload.renameTo(cachedFile)) {
                    return cachedFile;
                } else {
                    java.nio.file.Files.copy(tempDownload.toPath(), cachedFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    tempDownload.delete();
                    return cachedFile;
                }
            } else {
                System.err.println("[RaveX] Failed to download native " + libName + ": HTTP code " + responseCode);
            }
        } catch (Throwable t) {
            System.err.println("[RaveX] Error downloading native " + libName + ": " + t.getMessage());
        }

        // Direct download to temp file fallback
        try {
            String remoteUrlFallback = "https://raw.githubusercontent.com/StormDevzz/RaveX/main/src/main/resources/assets/ravex/natives/" + libName;
            java.net.URL url = new java.net.URL(remoteUrlFallback);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            if (conn.getResponseCode() == 200) {
                java.io.File tempFile = java.io.File.createTempFile(tempPrefix, tempSuffix);
                tempFile.deleteOnExit();
                try (java.io.InputStream in = conn.getInputStream();
                     java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile)) {
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

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        try {
            System.loadLibrary("ravex_jni");
            nativeAvailable = true;
        } catch (UnsatisfiedLinkError e) {
            String libName = getLibName();
            try {
                java.io.InputStream in = NativeLoader.class.getResourceAsStream("/assets/ravex/natives/" + libName);
                if (in != null) {
                    java.io.File tempFile = java.io.File.createTempFile(getTempPrefix(), getTempSuffix());
                    tempFile.deleteOnExit();
                    try (java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                    System.load(tempFile.getAbsolutePath());
                    nativeAvailable = true;
                } else {
                    java.io.File file = getOrDownloadLibrary(libName, getTempPrefix(), getTempSuffix());
                    if (file != null) {
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
                java.io.InputStream in = NativeLoader.class.getResourceAsStream("/assets/ravex/natives/" + libName);
                if (in != null) {
                    java.io.File tempFile = java.io.File.createTempFile(tempPrefix, tempSuffix);
                    tempFile.deleteOnExit();
                    try (java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                    System.load(tempFile.getAbsolutePath());
                    return true;
                } else {
                    java.io.File file = getOrDownloadLibrary(libName, tempPrefix, tempSuffix);
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
