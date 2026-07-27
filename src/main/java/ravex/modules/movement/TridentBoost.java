package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.PhysicUtility;

import ravex.utility.player.InventoryUtility;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "TridentBoost", category = "Movement")
public class TridentBoost {
    @Parameter(name = "Mode", modes = {"Normal", "Always"})
    public String mode = "Normal";
    @Parameter(name = "Speed", min = 0.5, max = 3.0, step = 0.1)
    public double speed = 1.0;
    @Parameter(name = "Vertical", min = 0.0, max = 2.0, step = 0.1)
    public double vertical = 0.5;
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null) return;
        var main = mc.player.getMainHandItem();
        if (!InventoryUtility.isTrident(main)) return;
        if (!InventoryUtility.hasEnchantment(main, "riptide")) return;
        if (!mc.player.isUsingItem()) return;
        String m = mode;
        if (m.equals("Normal") && !mc.player.isInWaterOrRain()) return;
        float yaw = mc.player.getYRot() * ((float)Math.PI / 180F);
        float pitch = mc.player.getXRot() * ((float)Math.PI / 180F);
        double mult = speed;
        double vert = vertical;
        double dx = -Math.sin(yaw) * Math.cos(pitch) * mult;
        double dy = -Math.sin(pitch) * vert;
        double dz = Math.cos(yaw) * Math.cos(pitch) * mult;
        mc.player.setDeltaMovement(new net.minecraft.world.phys.Vec3(dx, dy, dz));
        mc.player.hurtMarked = true;
    }




}