package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.Minecraft;
@ModuleInfo(name = "ReverseStep", category = "Movement")
public class ReverseStep implements ModuleAccess {
    @Parameter(name = "Force", min = 1.0, max = 4.0, step = 0.5)
    public double force = 1.5;
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.player.onGround() || mc.player.isPassenger() || mc.player.getAbilities().flying || mc.player.isFallFlying()) {
            return;
        }
        if (mc.options.keyJump.isDown() || mc.player.isInWater() || mc.player.isInLava() || mc.player.onClimbable()) {
            return;
        }
        double currentX = mc.player.getX();
        double currentY = mc.player.getY();
        double currentZ = mc.player.getZ();
        boolean foundGround = false;
        for (double dy = 0.0; dy <= 3.0; dy += 0.5) {
            net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(currentX, currentY - dy, currentZ);
            if (mc.level.getBlockState(pos).isSolid()) {
                foundGround = true;
                break;
            }
        }
        if (foundGround) {
            var motion = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(motion.x, -force, motion.z);
        }
    }
    public static ReverseStep itz() {
        return ravex.manager.ModuleManager.delegate(ReverseStep.class);
    }


}