package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.rotation.AimUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.misc.EntityUtility;

import net.minecraft.world.item.BowItem;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "AimAssist", category = "Combat")
public class AimAssist {
    @Parameter(name = "Target", modes = {"Players", "Monsters", "All"})
    public String targetMode = "Players";
    @Parameter(name = "FOV", min = 10.0, max = 180.0, step = 5.0)
    public double fov = 45.0;
    @Parameter(name = "Speed", min = 1.0, max = 20.0, step = 0.5)
    public double speed = 5.0;
    @Parameter(name = "BowOnly")
    public boolean bowOnly = false;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        if (bowOnly && !(mc.getPlayer().getMainHandItem().getItem() instanceof BowItem)) {
            return;
        }
        net.minecraft.world.entity.LivingEntity target = null;
        double bestDist = Double.MAX_VALUE;
        for (net.minecraft.world.entity.Entity entity : mc.getLevel().entitiesForRendering()) {
            if (!(entity instanceof net.minecraft.world.entity.LivingEntity p)) continue;
            if (EntityUtility.isSelf(p) || !EntityUtility.isAlive(p)) continue;
            String mode = targetMode;
            if (mode.equals("Players") && !EntityUtility.isPlayer(p)) continue;
            if (mode.equals("Monsters") && !EntityUtility.isHostile(p)) continue;
            double dist = EntityUtility.distanceToPlayer(p);
            if (dist < bestDist && dist <= 40.0) {
                target = p;
                bestDist = dist;
            }
        }
        if (target != null) {
            float[] angles = RotationUtility.anglesTo(mc.getPlayer(), target);
            float diffYaw = RotationUtility.diffYaw(mc.getPlayer().getYRot(), angles[0]);
            float diffPitch = RotationUtility.diffPitch(mc.getPlayer().getXRot(), angles[1]);
            if (Math.abs(diffYaw) < fov) {
                float speedVal = (float) speed;
                mc.getPlayer().setYRot(mc.getPlayer().getYRot() + (diffYaw / speedVal));
                mc.getPlayer().setXRot(mc.getPlayer().getXRot() + (diffPitch / speedVal));
            }
        }
    }




}
