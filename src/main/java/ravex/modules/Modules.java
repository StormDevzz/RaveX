package ravex.modules;

import ravex.manager.ModuleManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Modules {
    public static boolean enabled(@NotNull Class<?> type) {
        return ModuleManager.isEnabled(type);
    }

    @Nullable
    public static <T> T get(@NotNull Class<T> type) {
        return ModuleManager.get(type);
    }

    public static void setEnabled(@NotNull Class<?> type, boolean value) {
        Module m = ModuleManager.delegate(type);
        if (m != null) m.setEnabled(value);
    }
}
