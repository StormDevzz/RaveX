package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.SwingUtility;
import ravex.utility.misc.EntityUtility;

import ravex.utility.misc.PhysicUtility;

import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.InventoryUtility;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "WindAura", category = "Combat")
public class WindAura {
    @Parameter(name = "Mode", modes = {"Normal", "Silent"})
    public String mode = "Normal";
    @Parameter(name = "Range", min = 3.0, max = 30.0, step = 0.5)
    public double range = 10.0;
    @Parameter(name = "Delay", min = 1.0, max = 20.0, step = 1.0)
    public double delay = 5.0;
    @Parameter(name = "Players")
    public boolean players = true;
    @Parameter(name = "AutoSwitch")
    public boolean autoSwitch = true;
    private int tickCounter = 0;
    public void onEnable() {
        tickCounter = 0;
    }
    private int findWindChargeSlot() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (InventoryUtility.isItem(InventoryUtility.getItem(mc.getPlayer(), i), "wind_charge")) return i;
        }
        return -1;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        tickCounter++;
        if (tickCounter < (int) delay) return;
        tickCounter = 0;
        double r = range;
        net.minecraft.world.entity.Entity target = null;
        double nearest = r + 1;
        for (net.minecraft.world.entity.Entity entity : mc.getLevel().entitiesForRendering()) {
            var living = EntityUtility.asLivingEntity(entity);
            if (living == null || EntityUtility.isSelf(living)) continue;
            if (!players && EntityUtility.isPlayer(living)) continue;
            double dist = EntityUtility.distanceToPlayer(entity);
            if (dist > r) continue;
            if (dist < nearest) {
                nearest = dist;
                target = entity;
            }
        }
        if (target == null) return;
        if (autoSwitch) {
            int slot = findWindChargeSlot();
            if (slot < 0) return;
            InventoryUtility.selectSlot(mc.getPlayer(), slot);
        } else {
            if (!InventoryUtility.isItem(mc.getPlayer().getMainHandItem(), "wind_charge")) return;
        }
        float[] angles = RotationUtility.anglesTo(mc.getPlayer().getEyePosition(), target.getBoundingBox().getCenter());
        float yaw = angles[0], pitch = angles[1];
        if (mode.equals("Silent")) {
            float oldYaw = mc.getPlayer().getYRot();
            float oldPitch = mc.getPlayer().getXRot();
            mc.getPlayer().setYRot(yaw); mc.getPlayer().setXRot(pitch);
            mc.getGameMode().useItem(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
            mc.getPlayer().setYRot(oldYaw); mc.getPlayer().setXRot(oldPitch);
        } else {
            mc.getPlayer().setYRot(yaw); mc.getPlayer().setXRot(pitch);
            mc.getGameMode().useItem(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }




}
