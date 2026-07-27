package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.EntityUtility;
import ravex.utility.misc.PhysicUtility;

import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
@ModuleInfo(name = "RidingHelper", category = "Movement")
public class RidingHelper implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"Normal", "Custom"})
    public String mode = "Normal";
    @Parameter(name = "Speed", min = 1.0, max = 5.0, step = 0.1)
    public double speed = 2.0;
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null) return;
        net.minecraft.world.entity.Entity vehicle = mc.player.getVehicle();
        if (vehicle == null) return;
        double mult = mode.equals("Custom") ? speed : 2.0;
        net.minecraft.world.phys.Vec3 motion = vehicle.getDeltaMovement();
        vehicle.setDeltaMovement(motion.x * mult, motion.y, motion.z * mult);
    }
    public static RidingHelper itz() {
        return ravex.manager.ModuleManager.delegate(RidingHelper.class);
    }


}