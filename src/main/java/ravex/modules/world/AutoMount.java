package ravex.modules.world;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.MobUtility;
import ravex.utility.player.SwingUtility;
@ModuleInfo(name = "AutoMount", category = "World")
public class AutoMount extends ravex.modules.Module {
public final ravex.parameter.ModeParameter mode = new ravex.parameter.ModeParameter("Mode", "Normal", java.util.List.of("Normal", "Fast"));
    private int cooldown = 0;
    public void onTick() {
        if ("Normal".equals(mode.getValue()) && cooldown > 0) {
            cooldown--;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) return;
        if (p.getVehicle() != null) {
            return;
        }
        var target = (net.minecraft.world.entity.Entity) null;
        double closestDist = 4.5;
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity.isAlive() && entity != p) {
                if (MobUtility.isMountable(entity) && !MobUtility.isVehicle(entity)) {
                    double dist = p.distanceTo(entity);
                    if (dist < closestDist) {
                        closestDist = dist;
                        target = entity;
                    }
                }
            }
        }
        if (target != null) {
            MobUtility.interact(mc, target);
            SwingUtility.swingMainHand(p);
            cooldown = 20;
        }
    }
    public static AutoMount itz() {
        return ravex.manager.ModuleManager.delegate(AutoMount.class);
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