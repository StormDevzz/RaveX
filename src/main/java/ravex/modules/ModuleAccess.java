package ravex.modules;

import ravex.manager.ModuleManager;
import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public interface ModuleAccess {
    @Contract(pure = true)
    @Nullable
    default Module self() {
        ModuleInfo info = getClass().getAnnotation(ModuleInfo.class);
        if (info == null || ModuleManager.INSTANCE == null) return null;
        return ModuleManager.INSTANCE.getByName(info.name());
    }

    @Contract(pure = true)
    @Nullable
    default Module getModule(String name) {
        if (ModuleManager.INSTANCE == null) return null;
        return ModuleManager.INSTANCE.getByName(name);
    }

    @Contract(pure = true)
    default boolean getEnabled() {
        Module s = self();
        return s != null && s.getEnabled();
    }

    default void setEnabled(boolean value) {
        Module s = self();
        if (s != null) s.setEnabled(value);
    }

    default void toggle() {
        Module s = self();
        if (s != null) s.toggle();
    }

    default void onEnable() {}
    default void onDisable() {}
    default void onTick() {}
    default void render(GuiGraphics graphics, float partialTicks) {}
}

