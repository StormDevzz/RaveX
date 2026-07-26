package ravex.modules.movement;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.InventoryUtility;
import java.util.List;
@ModuleInfo(name = "TridentBoost", category = "Movement")
public class TridentBoost extends ravex.modules.Module {
public final ModeParameter mode = new ModeParameter("Mode", "Normal", List.of("Normal", "Always"));
    public final NumberParameter speed = new NumberParameter("Speed", 1.0, 0.5, 3.0, 0.1);
    public final NumberParameter vertical = new NumberParameter("Vertical", 0.5, 0.0, 2.0, 0.1);
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var main = mc.player.getMainHandItem();
        if (!InventoryUtility.isTrident(main)) return;
        if (!InventoryUtility.hasEnchantment(main, "riptide")) return;
        if (!mc.player.isUsingItem()) return;
        String m = mode.getValue();
        if (m.equals("Normal") && !mc.player.isInWaterOrRain()) return;
        float yaw = mc.player.getYRot() * ((float)Math.PI / 180F);
        float pitch = mc.player.getXRot() * ((float)Math.PI / 180F);
        double mult = speed.getValue();
        double vert = vertical.getValue();
        double dx = -Math.sin(yaw) * Math.cos(pitch) * mult;
        double dy = -Math.sin(pitch) * vert;
        double dz = Math.cos(yaw) * Math.cos(pitch) * mult;
        mc.player.setDeltaMovement(new Vec3(dx, dy, dz));
        mc.player.hurtMarked = true;
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("TridentBoost").getEnabled();
    }
    public static TridentBoost itz() {
        return ravex.manager.ModuleManager.delegate(TridentBoost.class);
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