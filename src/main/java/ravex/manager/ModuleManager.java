package ravex.manager;

import ravex.event.EventBusHolder;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.modules.annotations.ModuleInfo;
import ravex.utility.system.AnnotationScannerUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                Category cat;
                try {
                    cat = Category.valueOf(info.category().toUpperCase());
                } catch (IllegalArgumentException e) {
                    cat = Category.CUSTOM;
                }
                var ctor = clazz.getDeclaredConstructor();
                ctor.setAccessible(true);
                Object instance = ctor.newInstance();
                Module module = (Module) instance;
                byClass.put(clazz, module);
                module.setCategory(cat);
                if (cat == Category.HUD) module.setHud(true);
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

    public List<Module> getByCategory(Category category) {
        List<Module> list = new ArrayList<>();
        for (Module m : modules) {
            if (m.getCategory() == category) list.add(m);
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
