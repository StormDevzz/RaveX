package ravex.utility.nativelib;

import java.io.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class NativeLoader {
    private static boolean loaded = false;
    private static boolean nativeAvailable = false;
    private static boolean jawtLoaded = false;

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
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

    private static synchronized void ensureJawtLoaded() {
        if (jawtLoaded || isWindows()) return;
        String javaHome = System.getProperty("java.home");
        if (javaHome == null) return;
        File jawt = new File(javaHome + "/lib/libjawt.so");
        if (jawt.exists()) {
            try {
                System.load(jawt.getAbsolutePath());
                jawtLoaded = true;
            } catch (Throwable ignored) {}
        }
    }

    private static void extractResourceToDir(File dir, String resourcePath, String fileName) {
        File outFile = new File(dir, fileName);
<<<<<<< HEAD
=======
        if (outFile.exists() && outFile.length() > 0) return;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        try (InputStream in = NativeLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) return;
            try (FileOutputStream out = new FileOutputStream(outFile)) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
            }
            outFile.setExecutable(true);
        } catch (Throwable ignored) {}
    }

    private static void extractAllNatives(File dir) {
        String prefix = "assets/ravex/natives/";
        String suffix = isWindows() ? ".dll" : ".so";

        try {
            java.net.URL jarUrl = NativeLoader.class.getProtectionDomain().getCodeSource().getLocation();
            if (jarUrl != null) {
                String jarPath = jarUrl.getPath();
                if (jarPath != null && jarPath.endsWith(".jar")) {
                    try (JarFile jar = new JarFile(new File(jarPath))) {
                        java.util.Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
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
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {}

        String[] knownLibs = {
            "libravex_jni.so", "libravex_optimize.so", "libravex_manager.so",
            "libravex_github_tools.so", "libravex_loader.so", "libravex_addon.so",
<<<<<<< HEAD
            "libravex_fileprot.so", "libravex_fastexp.so",
<<<<<<< HEAD
=======
            "libravex_benchmark.so", "libravex_fileprot.so", "libravex_fastexp.so",
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            "libravex_nativesc.so",
=======
            "libravex_nativesc.so", "libravex_c_addon_loader.so",
>>>>>>> 0ab37177398daa0e9880b2ec0d3ee76a2dbed416
            "libravex_autocrystal.so", "libravex_phase.so", "libravex_tntaura.so",
            "libravex_anchoraura.so", "libravex_breaker.so", "libravex_holefill.so",
            "libravex_trap.so", "libravex_selftrap.so", "libravex_baseplace.so",
            "libravex_bowaim.so", "libravex_quiver.so", "libravex_autoapple.so",
            "libravex_pearltarget.so", "libravex_antipearl.so", "libravex_bedbomb.so",
<<<<<<< HEAD
            "libravex_ecfarmer.so", "libravex_shieldfucker.so",
=======
            "libravex_ecfarmer.so", "libravex_shieldfucker.so", "libravex_noghostblocks.so",
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            "libravex_portalbuild.so", "libravex_chunkexploit.so", "libravex_packetmine.so",
            "libravex_nuker.so", "libravex_treecutter.so", "libravex_autoclicker.so",
            "libravex_antibot.so", "libravex_autodrop.so", "libravex_selffill.so",
            "libravex_burrow.so", "libravex_autoregear.so", "libravex_antiregear.so",
<<<<<<< HEAD
            "libravex_elytraplusplus.so",
            "libravex_desktopgui.so", "libravex_calculator.so",
            "libravex_faststairs.so", "libravex_noslow.so",
            "libravex_fakepearl.so", "libravex_antivoid.so",
            "libravex_antiquit.so", "libravex_safewalk.so", "libravex_mediaquery.so",
            "libravex_dc.so", "libravex_nametags.so", "libravex_animations.so",
=======
            "libravex_witherroseaura.so", "libravex_elytraplusplus.so",
            "libravex_desktopgui.so", "libravex_calculator.so", "libravex_voidesp.so",
            "libravex_faststairs.so", "libravex_noslow.so", "libravex_nofall.so",
            "libravex_replenish.so",
            "libravex_fakepearl.so", "libravex_coordlogger.so", "libravex_antivoid.so",
            "libravex_antiquit.so", "libravex_safewalk.so", "libravex_mediaquery.so",
            "libravex_dc.so", "libravex_nametags.so", "libravex_animations.so",
            "libravex_tunnels.so", "libravex_holeesp.so",
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            "libravex_shaders_native.so"
        };
        for (String lib : knownLibs) {
            String resource = prefix + lib;
            extractResourceToDir(dir, resource, lib);
        }
    }

    private static void loadJniDependencies(File dir) {
        if (isWindows()) return;
        String[] deps = {"libravex_optimize.so", "libravex_manager.so", "libravex_github_tools.so"};
        for (String dep : deps) {
            File f = new File(dir, dep);
            if (f.exists()) {
                try {
                    System.load(f.getAbsolutePath());
                } catch (Throwable t) {
                    System.err.println("[RaveX] Failed to pre-load dependency " + dep + ": " + t.getMessage());
                }
            }
        }
    }

    private static File findLibrary(String name) {
        boolean isWin = isWindows();
        String fileName = isWin ? name + ".dll" : "lib" + name + ".so";
        String prefix = isWin ? name : "lib" + name;
        String suffix = isWin ? ".dll" : ".so";

        File cacheDir = getCacheDir();
        if (!cacheDir.exists()) cacheDir.mkdirs();
        extractAllNatives(cacheDir);

        File cached = new File(cacheDir, fileName);
        if (cached.exists() && cached.length() > 0) return cached;

        try {
            InputStream in = NativeLoader.class.getResourceAsStream("/assets/ravex/natives/" + fileName);
            if (in != null) {
                File tempFile = File.createTempFile(prefix, suffix);
                tempFile.deleteOnExit();
                try (FileOutputStream out = new FileOutputStream(tempFile)) {
                    byte[] buf = new byte[8192];
                    int read;
                    while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
                }
                return tempFile;
            }
        } catch (Throwable ignored) {}

        return getOrDownloadLibrary(fileName);
    }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        try {
            System.loadLibrary("ravex_jni");
            nativeAvailable = true;
            return;
        } catch (UnsatisfiedLinkError ignored) {}

        String libName = getLibName();
        try {
            File cacheDir = getCacheDir();
            if (!cacheDir.exists()) cacheDir.mkdirs();
            extractAllNatives(cacheDir);
            ensureJawtLoaded();

            File cached = new File(cacheDir, libName);
            if (cached.exists() && cached.length() > 0) {
                loadJniDependencies(cacheDir);
                System.load(cached.getAbsolutePath());
                nativeAvailable = true;
                return;
            }

            File libFile = findLibrary("ravex_jni");
            if (libFile != null) {
                loadJniDependencies(libFile.getParentFile());
                System.load(libFile.getAbsolutePath());
                nativeAvailable = true;
                return;
            }

            System.err.println("[RaveX] JNI native library could not be loaded/downloaded: " + libName);
        } catch (Throwable ex) {
            System.err.println("[RaveX] WARNING: Failed to dynamically load native library: " + ex.getMessage());
        }
    }

    private static File getOrDownloadLibrary(String libName) {
        File cacheDir = getCacheDir();
        if (!cacheDir.exists()) cacheDir.mkdirs();
        extractAllNatives(cacheDir);
        File cachedFile = new File(cacheDir, libName);
        if (cachedFile.exists() && cachedFile.length() > 0) {
            return cachedFile;
        }
        return null;
    }

    public static synchronized boolean loadLibrary(String name) {
        try {
            System.loadLibrary(name);
            return true;
        } catch (UnsatisfiedLinkError ignored) {}

        try {
            ensureJawtLoaded();
            File libFile = findLibrary(name);
            if (libFile != null) {
                System.load(libFile.getAbsolutePath());
                return true;
            }
        } catch (Throwable ex) {
            System.err.println("[RaveX] Failed to load native library " + name + ": " + ex.getMessage());
        }
        return false;
    }

    public static boolean isNativeAvailable() {
        return nativeAvailable;
    }
}
