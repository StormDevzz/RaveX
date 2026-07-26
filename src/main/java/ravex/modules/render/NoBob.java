package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
@ModuleInfo(name = "NoBob", category = "Render")
public class NoBob extends ravex.modules.Module {
private boolean originalBob = true;
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            originalBob = mc.options.bobView().get();
            mc.options.bobView().set(false);
        }
    }
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        if (mc.options.bobView().get()) {
            mc.options.bobView().set(false);
        }
    }
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        mc.options.bobView().set(originalBob);
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("NoBob").getEnabled();
    }

    public static NoBob itz() {
        return ravex.manager.ModuleManager.delegate(NoBob.class);
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