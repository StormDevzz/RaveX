package ravex.utility.nativelib;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class NativeLoader {
    private static boolean loaded = false;
    private static boolean nativeAvailable = false;
    private static boolean jawtLoaded = false;
    private static boolean summaryPrinted = false;
    private static final String MANAGER_EXE = "ravex_native_manager.exe";

    private static final Map<String, String> loadSuccesses = new ConcurrentHashMap<>();
    private static final Map<String, String> loadFailures = new ConcurrentHashMap<>();

    private static final Map<String, String> NATIVE_MANIFEST = Map.ofEntries(
        Map.entry("libravex_addon.so", "1b04210a926b218f81f302137e58d5b3178f55a0f1902976a01e16ec3b6968ae"),
        Map.entry("libravex_anchoraura.so", "17bc66f2652c174e37e4b0a058ba7410dc38ef83c1506a2392988e5b610cde35"),
        Map.entry("libravex_antibot.so", "004b59613da79e0061ef200520215934fcce1e7ec23426b71419f7bab08f27d2"),
        Map.entry("libravex_antipearl.so", "b13369ade37d5c8705f2b5d62f3fafef0d35eda2c695a71b593f232ef6874109"),
        Map.entry("libravex_antiquit.so", "b61021fb3dae5ca0138b47c0e1509e759aff2ec3126c30566aae7c92b81f533c"),
        Map.entry("libravex_antiregear.so", "935a86cf0da8195cb205ffde69ab3d890c33329c297332aa610b003b53395ab0"),
        Map.entry("libravex_autocrystal.so", "c94796163bf72dab1a19e24b4c2a097b5888871a21361cd445b16bc81367167e"),
        Map.entry("libravex_baseplace.so", "32a21ee2c7ccfdb911a08dc736ecbbfb20c750db67ee72b256fd40cdc31025a3"),
        Map.entry("libravex_bedbomb.so", "9f98e6a9cc18947d53a3ee64b5f6cf6c998880a5f951d8fb63ba20bb622ed798"),
        Map.entry("libravex_bowaim.so", "e6391676faedc0e1a3d7a46b421f4e1f6910786e31df08b4ad98309e16d11cdc"),
        Map.entry("libravex_breaker.so", "5be6823edca6d1d8ef7dc1dc516cdab178d2ac8f5d283e46a3471dff893c2096"),
        Map.entry("libravex_burrow.so", "c395a40bc5f87815173eaa859d26af80991f03a37cd0698e14e3f8c025752fa2"),
        Map.entry("libravex_calculator.so", "008e6c95184e99b0ac55014c574056909d3c636512a39f6fc9609f6e54b914bb"),
        Map.entry("libravex_dc.so", "9bfce280d0b33129f09f71f223b590252bd79c60bc303f5ecf0acedc77f596ef"),
        Map.entry("libravex_desktopgui.so", "3aea86d43b494bcd6a824a289cd7909f6cb2bd4861b7d00e10da8db64ffc4b1b"),
        Map.entry("libravex_elytraplusplus.so", "8d5b514ccac09df4ba155064d92b003152e8384347fa06d9cef2433f6c51e87c"),
        Map.entry("libravex_fastexp.so", "8d6e2f306774437db4c50518eba0c80b42e6180890260dc65ea17525a5680941"),
        Map.entry("libravex_fileprot.so", "2391cff1a348f356baa2a4626ffdd6645876a3fb568395efc4556f172b0a6efc"),
        Map.entry("libravex_github_tools.so", "067ce0e093aa1c77495ed2c519829d615af2bcf1943aad40cf7f3a4eae15907e"),
        Map.entry("libravex_holefill.so", "97c6bfd4e19e4e47d4795c3c9d501487730a95be7c2ffd05440088a9a66c942f"),
        Map.entry("libravex_jni.so", "d531a0e2e43b05c8d38998da4d276e76aaabe51dae7f77aa4a5d92be9bba9124"),
        Map.entry("libravex_loader.so", "91df4b7a1f307a7467a31e89ee8153d262cda4b0ca3bad8d0c482e56c13de505"),
        Map.entry("libravex_manager.so", "97b64fe4807c2987ab48872599f027ff98e5b5361bbc6a516919f5e3b9852e6b"),
        Map.entry("libravex_mediaquery.so", "1ae5d82c3d25af7b3337c16301367166e5c52cdaacc463aa4e49d13e1145b576"),
        Map.entry("libravex_nativesc.so", "714c4ea0921c355d1838a71e4ddcc8a7cae46a9bb29b713c9fdb0d0eeb55cb4f"),
        Map.entry("libravex_nopacktkick.so", "bf859e06ab3aea48286ab84051d932b7e2014bcfb19eb34b61de3cd521e6fbdd"),
        Map.entry("libravex_optimize.so", "b23a3d7b229569584fdc0c566dbb78ed5d755ebd2c707b5421f5a9b66b3b9b86"),
        Map.entry("libravex_pearltarget.so", "172726c629a289a102f2958ea2d061daf52ff19b7b03af98552e0f6850284462"),
        Map.entry("libravex_phase.so", "de6ac9835c4e3739010bb89f1e2ca6d43397ac804875ede5c5f9c5d4ef6e2c1d"),
        Map.entry("libravex_quiver.so", "2b8ee2e42b0bfacee5c8358bd08292cb5ef2b18bb0f9b1371caee944d0783701"),
        Map.entry("libravex_selftrap.so", "76c62798fd0078656e368bdb7bc0a6b1a046983cde8b99c2a4958b83185dc4ef"),
        Map.entry("libravex_shieldfucker.so", "e1282362b933118caf04af80445e13aea9f96ff6a41ba028288861b0e15df8e9"),
        Map.entry("libravex_tntaura.so", "c3f108e7579d14c44fad60c9758dc2cee36293d716da8df78e3a45c2b6a004f1"),
        Map.entry("libravex_trap.so", "a6e54f22900e80423832e1326012e9ad3814432f6841197183f47f2bab950d4f"),
        Map.entry("ravex_addon.dll", "7b6683ada54862e365fa8ad8769ae1a50bbc3aa54f5d63b2684aca9ac1c649bf"),
        Map.entry("ravex_anchoraura.dll", "e72c5b0c6ab16e25256b982421485561fa10665026f3e44e4d431abc538ebff5"),
        Map.entry("ravex_antibot.dll", "b0762e70d7da5fb79dd0683fd4acc3f890fb81d174daec87f5115dceed71f210"),
        Map.entry("ravex_antipearl.dll", "5a705b506fae22b9e1b4217de5056b68016e6007ed415fef6254583b753fead4"),
        Map.entry("ravex_antiquit.dll", "4ce79179156528c25705c9721368fd061dd386553e675da8efee5f3b247a9214"),
        Map.entry("ravex_antiregear.dll", "7da2ada172f9f5e076503639b19be447bcb569d850b5dc8d50a16f7c348bbf77"),
        Map.entry("ravex_autocrystal.dll", "63fa6d52240c64cf247280988e206adb8aa2f46a05371cace3275434f8e291ca"),
        Map.entry("ravex_baseplace.dll", "7ff8759c9498b9788ecb3687d2a5add7144e338cb86f9540f4a8096f6f8e6562"),
        Map.entry("ravex_bedbomb.dll", "5f61191dfa828da5a0d190a3cc254ca894a82dbd39fd333bd1f0e2d2e5e5db89"),
        Map.entry("ravex_bowaim.dll", "61f7ba67337d42e628205a971074d4e561ef30954602fa02ae771f5d4602c298"),
        Map.entry("ravex_breaker.dll", "e4e1034bbd3e3ad61a55765f56cf3be8699df42a684fd8f06e8c92f2fadd11c6"),
        Map.entry("ravex_burrow.dll", "55ee217917430c60e0a13654aa9dbedb3b4d8172c9ff86308271afb668dbba47"),
        Map.entry("ravex_calculator.dll", "a24e1aa711ad8f2fc0aecd4caad76d69f5cd5e8e3c49069afe984b682495b8ef"),
        Map.entry("ravex_dc.dll", "2227c5aab52af498def0ccf69dacf323c1b7c8f97c828b6e572ebc91d94ca61d"),
        Map.entry("ravex_desktopgui.dll", "f0622041bd153f8c54ee7b016bb8bd66b5d5a6240ea29afe78ab6874104fe68b"),
        Map.entry("ravex_elytraplusplus.dll", "333518e325a8e4266d33ca6984b5a4d7cf48ad63a1a73edcd483160849d93796"),
        Map.entry("ravex_fastexp.dll", "a0e391486ccdb287a9dde73bd8fd389817f7ce452e39fafaac50e5a1a3a1cbf8"),
        Map.entry("ravex_fileprot.dll", "84995280c77870cde00ceed0b8623d8f71437dc8588a76357e13d1c099180b1b"),
        Map.entry("ravex_font.dll", "465a7498840e9946ceb8bf17998fd1f66b1bad4c1dbaca3f87586c77d0c20f80"),
        Map.entry("ravex_github_tools.dll", "0ef6bdabb16b8256d63fa661734e5879faebecfdc70189999464e1e016f2a11d"),
        Map.entry("ravex_holefill.dll", "5f0d335d2be502957135a9458c4abf766baf4ec575ba47c04fd4138b7ec67a59"),
        Map.entry("ravex_jni.dll", "abd9baeb05293287999216f7a452a7f2fab9c163b974866fb4c67984daeaa35a"),
        Map.entry("ravex_loader.dll", "9977e6592a4bac41145e6f0dec9e64e5488c6e88a08464bde0e2e26eea3d3ea8"),
        Map.entry("ravex_manager.dll", "a98ebaf6a572247eaef9f97308dff73a3efd43ba7218ab3d278aeb3f367206ba"),
        Map.entry("ravex_mediaquery.dll", "4c8df3a2ed66ff8c4bf3fbf457ed77b1798a2777710edce401ccd5732f10927e"),
        Map.entry("ravex_native_manager.exe", "38861e8a53534bad376ca85abca377675d28eadf839ba773e88101ddbd8f7448"),
        Map.entry("ravex_nativesc.dll", "e5a87f361305f1b5c1b4f6dea6261785d190e7c7e3f2d8ee34c6312b8bc98bb2"),
        Map.entry("ravex_nopacktkick.dll", "420b001cf7b462f1879a701fdd8b96d1d02fb2359ec3dd95af732ef9f8795903"),
        Map.entry("ravex_nuker.dll", "0a1c2f2971bd1afbeb5536726d5bfa480fc61c848d9a1c0954b9d2a272996d21"),
        Map.entry("ravex_optimize.dll", "21ee61973f63969c6c7de08484077dd390f5b4b4447ac7f3d9c84d8d53d9fadf"),
        Map.entry("ravex_pearltarget.dll", "169c141d2126c15191af62fe537136be5823ff7805e12199f46b8bcadb161c1c"),
        Map.entry("ravex_phase.dll", "37088d17d9d675e681ac8e28d4d67198162b214a24551c9f8ab047b115f9c2df"),
        Map.entry("ravex_quiver.dll", "39a530e3f391b6b6d5cc300c24f0b1a761bf21094616c602d5a849787eb55630"),
        Map.entry("ravex_selftrap.dll", "b6c6dbfda0782728bc71e9f0e1b31075ab56b98337e6b7d55103d77a859ea0ee"),
        Map.entry("ravex_shaderfx.dll", "891d93aba89455df08c673cb70594649b448b4601b3cbc64c284d1db7d12c298"),
        Map.entry("ravex_shieldfucker.dll", "2727e9f6659f8c407c53ac6bc2b44b33d8b3a21c64086af67478844b2ea20403"),
        Map.entry("ravex_tntaura.dll", "b650e178669cf6c79ecc5281cf2fb03c503922e61b43db130d83de613454a29d"),
        Map.entry("ravex_trap.dll", "604b03c8e5953b04ad035d37a4caad271fa1c2c5f4c22d9cb28b2a1a02b3ee56")
    );

    private static boolean isNativeBlockedByGlibc() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.contains("linux")) return false;
        try {
            Process p = new ProcessBuilder("ldd", "--version").redirectErrorStream(true).start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains("2.43") || line.contains("2.44") || line.contains("2.45")) return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static String getLibName() {
        return isWindows() ? "ravex_jni.dll" : "libravex_jni.so";
    }

    private static File getCacheDir() {
        String home = System.getProperty("user.home");
        return new File(home + "/.ravex/natives");
    }

    private static synchronized void ensureJawtLoaded() {
        if (jawtLoaded) return;
        String javaHome = System.getProperty("java.home");
        if (javaHome == null) return;
        String[] jawtPaths;
        if (isWindows()) {
            jawtPaths = new String[]{javaHome + "/bin/jawt.dll"};
        } else {
            jawtPaths = new String[]{javaHome + "/lib/libjawt.so", javaHome + "/lib/amd64/libjawt.so"};
        }
        for (String jawtPath : jawtPaths) {
            File jawt = new File(jawtPath);
            if (jawt.exists()) {
                try {
                    System.load(jawt.getAbsolutePath());
                    jawtLoaded = true;
                    return;
                } catch (Throwable ignored) {}
            }
        }
    }

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

    private static final String EXTRACTED_STATE = ".ravex-extracted.txt";
    private static final Map<String, String> extractedHashes = new ConcurrentHashMap<>();
    private static final Set<String> customBuildsLogged = ConcurrentHashMap.newKeySet();
    private static boolean extractedStateLoaded = false;

    private static synchronized void loadExtractedState(File cacheDir) {
        if (extractedStateLoaded) return;
        extractedStateLoaded = true;
        File state = new File(cacheDir, EXTRACTED_STATE);
        if (!state.exists()) return;
        try {
            for (String line : Files.readAllLines(state.toPath())) {
                int eq = line.indexOf('=');
                if (eq > 0) extractedHashes.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        } catch (Throwable ignored) {}
    }

    private static synchronized void rememberExtracted(String fileName, String hash) {
        if (hash == null) return;
        extractedHashes.put(fileName, hash);
        File cacheDir = getCacheDir();
        if (!cacheDir.exists()) cacheDir.mkdirs();
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> e : extractedHashes.entrySet()) {
            lines.add(e.getKey() + "=" + e.getValue());
        }
        try {
            Files.write(new File(cacheDir, EXTRACTED_STATE).toPath(), lines);
        } catch (Throwable ignored) {}
        writeCacheNotice(cacheDir);
    }

    private static void writeCacheNotice(File cacheDir) {
        File notice = new File(cacheDir, "README.txt");
        if (notice.exists()) return;
        List<String> lines = List.of(
            "RaveX native libraries cache",
            "",
            "Files here are unpacked from the mod jar by NativeLoader.",
            "You can drop your own build of a library into this folder: if its hash is not the bundled one,",
            "the file is used as is, which is handy for testing native changes without rebuilding the mod.",
            "Delete a file to get the bundled build back.",
            ".ravex-extracted.txt lists the files unpacked by the mod, those are refreshed automatically."
        );
        try {
            Files.write(notice.toPath(), lines);
        } catch (Throwable ignored) {}
    }

    private static File getCachedFile(String fileName) {
        File cacheDir = getCacheDir();
        File cached = new File(cacheDir, fileName);
        if (!cached.exists() || cached.length() == 0) return null;

        loadExtractedState(cacheDir);
        writeCacheNotice(cacheDir);

        String expectedHash = NATIVE_MANIFEST.get(fileName);
        String actualHash = sha256Hex(cached);
        if (expectedHash != null && expectedHash.equals(actualHash)) return cached;

        String extractedHash = extractedHashes.get(fileName);
        if (extractedHash != null && extractedHash.equals(actualHash)) {
            cached.delete();
            return null;
        }

        // хеш не совпал с манифестом и не наш, значит файл положил пользователь
        if (customBuildsLogged.add(fileName)) {
            try {
                ravex.RaveX.LOGGER.info("[Natives] {} is used as a custom build, its hash is not the bundled one", fileName);
            } catch (Throwable ignored) {}
        }
        return cached;
    }

    private static boolean writeResourceToFile(String resourcePath, File outFile) {
        try (InputStream in = NativeLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) return false;
            try (FileOutputStream out = new FileOutputStream(outFile)) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean writeJarEntryToFile(String entryName, File outFile) {
        try {
            URL jarUrl = NativeLoader.class.getProtectionDomain().getCodeSource().getLocation();
            if (jarUrl == null) return false;
            String jarPath = jarUrl.getPath();
            if (jarPath == null || !jarPath.endsWith(".jar")) return false;
            try (JarFile jar = new JarFile(new File(jarPath))) {
                JarEntry entry = jar.getJarEntry(entryName);
                if (entry == null || entry.isDirectory()) return false;
                try (InputStream in = jar.getInputStream(entry);
                     FileOutputStream out = new FileOutputStream(outFile)) {
                    byte[] buf = new byte[8192];
                    int read;
                    while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
                }
                return true;
            }
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static File extractFromJar(File dir, String fileName) {
        String entryName = "assets/ravex/natives/" + fileName;
        File outFile = new File(dir, fileName);
        if (writeResourceToFile("/" + entryName, outFile) || writeJarEntryToFile(entryName, outFile)) {
            String expectedHash = NATIVE_MANIFEST.get(fileName);
            if (expectedHash != null && verifySha256(outFile, expectedHash)) {
                outFile.setExecutable(true);
                rememberExtracted(fileName, expectedHash);
                return outFile;
            }
        }
        if (outFile.exists()) outFile.delete();
        return null;
    }

    private static void preloadDependencies(File dir) {
        String[] deps = {"optimize", "manager", "github_tools"};
        for (String dep : deps) {
            String fileName = isWindows() ? "ravex_" + dep + ".dll" : "libravex_" + dep + ".so";
            File f = new File(dir, fileName);
            if (!f.exists()) f = obtainLibrary(dep);
            if (f != null) {
                try {
                    System.load(f.getAbsolutePath());
                    loadSuccesses.put(dep, "dependency");
                } catch (Throwable t) {
                    loadFailures.put(dep, "load error: " + t.getMessage());
                }
            } else {
                loadFailures.put(dep, "file not found in JAR");
            }
        }
    }

    private static final Set<String> BROKEN_LIBS = Set.of();

    private static File obtainLibrary(String name) {
        boolean isWin = isWindows();
        String fileName = isWin ? name + ".dll" : "lib" + name + ".so";

        File cached = getCachedFile(fileName);
        if (cached != null) return cached;

        File cacheDir = getCacheDir();
        if (!cacheDir.exists()) cacheDir.mkdirs();

        File extracted = extractFromJar(cacheDir, fileName);
        if (extracted != null) return extracted;

        return null;
    }

    // keeps a ravex entry in the task manager while the game runs
    private static void startNativeManager() {
        if (!isWindows()) return;
        try {
            File cacheDir = getCacheDir();
            if (!cacheDir.exists()) cacheDir.mkdirs();
            File exe = getCachedFile(MANAGER_EXE);
            if (exe == null) exe = extractFromJar(cacheDir, MANAGER_EXE);
            if (exe == null) return;
            new ProcessBuilder(exe.getAbsolutePath(), "--parent", String.valueOf(ProcessHandle.current().pid()))
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            loadSuccesses.put("native_manager", "started");
        } catch (Throwable t) {
            loadFailures.put("native_manager", String.valueOf(t.getMessage()));
        }
    }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;

        if (isNativeBlockedByGlibc()) {
            loadFailures.put("ravex_jni", "glibc 2.43+ detected, dlopen unsafe");
            System.err.println("[RaveX] Glibc 2.43+ detected - native libs disabled to avoid dlopen crash. If you see this, please report it on our Discord: https://discord.gg/MeAWgDYzqn");
            return;
        }

        try {
            System.loadLibrary("ravex_jni");
            nativeAvailable = true;
            loadSuccesses.put("ravex_jni", "system path");
            NativeLibraryUtility.enableLoading();
            startNativeManager();
            return;
        } catch (UnsatisfiedLinkError ignored) {}

        try {
            ensureJawtLoaded();
            preloadDependencies(getCacheDir());

            File jniFile = obtainLibrary("ravex_jni");
            if (jniFile != null) {
                System.load(jniFile.getAbsolutePath());
                nativeAvailable = true;
                loadSuccesses.put("ravex_jni", "extracted");
                NativeLibraryUtility.enableLoading();
                startNativeManager();
                return;
            }

            loadFailures.put("ravex_jni", "could not be extracted from JAR");
            System.err.println("[RaveX] JNI native library could not be loaded: " + getLibName() + ". If you see this, please report it on our Discord: https://discord.gg/MeAWgDYzqn");
        } catch (Throwable ex) {
            loadFailures.put("ravex_jni", ex.getMessage());
            System.err.println("[RaveX] WARNING: Failed to dynamically load native library: " + ex.getMessage() + ". If you see this, please report it on our Discord: https://discord.gg/MeAWgDYzqn");
        }
    }

    public static synchronized boolean loadLibrary(String name) {
        if (loadSuccesses.containsKey(name)) return true;
        if (loadFailures.containsKey(name)) return false;

        try {
            System.loadLibrary(name);
            loadSuccesses.put(name, "system path");
            return true;
        } catch (UnsatisfiedLinkError ignored) {}

        String libFileName = (isWindows() ? "" : "lib") + name + (isWindows() ? ".dll" : ".so");
        if (BROKEN_LIBS.contains(libFileName)) {
            loadFailures.put(name, "disabled due to system compatibility");
            System.err.println("[RaveX] Native library " + name + " disabled due to system compatibility. If you see this, please report it on our Discord: https://discord.gg/MeAWgDYzqn");
            return false;
        }

        try {
            ensureJawtLoaded();
            File libFile = obtainLibrary(name);
            if (libFile != null) {
                try {
                    System.load(libFile.getAbsolutePath());
                    loadSuccesses.put(name, "extracted");
                    return true;
                } catch (UnsatisfiedLinkError e) {
                    loadFailures.put(name, "load error: " + e.getMessage());
                    System.err.println("[RaveX] Native library " + name + " unavailable: " + e.getMessage() + ". If you see this, please report it on our Discord: https://discord.gg/MeAWgDYzqn");
                }
            } else {
                loadFailures.put(name, "file not found in JAR");
            }
        } catch (Throwable ex) {
            loadFailures.put(name, ex.getMessage());
            System.err.println("[RaveX] Failed to load native library " + name + ": " + ex.getMessage() + ". If you see this, please report it on our Discord: https://discord.gg/MeAWgDYzqn");
        }
        return false;
    }

    public static boolean isNativeAvailable() {
        return nativeAvailable;
    }

    public static int getLoadedCount() {
        return loadSuccesses.size();
    }

    public static int getFailedCount() {
        return loadFailures.size();
    }

    public static int getTotalCount() {
        return loadSuccesses.size() + loadFailures.size();
    }

    public static synchronized void printLoadSummary() {
        if (summaryPrinted) return;
        summaryPrinted = true;

        int ok = loadSuccesses.size();
        int fail = loadFailures.size();
        int total = ok + fail;
        String os = System.getProperty("os.name", "unknown");

        ravex.RaveX.LOGGER.info("[Natives] OS: {} | JNI available: {} | Libraries loaded: {}/{}", os, nativeAvailable, ok, total);

        if (!loadSuccesses.isEmpty()) {
            for (Map.Entry<String, String> e : loadSuccesses.entrySet()) {
                ravex.RaveX.LOGGER.info("[Natives]   OK: {} ({})", e.getKey(), e.getValue());
            }
        }

        if (!loadFailures.isEmpty()) {
            for (Map.Entry<String, String> e : loadFailures.entrySet()) {
                ravex.RaveX.LOGGER.warn("[Natives]   FAIL: {} - {}", e.getKey(), e.getValue());
            }
        }

        if (total == 0) {
            ravex.RaveX.LOGGER.warn("[Natives] No native libraries were loaded. Native features will be unavailable.");
        } else if (fail == 0) {
            ravex.RaveX.LOGGER.info("[Natives] All {} native library(ies) loaded successfully.", ok);
        } else {
            ravex.RaveX.LOGGER.warn("[Natives] {}/{} native library(ies) failed to load. Some features may be unavailable.", fail, total);
        }
    }
}