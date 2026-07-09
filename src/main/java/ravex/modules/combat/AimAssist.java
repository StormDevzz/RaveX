package ravex.modules.combat;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.player.rotation.AimUtility;
import ravex.utility.player.rotation.RotationUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
<<<<<<< HEAD
import ravex.utility.misc.MobUtility;
public class AimAssist extends Module {
=======
public class AimAssist extends Module {
    public static final AimAssist INSTANCE = new AimAssist();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final ModeParameter targetMode = new ModeParameter("Target", "Players", java.util.List.of("Players", "Monsters", "All"));
    public final NumberParameter fov = new NumberParameter("FOV", 45.0, 10.0, 180.0, 5.0);
    public final NumberParameter speed = new NumberParameter("Speed", 5.0, 1.0, 20.0, 0.5);
    public final BooleanParameter bowOnly = new BooleanParameter("BowOnly", false);

    @Override
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
<<<<<<< HEAD
            if (MobUtility.isSelf(p) || !MobUtility.isAlive(p)) continue;
            String mode = targetMode.getValue();
            if (mode.equals("Players") && !MobUtility.isPlayer(p)) continue;
            if (mode.equals("Monsters") && !MobUtility.isHostile(p)) continue;
            double dist = MobUtility.distanceToPlayer(p);
=======
            if (p == mc.player || !p.isAlive()) continue;
            String mode = targetMode.getValue();
            if (mode.equals("Players") && !(p instanceof Player)) continue;
            if (mode.equals("Monsters") && !(p instanceof Monster)) continue;
            double dist = mc.player.distanceTo(p);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
        return maybeEnabled(AimAssist.class);
    }
    public static AimAssist itz() {
        return ModuleManager.get(AimAssist.class);
    }

}