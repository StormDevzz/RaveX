package ravex.utility.nativelib;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class NativeLoader {
    private static boolean loaded = false;
    private static boolean nativeAvailable = false;
    private static boolean jawtLoaded = false;

    private static final String REMOTE_BASE = "https://raw.githubusercontent.com/StormDevzz/RaveX/main/assets/ravex/natives/";

    private static final Map<String, String> NATIVE_MANIFEST = Map.ofEntries(
        Map.entry("libravex_addon.so", "1b04210a926b218f81f302137e58d5b3178f55a0f1902976a01e16ec3b6968ae"),
        Map.entry("libravex_anchoraura.so", "17bc66f2652c174e37e4b0a058ba7410dc38ef83c1506a2392988e5b610cde35"),
        Map.entry("libravex_antibot.so", "004b59613da79e0061ef200520215934fcce1e7ec23426b71419f7bab08f27d2"),
        Map.entry("libravex_antipearl.so", "b13369ade37d5c8705f2b5d62f3fafef0d35eda2c695a71b593f232ef6874109"),
        Map.entry("libravex_antiquit.so", "b61021fb3dae5ca0138b47c0e1509e759aff2ec3126c30566aae7c92b81f533c"),
        Map.entry("libravex_antiregear.so", "935a86cf0da8195cb205ffde69ab3d890c33329c297332aa610b003b53395ab0"),
        Map.entry("libravex_autoclicker.so", "bcc0aaca05b30f381d00c4aabbd08883e1627a938a340f0247c0f09de457a15f"),
        Map.entry("libravex_autocrystal.so", "c94796163bf72dab1a19e24b4c2a097b5888871a21361cd445b16bc81367167e"),
        Map.entry("libravex_autodrop.so", "e60a79335884e2b9304d8164c23d0c5e9f9482af3eb53ac28bcb5b574699d87f"),
        Map.entry("libravex_autoregear.so", "9ba8efadce23fc0e4e479797570c835c06c66869a48038edd3efd1ef1109d88c"),
        Map.entry("libravex_baseplace.so", "32a21ee2c7ccfdb911a08dc736ecbbfb20c750db67ee72b256fd40cdc31025a3"),
        Map.entry("libravex_bedbomb.so", "9f98e6a9cc18947d53a3ee64b5f6cf6c998880a5f951d8fb63ba20bb622ed798"),
        Map.entry("libravex_bowaim.so", "e6391676faedc0e1a3d7a46b421f4e1f6910786e31df08b4ad98309e16d11cdc"),
        Map.entry("libravex_breaker.so", "5be6823edca6d1d8ef7dc1dc516cdab178d2ac8f5d283e46a3471dff893c2096"),
        Map.entry("libravex_burrow.so", "c395a40bc5f87815173eaa859d26af80991f03a37cd0698e14e3f8c025752fa2"),
        Map.entry("libravex_calculator.so", "008e6c95184e99b0ac55014c574056909d3c636512a39f6fc9609f6e54b914bb"),
        Map.entry("libravex_chunkexploit.so", "37776b6563a47710bb32008e3588d5bc186493f0b136cae0626a5bd3d109c3f4"),
        Map.entry("libravex_dc.so", "9bfce280d0b33129f09f71f223b590252bd79c60bc303f5ecf0acedc77f596ef"),
        Map.entry("libravex_desktopgui.so", "3aea86d43b494bcd6a824a289cd7909f6cb2bd4861b7d00e10da8db64ffc4b1b"),
        Map.entry("libravex_ecfarmer.so", "7ee59bc814ecdbaa1a746244484773a6cc8321c59f733b15d5d1c7033ac7273a"),
        Map.entry("libravex_elytraplusplus.so", "8d5b514ccac09df4ba155064d92b003152e8384347fa06d9cef2433f6c51e87c"),
        Map.entry("libravex_fakepearl.so", "7e4be0b9d64317fc9c6103273dc59006c2b7a8c822d2c99a8e83a26dc8187de5"),
        Map.entry("libravex_fastexp.so", "8d6e2f306774437db4c50518eba0c80b42e6180890260dc65ea17525a5680941"),
        Map.entry("libravex_fileprot.so", "2391cff1a348f356baa2a4626ffdd6645876a3fb568395efc4556f172b0a6efc"),
        Map.entry("libravex_github_tools.so", "067ce0e093aa1c77495ed2c519829d615af2bcf1943aad40cf7f3a4eae15907e"),
        Map.entry("libravex_holefill.so", "97c6bfd4e19e4e47d4795c3c9d501487730a95be7c2ffd05440088a9a66c942f"),
        Map.entry("libravex_jni.so", "d531a0e2e43b05c8d38998da4d276e76aaabe51dae7f77aa4a5d92be9bba9124"),
        Map.entry("libravex_loader.so", "91df4b7a1f307a7467a31e89ee8153d262cda4b0ca3bad8d0c482e56c13de505"),
        Map.entry("libravex_manager.so", "97b64fe4807c2987ab48872599f027ff98e5b5361bbc6a516919f5e3b9852e6b"),
        Map.entry("libravex_mediaquery.so", "1ae5d82c3d25af7b3337c16301367166e5c52cdaacc463aa4e49d13e1145b576"),
        Map.entry("libravex_nametags.so", "e0ec4dfe357bf0cd02e18233219a460612974d8530ffdb9347f7dc84f693dc73"),
        Map.entry("libravex_nativesc.so", "714c4ea0921c355d1838a71e4ddcc8a7cae46a9bb29b713c9fdb0d0eeb55cb4f"),
        Map.entry("libravex_nopacktkick.so", "bf859e06ab3aea48286ab84051d932b7e2014bcfb19eb34b61de3cd521e6fbdd"),
        Map.entry("libravex_noslow.so", "4e6ea8fdc74f42bb99e0e67ed5524a34a776c37bab07bae4e439f139a2e57d14"),
        Map.entry("libravex_nuker.so", "8d23a764c087a8c1efc14084fa36c9da4387d982635542e85fe3dced66f2f5c5"),
        Map.entry("libravex_optimize.so", "b23a3d7b229569584fdc0c566dbb78ed5d755ebd2c707b5421f5a9b66b3b9b86"),
        Map.entry("libravex_packetmine.so", "c9f34695eb655e7b0871e800bb276fb85988b94b4c72b87b0fb7c01ea8a0b9d6"),
        Map.entry("libravex_pearltarget.so", "172726c629a289a102f2958ea2d061daf52ff19b7b03af98552e0f6850284462"),
        Map.entry("libravex_phase.so", "de6ac9835c4e3739010bb89f1e2ca6d43397ac804875ede5c5f9c5d4ef6e2c1d"),
        Map.entry("libravex_quiver.so", "2b8ee2e42b0bfacee5c8358bd08292cb5ef2b18bb0f9b1371caee944d0783701"),
        Map.entry("libravex_safewalk.so", "25a959167840f5f994c5d5b0c21bfa8a2a70a3d05394b7376867e8514a26401b"),
        Map.entry("libravex_selffill.so", "cbbb94acd95f775ed67f15fdaf59fd96d1b76a56776b93fd6280c706744ef06f"),
        Map.entry("libravex_selftrap.so", "76c62798fd0078656e368bdb7bc0a6b1a046983cde8b99c2a4958b83185dc4ef"),
        Map.entry("libravex_shieldfucker.so", "e1282362b933118caf04af80445e13aea9f96ff6a41ba028288861b0e15df8e9"),
        Map.entry("libravex_tntaura.so", "c3f108e7579d14c44fad60c9758dc2cee36293d716da8df78e3a45c2b6a004f1"),
        Map.entry("libravex_trap.so", "a6e54f22900e80423832e1326012e9ad3814432f6841197183f47f2bab950d4f"),
        Map.entry("ravex_addon.dll", "f70d2a9041a83efc6d7da61c4c2ac08b780acb90c1c6c9e0b4e0a81ce1743dc5"),
        Map.entry("ravex_anchoraura.dll", "90db1c5bc3331496325ffb660f39f82096fabc5acd3942830bb639c9d77e0291"),
        Map.entry("ravex_antibot.dll", "0c39991a1465729457c9eb532ab1d0b0b0c7bf548379499829c59e135744c7c7"),
        Map.entry("ravex_antipearl.dll", "9300a23d453b1506b9ebc4aaa4af03156b0487fd0234b68ddff2840c9a6fc902"),
        Map.entry("ravex_antiquit.dll", "eb2bc0d7d19ef0835be71c0087044e428f5bfe7d90bb254380cd0231660b525c"),
        Map.entry("ravex_antiregear.dll", "d267a6ae82cfa82d7f3da09ab11b243bec3e2f9b22b821f8aab10693493b0248"),
        Map.entry("ravex_autoclicker.dll", "2fd33d617e3d8671db8bfbfcb05de1edd63385cd75b17bdc4b93c9a1e61836e8"),
        Map.entry("ravex_autocrystal.dll", "8d35960bb635521d582aaa995f2f3cfa5a9b751a96c810cdb5e49e2f80797a19"),
        Map.entry("ravex_autodrop.dll", "c14b2238288b8d0bc610d6e6fc163cf5e116abb8c84c10d0345731929b8b8e28"),
        Map.entry("ravex_autoregear.dll", "45a6cb121f33721c31d90206a265f0dac05ff5840fb41584b0887451168c5abc"),
        Map.entry("ravex_baseplace.dll", "9c31611269b7353e5dfdeb7694c4186ded5a86fc9a0f2e111536b854cc10065a"),
        Map.entry("ravex_bedbomb.dll", "2f101085fc00ace69dd5044c3aa859216f7af997b68f3a44f768dff069c12c51"),
        Map.entry("ravex_bowaim.dll", "ab9643f0bf38ad81f430805a2199e67c96f1393bd7759fa1c5e62d6eb44a3958"),
        Map.entry("ravex_breaker.dll", "8352e870dac38ed81c117a2756bf3ed0cd67fb8b126000a5286e6860b4934cde"),
        Map.entry("ravex_burrow.dll", "9d158d97c27da51eedb19ac8e5ea8cb69674c4f7424c35433ddfa47ce4062620"),
        Map.entry("ravex_calculator.dll", "a4f028dc890abdc00312ed68d3a755d4a453f4a6026f81df2b9bba859af71439"),
        Map.entry("ravex_chunkexploit.dll", "d7990345edab685f37aef7ae2c3bd5fd87817ccaf92597e2ee84e94a7130dc39"),
        Map.entry("ravex_dc.dll", "0b4fe267de9a9dac3ec3f85c295d81d203cb927c29bb3f1ea97cb94aef82e289"),
        Map.entry("ravex_desktopgui.dll", "fefee04eaf57193617d2cb4a9fe40bcd58f08e73853f510dec55f2dd011613a0"),
        Map.entry("ravex_ecfarmer.dll", "18ebf3a7d676fe8c1880fcc75f03aa288102c61875cd0bde787418d5fca2e96d"),
        Map.entry("ravex_elytraplusplus.dll", "8c2cb55c27c17121b63f9c3340c893c2eb98637c2a004f7829a5f0a7152aa333"),
        Map.entry("ravex_fakepearl.dll", "f6014784e32e1705c5777d49793cf881aa1271098d76ffb768adcab6c867b036"),
        Map.entry("ravex_fastexp.dll", "84837a6a54c305efd356c23ff859dd957217518ff81f8b4239caffb8a9131658"),
        Map.entry("ravex_fileprot.dll", "1beee2476c04e13397b06c09f62a1f5789c1a15d07162537b66e8f7c34c56546"),
        Map.entry("ravex_github_tools.dll", "1d2071c34b756ec15276dd5ec84863eeeba85f66cba689368ddf67e1728c4549"),
        Map.entry("ravex_holefill.dll", "67a00256558828bf3608dc9316d46a2eecf816815a8b8aa2f3835d2d0f38142d"),
        Map.entry("ravex_jni.dll", "1ac6230fc6a2657f8f6986981f5f51fd4d45eed0a34abb5044b795d5aa279395"),
        Map.entry("ravex_loader.dll", "84f9b122fa98ab81bcb2cc498dc4f3e29f56d7b683108dd3f6f1af9ecb2fd092"),
        Map.entry("ravex_manager.dll", "422a5feddd4bda16c960e00ad35b5ca5721c44afd8d8d6709b9138ad4062dd88"),
        Map.entry("ravex_mediaquery.dll", "9d2a3f7f87984df1d2e6250536d16d97302b15cbb5a9265b10c79b868e6b9835"),
        Map.entry("ravex_nametags.dll", "5456e2a34157056de9f19300fd4db469e09a10c779eeb7890410a36db04b3948"),
        Map.entry("ravex_nativesc.dll", "e4b58e5829eeda66277941319aac1b45148efaa6e0a1d41c904a08b49354cf4c"),
        Map.entry("ravex_nopacktkick.dll", "4fb03862b970b6061305f2d95fc77cf9ef2660c831fcd9cbdbc25d8f1c5bec40"),
        Map.entry("ravex_noslow.dll", "8414bf5fbc431a1a0fbf8fd7b918857e40bb83dd0e6030f3db3b77b5517d4735"),
        Map.entry("ravex_nuker.dll", "44eff1ebfdef4ab47a2d2e387ca4156056e5dfaac97037bd385ac17ad07c60d2"),
        Map.entry("ravex_optimize.dll", "8000c57e8c5c25f0b0bf010babdfef20325b4936b49f2dd849d605ee34e9809f"),
        Map.entry("ravex_packetmine.dll", "1dc6ca69042b2f42671422056c79c8c2481baff913d20516a936fd37d393665b"),
        Map.entry("ravex_pearltarget.dll", "54fa676b212eb2c1d03e123a4863f4e75b010cd53516388f1cb234afc64d6b05"),
        Map.entry("ravex_phase.dll", "293dab52a546a92d060b7e1353240436f82e6d4971191025de5c1f475239a7bc"),
        Map.entry("ravex_quiver.dll", "010d45aac2fa5d279b0ac5298140b6284cb0ff4f89f74818ee06fe1570bc13f0"),
        Map.entry("ravex_safewalk.dll", "f4e16a9fd7e141db8d5ca27fa215186f411ce6f4a3ded7e62a76439f4abcf33e"),
        Map.entry("ravex_selffill.dll", "a8c460898322b251340e568a8c77258d72e8c102877f7a64f70a17f46373ac98"),
        Map.entry("ravex_selftrap.dll", "b8ccf64aaa4986cc8274b6b58ff1f2484a96a5563a347731e0c7135c10d975f9"),
        Map.entry("ravex_shieldfucker.dll", "9aa950657172f4b8acccd40f4731fac75c7a21db5bba5cbaaa25ca378e622eca"),
        Map.entry("ravex_tntaura.dll", "6d6d7ad109c2eeb2fad117544466d857df94027b602d48d4c5460425aab41fb4"),
        Map.entry("ravex_trap.dll", "d09ea16bad0d6c194a00d8286326b7a8fdb502b78af084125c5a45cc6be3f629")
    );

    private static boolean isNativeBlockedByGlibc() {
        if (isWindows()) return false;
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

    private static String getTempPrefix() {
        return isWindows() ? "ravex_jni" : "libravex_jni";
    }

    private static String getTempSuffix() {
        return isWindows() ? ".dll" : ".so";
    }

    private static File getCacheDir() {
        String home = System.getProperty("user.home");
        return new File(home + "/.ravex/natives");
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

    private static File getCachedFile(String fileName) {
        File cacheDir = getCacheDir();
        File cached = new File(cacheDir, fileName);
        String expectedHash = NATIVE_MANIFEST.get(fileName);
        if (cached.exists() && cached.length() > 0) {
            if (expectedHash != null && verifySha256(cached, expectedHash)) {
                return cached;
            }
            cached.delete();
        }
        return null;
    }

    private static boolean downloadFromGitHub(String fileName, File dest) {
        String urlStr = REMOTE_BASE + fileName;
        String expectedHash = NATIVE_MANIFEST.get(fileName);
        if (expectedHash == null) return false;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) return false;

            File destDir = dest.getParentFile();
            if (destDir != null && !destDir.exists()) destDir.mkdirs();

            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
            }

            boolean verified = verifySha256(dest, expectedHash);
            if (!verified) {
                dest.delete();
                return false;
            }
            dest.setExecutable(true);
            return true;
        } catch (Throwable ignored) {
            if (dest.exists()) dest.delete();
            return false;
        }
    }

    private static void extractResourceToDir(File dir, String resourcePath, String fileName) {
        File outFile = new File(dir, fileName);
        String resolvedPath = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        try (InputStream in = NativeLoader.class.getResourceAsStream(resolvedPath)) {
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
                        Enumeration<JarEntry> entries = jar.entries();
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

    private static final Set<String> KNOWN_LIBS = Set.of(
        "libravex_optimize.so", "libravex_manager.so", "libravex_github_tools.so",
        "libravex_jni.so", "libravex_mediaquery.so",
        "libravex_autocrystal.so", "libravex_trap.so", "libravex_nametags.so",
        "libravex_fakepearl.so", "libravex_selftrap.so", "libravex_autoclicker.so",
        "libravex_antibot.so", "libravex_autodrop.so", "libravex_burrow.so",
        "libravex_animations.so", "libravex_autoregear.so", "libravex_antiregear.so",
        "libravex_tntaura.so", "libravex_elytraplusplus.so", "libravex_desktopgui.so",
        "libravex_calculator.so", "libravex_pearltarget.so", "libravex_antipearl.so",
        "libravex_bedbomb.so", "libravex_ecfarmer.so", "libravex_shieldfucker.so",
        "libravex_chunkexploit.so", "libravex_addon.so", "libravex_loader.so",
        "libravex_fastexp.so", "libravex_nativesc.so", "libravex_fileprot.so",
        "libravex_baseplace.so", "libravex_anchoraura.so", "libravex_breaker.so",
        "libravex_bowaim.so", "libravex_quiver.so", "libravex_nuker.so",
        "libravex_holefill.so", "libravex_packetmine.so", "libravex_phase.so",
        "libravex_nopacktkick.so", "libravex_antiquit.so", "libravex_safewalk.so",
        "libravex_selffill.so", "libravex_noslow.so", "libravex_dc.so",
        "libravex_shaders_native.so"
    );

    private static final Set<String> BROKEN_LIBS = Set.of("libravex_autocrystal.so");

    private static void preloadAllLibraries(File dir) {
        if (isWindows()) return;
        for (String lib : KNOWN_LIBS) {
            if (BROKEN_LIBS.contains(lib)) continue;
            File f = new File(dir, lib);
            if (f.exists()) {
                try {
                    System.load(f.getAbsolutePath());
                } catch (Throwable t) {
                    System.err.println("[RaveX] Failed to pre-load " + lib + ": " + t.getMessage());
                }
            }
        }
    }

    private static File obtainLibrary(String name) {
        boolean isWin = isWindows();
        String fileName = isWin ? name + ".dll" : "lib" + name + ".so";

        File cached = getCachedFile(fileName);
        if (cached != null) return cached;

        File cacheDir = getCacheDir();
        if (!cacheDir.exists()) cacheDir.mkdirs();

        File dest = new File(cacheDir, fileName);
        if (downloadFromGitHub(fileName, dest)) return dest;

        extractAllNatives(cacheDir);
        cached = getCachedFile(fileName);
        if (cached != null) return cached;

        try {
            InputStream in = NativeLoader.class.getResourceAsStream("/assets/ravex/natives/" + fileName);
            if (in != null) {
                String prefix = isWin ? name : "lib" + name;
                String suffix = isWin ? ".dll" : ".so";
                File tempFile = File.createTempFile(prefix, suffix);
                tempFile.deleteOnExit();
                try (FileOutputStream out = new FileOutputStream(tempFile)) {
                    byte[] buf = new byte[8192];
                    int read;
                    while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
                }
                String expectedHash = NATIVE_MANIFEST.get(fileName);
                if (expectedHash != null && verifySha256(tempFile, expectedHash)) {
                    return tempFile;
                }
            }
        } catch (Throwable ignored) {}

        return null;
    }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;

        if (isNativeBlockedByGlibc()) {
            System.err.println("[RaveX] Glibc 2.43+ detected — native libs disabled to avoid dlopen crash. If you see this, please report it on our Discord: https://discord.gg/n9HPbgN7S");
            return;
        }

        try {
            System.loadLibrary("ravex_jni");
            nativeAvailable = true;
            return;
        } catch (UnsatisfiedLinkError ignored) {}

        try {
            if (!isWindows()) {
                ensureJawtLoaded();
                String[] deps = {"optimize", "manager", "github_tools"};
                for (String dep : deps) {
                    File depFile = obtainLibrary(dep);
                    if (depFile != null) {
                        try {
                            System.load(depFile.getAbsolutePath());
                        } catch (Throwable t) {
                            System.err.println("[RaveX] Failed to pre-load " + dep + ": " + t.getMessage());
                        }
                    }
                }
            }

            File jniFile = obtainLibrary("ravex_jni");
            if (jniFile != null) {
                System.load(jniFile.getAbsolutePath());
                nativeAvailable = true;
                return;
            }

            System.err.println("[RaveX] JNI native library could not be loaded/downloaded: " + getLibName() + ". If you see this, please report it on our Discord: https://discord.gg/n9HPbgN7S");
        } catch (Throwable ex) {
            System.err.println("[RaveX] WARNING: Failed to dynamically load native library: " + ex.getMessage() + ". If you see this, please report it on our Discord: https://discord.gg/n9HPbgN7S");
        }
    }

    public static synchronized boolean loadLibrary(String name) {
        try {
            System.loadLibrary(name);
            return true;
        } catch (UnsatisfiedLinkError ignored) {}

        String libFileName = (isWindows() ? "" : "lib") + name + (isWindows() ? ".dll" : ".so");
        if (BROKEN_LIBS.contains(libFileName)) {
            System.err.println("[RaveX] Native library " + name + " disabled due to system compatibility. If you see this, please report it on our Discord: https://discord.gg/n9HPbgN7S");
            return false;
        }

        try {
            ensureJawtLoaded();
            File libFile = obtainLibrary(name);
            if (libFile != null) {
                try {
                    System.load(libFile.getAbsolutePath());
                    return true;
                } catch (UnsatisfiedLinkError e) {
                    System.err.println("[RaveX] Native library " + name + " unavailable: " + e.getMessage() + ". If you see this, please report it on our Discord: https://discord.gg/n9HPbgN7S");
                }
            }
        } catch (Throwable ex) {
            System.err.println("[RaveX] Failed to load native library " + name + ": " + ex.getMessage() + ". If you see this, please report it on our Discord: https://discord.gg/n9HPbgN7S");
        }
        return false;
    }

    public static boolean isNativeAvailable() {
        return nativeAvailable;
    }
}