package ravex.modules;

import ravex.manager.ModuleManager;
import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.gui.GuiGraphics;

public interface ModuleAccess {
    default Module self() {
        ModuleInfo info = getClass().getAnnotation(ModuleInfo.class);
        if (info == null) return null;
        return ModuleManager.INSTANCE.getByName(info.name());
    }

    default Module getModule(String name) {
        return ModuleManager.INSTANCE.getByName(name);
    }

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

