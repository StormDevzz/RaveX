package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
@ModuleInfo(name = "Fullbright", category = "Render")
public class Fullbright extends ravex.modules.Module {
public final NumberParameter brightness = new NumberParameter("Brightness", 1.0, 0.0, 1.0, 0.05);
    public final NumberParameter darknessMult = new NumberParameter("DarknessMult", 0.0, 0.0, 1.0, 0.05);
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Fullbright").getEnabled();
    }

    public static Fullbright itz() {
        return ravex.manager.ModuleManager.delegate(Fullbright.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}