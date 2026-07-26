package ravex.modules.combat;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.rotation.AimUtility;
import ravex.utility.player.rotation.RotationUtility;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.EntityUtility;

import net.minecraft.world.item.BowItem;
import ravex.utility.misc.MobUtility;
@ModuleInfo(name = "AimAssist", category = "Combat")
public class AimAssist implements ModuleAccess {
    @Parameter(name = "Target", modes = {"Players", "Monsters", "All"})
    public String targetMode = "Players";
    @Parameter(name = "FOV", min = 10.0, max = 180.0, step = 5.0)
    public double fov = 45.0;
    @Parameter(name = "Speed", min = 1.0, max = 20.0, step = 0.5)
    public double speed = 5.0;
    @Parameter(name = "BowOnly")
    public boolean bowOnly = false;
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (bowOnly && !(mc.player.getMainHandItem().getItem() instanceof BowItem)) {
            return;
        }
        net.minecraft.world.entity.LivingEntity target = null;
        double bestDist = Double.MAX_VALUE;
        for (net.minecraft.world.entity.Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof net.minecraft.world.entity.LivingEntity p)) continue;
            if (MobUtility.isSelf(p) || !MobUtility.isAlive(p)) continue;
            String mode = targetMode;
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
            if (Math.abs(diffYaw) < fov) {
                float speedVal = (float) speed;
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


}