package ravex.loader;

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
                    System.load(tmp.toAbsolutePath().toString());
                    tmp.toFile().deleteOnExit();
                    loaded = true;
                    loadError = null;
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
            boolean exists = cachedFile.exists() && cachedFile.length() > 0;

            if (!exists) {
                
                java.io.File buildNative = new java.io.File("build/native/loader/" + libName);
                if (!buildNative.exists()) {
                    buildNative = new java.io.File("build/native/launcher/windows/" + libName);
                }
                if (buildNative.exists() && buildNative.length() > 0) {
                    try {
                        java.nio.file.Files.copy(buildNative.toPath(), cachedFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        exists = true;
                    } catch (Exception ignored) {}
                }
            }

            if (!exists) {
                
                String remoteUrl = "https:
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
                    exists = true;
                } else {
                    throw new java.io.IOException("HTTP response code: " + responseCode);
                }
            }

            if (exists) {
                System.load(cachedFile.getAbsolutePath());
                loaded = true;
                loadError = null;
                return true;
            }
        } catch (Throwable e3) {
            loadError = e3.getMessage();
            System.err.println("[RaveX-Loader] Failed to download/load remote native loader: " + e3.getMessage());

            
            try {
                String libName = getLibName();
                String remoteUrl = "https:
                java.net.URL url = new java.net.URL(remoteUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                if (conn.getResponseCode() == 200) {
                    java.nio.file.Path tmp = java.nio.file.Files.createTempFile("ravex_loader", getTempSuffix());
                    try (java.io.InputStream in = conn.getInputStream()) {
                        java.nio.file.Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                    System.load(tmp.toAbsolutePath().toString());
                    tmp.toFile().deleteOnExit();
                    loaded = true;
                    loadError = null;
                    return true;
                }
            } catch (Throwable e4) {
                loadError = e4.getMessage();
            }
        }
        return loaded;
    }

    public static boolean isLoaded() { return loaded; }
    public static String getLoadError() { return loadError; }

    public static native String runChecks();
    public static native String optimize();
    public static native int trimMemory();
    public static native int setHighPriority();
    public static native String getSystemInfo();
    public static native int getScore();
}
