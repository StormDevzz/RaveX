package ravex.utility.system;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class AnnotationScannerUtility {
    private AnnotationScannerUtility() {}

    public static List<Class<?>> findAnnotatedClasses(String packageName, Class<? extends Annotation> annotation) {
        List<Class<?>> result = new ArrayList<>();
        String path = packageName.replace('.', '/');
        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                scanResource(resource, packageName, annotation, result);
            }
        } catch (IOException ignored) {}
        return result;
    }

    private static void scanResource(URL resource, String packageName, Class<? extends Annotation> annotation, List<Class<?>> result) {
        String protocol = resource.getProtocol();
        if ("file".equals(protocol)) {
            try {
                File dir = new File(URLDecoder.decode(resource.toURI().getPath(), StandardCharsets.UTF_8));
                scanDirectory(dir, packageName, annotation, result);
            } catch (Exception ignored) {}
        } else if ("jar".equals(protocol)) {
            scanJar(resource, packageName, annotation, result);
        }
    }

    private static void scanDirectory(File dir, String packageName, Class<? extends Annotation> annotation, List<Class<?>> result) {
        if (!dir.exists() || !dir.isDirectory()) return;
        try {
            Files.walk(dir.toPath(), FileVisitOption.FOLLOW_LINKS)
                .filter(p -> p.toString().endsWith(".class"))
                .forEach(p -> {
                    String relative = dir.toPath().relativize(p).toString();
                    String className = packageName + "." + relative.replace(File.separator, ".").replace(".class", "");
                    tryRegisterClass(className, annotation, result);
                });
        } catch (IOException ignored) {}
    }

    private static void scanJar(URL resource, String packageName, Class<? extends Annotation> annotation, List<Class<?>> result) {
        String jarPath = resource.getPath();
        int bang = jarPath.indexOf("!/");
        if (bang < 0) return;
        String jarFile = jarPath.substring(5, bang);
        String prefix = packageName.replace('.', '/') + "/";
        try {
            try (JarFile jf = new JarFile(URLDecoder.decode(jarFile, StandardCharsets.UTF_8))) {
                Enumeration<JarEntry> entries = jf.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(prefix) && name.endsWith(".class")) {
                        String className = name.replace('/', '.').replace(".class", "");
                        tryRegisterClass(className, annotation, result);
                    }
                }
            }
        } catch (IOException ignored) {}
    }

    private static void tryRegisterClass(String className, Class<? extends Annotation> annotation, List<Class<?>> result) {
        try {
            Class<?> clazz = Class.forName(className);
            if (clazz.isAnnotationPresent(annotation) && !clazz.isInterface() && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                result.add(clazz);
            }
        } catch (Exception | LinkageError ignored) {}
    }
}
