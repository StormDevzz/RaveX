package ravex.loader;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class AssetDownloader {
    private static final String ASSETS_BASE = "src/main/resources/assets/ravex";
    private static final String REMOTE_BASE = "https://raw.githubusercontent.com/StormDevzz/RaveX/main/";

    public void downloadRequiredAssets() {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWin = os.contains("win");

        List<String> assetList = new ArrayList<>(List.of(
            "font/sf_medium.ttf",
            "font/sf_bold.ttf",
            "font/comfortaa.ttf"
        ));

        String loaderLib = isWin ? "ravex_loader.dll" : "libravex_loader.so";
        String jniLib = isWin ? "ravex_jni.dll" : "libravex_jni.so";
        assetList.add("natives/" + loaderLib);
        assetList.add("natives/" + jniLib);

        File localSrc = new File(ASSETS_BASE);
        File buildNative = new File("build/native");
        File cacheDir = new File(System.getProperty("user.home"), ".ravex");

        for (int i = 0; i < assetList.size(); i++) {
            String assetPath = assetList.get(i);
            File cachedFile = new File(cacheDir, assetPath);

            if (cachedFile.exists() && cachedFile.length() > 0) continue;

            File localFile = new File(localSrc, assetPath);
            if (!localFile.exists() || localFile.length() == 0) {
                localFile = new File(buildNative, assetPath.replace("natives/", ""));
                if (!localFile.exists() || localFile.length() == 0) {
                    localFile = null;
                }
            }

            if (localFile != null && localFile.exists() && localFile.length() > 0) {
                File parent = cachedFile.getParentFile();
                if (!parent.exists()) parent.mkdirs();
                try {
                    Files.copy(localFile.toPath(), cachedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("[RaveX-Loader] Cached local asset: " + assetPath);
                    continue;
                } catch (Exception ignored) {}
            }

            File parent = cachedFile.getParentFile();
            if (!parent.exists()) parent.mkdirs();

            String remoteUrl = REMOTE_BASE + assetPath;
            System.out.println("[RaveX-Loader] Pre-downloading asset: " + assetPath);
            RaveXLoader.updateWindowStatus("Downloading asset " + (i + 1) + "/" + assetList.size() + "...", 5 + (i * 2));

            try {
                URL url = new URL(remoteUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                if (conn.getResponseCode() == 200) {
                    File tempDownload = new File(parent, cachedFile.getName() + ".tmp");
                    try (InputStream in = conn.getInputStream();
                         FileOutputStream out = new FileOutputStream(tempDownload)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                    if (!tempDownload.renameTo(cachedFile)) {
                        Files.copy(tempDownload.toPath(), cachedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        tempDownload.delete();
                    }
                }
            } catch (Throwable t) {
                System.err.println("[RaveX-Loader] Failed to pre-download " + assetPath + ": " + t.getMessage());
            }
        }
    }
}
