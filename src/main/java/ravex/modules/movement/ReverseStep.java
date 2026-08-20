package ravex.modules.movement;
import ravex.utility.player.PlayerUtility;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.movement.MoveUtility;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "ReverseStep", category = "Movement")
public class ReverseStep {
    @Parameter(name = "Force", min = 1.0, max = 4.0, step = 0.5)
    public double force = 1.5;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) return;
        var abilities = PlayerUtility.getAbilities();
        if (PlayerUtility.isOnGround() || player.isPassenger() || (abilities != null && abilities.flying) || player.isFallFlying()) {
            return;
        }
        if (mc.isJumpKeyDown() || player.isInWater() || player.isInLava() || player.onClimbable()) {
            return;
        }
        double currentX = PlayerUtility.getX();
        double currentY = PlayerUtility.getY();
        double currentZ = PlayerUtility.getZ();
        boolean foundGround = false;
        for (double dy = 0.0; dy <= 3.0; dy += 0.5) {
            net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(currentX, currentY - dy, currentZ);
            if (BlockUtility.isSolid(mc.getLevel(), pos)) {
                foundGround = true;
                break;
            }
        }
        if (foundGround) {
            var motion = PlayerUtility.getDeltaMovement();
            if (motion != null) {
                MoveUtility.setMotion(motion.x, -force, motion.z);
            }
        }
    }
}
