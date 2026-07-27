package ravex.manager;

import ravex.event.EventBusHolder;
import ravex.module.ModuleProxy;
import ravex.modules.Module;
import ravex.modules.annotations.ModuleInfo;
import ravex.utility.system.AnnotationScannerUtility;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModuleManager {
    public static final ModuleManager INSTANCE = new ModuleManager();

    private final List<Module> modules = new ArrayList<>();
    private final Map<Class<?>, Module> byClass = new HashMap<>();

    private ModuleManager() {
        List<Class<?>> annotated = AnnotationScannerUtility.findAnnotatedClasses("ravex.modules", ModuleInfo.class);
        for (Class<?> clazz : annotated) {
            try {
                ModuleInfo info = clazz.getDeclaredAnnotation(ModuleInfo.class);
                if (info == null) continue;
                String cat = info.category();
                var ctor = clazz.getDeclaredConstructor();
                ctor.setAccessible(true);
                Object instance = ctor.newInstance();
                Module module;
                if (instance instanceof Module) {
                    module = (Module) instance;
                } else {
                    module = new ModuleProxy(instance);
                }
                byClass.put(clazz, module);
                module.setCategory(cat);
                if ("HUD".equals(cat)) module.setHud(true);
                modules.add(module);
            } catch (Exception e) {
                throw new RuntimeException("Failed to register " + clazz.getName(), e);
            }
        }
    }

    public List<Module> getClickGuiModules() {
        List<Module> list = new ArrayList<>();
        for (Module m : modules) {
            if (!m.isHud()) list.add(m);
        }
        return list;
    }

    public List<Module> getHudModules() {
        List<Module> list = new ArrayList<>();
        for (Module m : modules) {
            if (m.isHud()) list.add(m);
        }
        return list;
    }

    public List<Module> getByCategory(String category) {
        List<Module> list = new ArrayList<>();
        for (Module m : modules) {
            if (m.getCategory().equals(category)) list.add(m);
        }
        return list;
    }

    public Module getByName(String name) {
        for (Module m : modules) {
            if (m.getName().equalsIgnoreCase(name)) return m;
        }
        return null;
    }

    public void init() {
        for (Module m : modules) {
            EventBusHolder.get().subscribe(m);
        }
    }

    public List<Module> getModules() { return modules; }

    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> clazz) {
        return (T) INSTANCE.byClass.get(clazz);
    }

    public static boolean isEnabled(Class<?> clazz) {
        Module m = INSTANCE.byClass.get(clazz);
        return m != null && m.getEnabled();
    }

    @SuppressWarnings("unchecked")
    public static <T> T delegate(Class<T> clazz) {
        return (T) INSTANCE.byClass.get(clazz);
    }

    public void onTick() {
        for (Module m : modules) {
            if (!m.isHud() && m.getEnabled()) m.onTick();
        }
    }
}
