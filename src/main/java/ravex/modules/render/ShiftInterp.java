package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.EntityUtility;

import ravex.parameter.ModeParameter;
@ModuleInfo(name = "ShiftInterp", category = "Render")
public class ShiftInterp extends ravex.modules.Module {
public final ModeParameter target = new ModeParameter("Target", "All", java.util.List.of("All", "Others", "Self"));

    public boolean shouldCrouch(net.minecraft.world.entity.Entity entity) {
        if (!getEnabled()) return false;
        if (!(entity instanceof net.minecraft.world.entity.player.Player)) return false;
        Minecraft mc = Minecraft.getInstance();
        boolean isSelf = (entity == mc.player);
        String t = target.getValue();
        if (t.equals("Self")) {
            return isSelf;
        } else if (t.equals("Others")) {
            return !isSelf;
        } else {
            return true;
        }
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("ShiftInterp").getEnabled();
    }

    public static ShiftInterp itz() {
        return ravex.manager.ModuleManager.delegate(ShiftInterp.class);
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