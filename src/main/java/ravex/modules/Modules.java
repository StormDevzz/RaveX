package ravex.modules;

import ravex.manager.ModuleManager;

public class Modules {
    public static boolean enabled(Class<?> type) {
        return ModuleManager.isEnabled(type);
    }

    public static <T> T get(Class<T> type) {
        return ModuleManager.get(type);
    }

    public static void setEnabled(Class<?> type, boolean value) {
        Module m = ModuleManager.delegate(type);
        if (m != null) m.setEnabled(value);
    }
}
