package ravex.addon.core;

import ravex.addon.Addon;
import ravex.addon.util.AddonException;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;
import java.util.jar.Attributes;

public class AddonLoader {
    public static Addon loadAddon(File jarFile) throws AddonException {
        try (JarFile jar = new JarFile(jarFile)) {
            Attributes attrs = jar.getManifest().getMainAttributes();
            if (attrs == null) {
                throw new AddonException("No manifest attributes found in " + jarFile.getName());
            }
            String name = attrs.getValue("Addon-Name");
            String version = attrs.getValue("Addon-Version");
            String author = attrs.getValue("Addon-Author");
            String mainClass = attrs.getValue("Addon-Main-Class");

            if (name == null || version == null || mainClass == null) {
                throw new AddonException("Missing required manifest attributes in " + jarFile.getName());
            }

            AddonInfo info = new AddonInfo(name, "External Addon", version, author != null ? author : "Unknown", mainClass);
            URL[] urls = { jarFile.toURI().toURL() };
            URLClassLoader classLoader = new URLClassLoader(urls, AddonLoader.class.getClassLoader());
            
            Class<?> clazz = Class.forName(mainClass, true, classLoader);
            if (!Addon.class.isAssignableFrom(clazz)) {
                throw new AddonException("Main class does not implement Addon interface: " + mainClass);
            }

            Addon addon = (Addon) clazz.getDeclaredConstructor().newInstance();
            return addon;
        } catch (Exception e) {
            throw new AddonException("Failed to load JAR " + jarFile.getName(), e);
        }
    }
}
