package ravex.modules.combat;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.NumberParameter;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.player.rotation.AimUtility;
import ravex.utility.player.rotation.RotationUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import ravex.utility.misc.MobUtility;
@ModuleInfo(name = "AimAssist", category = "Combat")
public class AimAssist extends ravex.modules.Module {
public final ModeParameter targetMode = new ModeParameter("Target", "Players", java.util.List.of("Players", "Monsters", "All"));
    public final NumberParameter fov = new NumberParameter("FOV", 45.0, 10.0, 180.0, 5.0);
    public final NumberParameter speed = new NumberParameter("Speed", 5.0, 1.0, 20.0, 0.5);
    public final BooleanParameter bowOnly = new BooleanParameter("BowOnly", false);
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (bowOnly.getValue() && !(mc.player.getMainHandItem().getItem() instanceof BowItem)) {
            return;
        }
        LivingEntity target = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity p)) continue;
            if (MobUtility.isSelf(p) || !MobUtility.isAlive(p)) continue;
            String mode = targetMode.getValue();
            if (mode.equals("Players") && !MobUtility.isPlayer(p)) continue;
            if (mode.equals("Monsters") && !MobUtility.isHostile(p)) continue;
            double dist = MobUtility.distanceToPlayer(p);
            if (dist < bestDist && dist <= 40.0) {
                target = p;
                bestDist = dist;
            }
        }
        if (target != null) {
            float[] angles = RotationUtility.anglesTo(mc.player, target);
            float diffYaw = RotationUtility.diffYaw(mc.player.getYRot(), angles[0]);
            float diffPitch = RotationUtility.diffPitch(mc.player.getXRot(), angles[1]);
            if (Math.abs(diffYaw) < fov.getValue()) {
                float speedVal = speed.getValue().floatValue();
                mc.player.setYRot(mc.player.getYRot() + (diffYaw / speedVal));
                mc.player.setXRot(mc.player.getXRot() + (diffPitch / speedVal));
            }
        }
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("AimAssist").getEnabled();
    }
    public static AimAssist itz() {
        return ravex.manager.ModuleManager.delegate(AimAssist.class);
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