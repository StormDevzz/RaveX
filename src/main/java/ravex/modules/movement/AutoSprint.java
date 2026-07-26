package ravex.modules.movement;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.ModeParameter;
import net.minecraft.client.Minecraft;
import java.util.List;
@ModuleInfo(name = "AutoSprint", category = "Movement")
public class AutoSprint extends ravex.modules.Module {
public final ModeParameter mode = new ModeParameter("Mode", "Rage", List.of("Legit", "Rage"));
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if ("Rage".equals(mode.getValue())) {
            mc.player.setSprinting(true);
        } else {
            if (mc.player.input.hasForwardImpulse() && !mc.player.isUsingItem() && !mc.player.isShiftKeyDown()) {
                mc.player.setSprinting(true);
            }
        }
    }
    public static AutoSprint itz() {
        return ravex.manager.ModuleManager.delegate(AutoSprint.class);
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