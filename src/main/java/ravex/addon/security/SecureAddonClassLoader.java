package ravex.addon.security;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.*;
import java.util.*;

public class SecureAddonClassLoader extends URLClassLoader {
    private static final Set<String> DENIED_PACKAGES = new HashSet<>(Arrays.asList(
        "java.lang.reflect",
        "java.lang.invoke",
        "java.lang.management",
        "java.lang.ProcessBuilder",
        "java.lang.Runtime",
        "java.lang.System",
        "java.io.FileOutputStream",
        "java.io.FileInputStream",
        "java.io.RandomAccessFile",
        "java.io.FileWriter",
        "java.io.FileReader",
        "java.net.Socket",
        "java.net.ServerSocket",
        "java.net.HttpURLConnection",
        "java.net.URLConnection",
        "java.security",
        "javax.crypto",
        "javax.script",
        "java.lang.reflect.Proxy",
        "sun.misc",
        "sun.reflect",
        "jdk.internal"
    ));

    private static final Set<String> ALLOWED_PACKAGES = new HashSet<>(Arrays.asList(
        "java.lang",
        "java.util",
        "java.math",
        "java.text",
        "java.io.String",
        "ravex.addon",
        "com.google.gson",
        "org.slf4j",
        "org.apache.logging",
        "net.minecraft"
    ));

    private final String addonName;
    private final File addonDir;

    public SecureAddonClassLoader(String name, URL[] urls, ClassLoader parent, File addonDir) {
        super(urls, parent);
        this.addonName = name;
        this.addonDir = addonDir;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        checkPackageAccess(name);
        return super.loadClass(name, resolve);
    }

    private void checkPackageAccess(String className) throws ClassNotFoundException {
        if (className == null || className.startsWith("ravex.addon.security")) return;
        if (className.startsWith("java.") || className.startsWith("javax.")) {
            for (String denied : DENIED_PACKAGES) {
                if (className.equals(denied) || className.startsWith(denied + ".")) {
                    throw new ClassNotFoundException("SECURITY: " + addonName +
                        " cannot access " + className);
                }
            }
        }
    }

    public static boolean isPathAllowed(File file, File addonDir) {
        try {
            String filePath = file.getCanonicalPath();
            String addonPath = addonDir.getCanonicalPath();
            return filePath.startsWith(addonPath);
        } catch (Exception e) {
            return false;
        }
    }
}
