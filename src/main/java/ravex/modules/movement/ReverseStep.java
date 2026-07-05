package ravex.modules.movement;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
public class ReverseStep extends Module {
    public static final ReverseStep INSTANCE = new ReverseStep();
    public final NumberParameter force = new NumberParameter("Force", 1.5, 1.0, 4.0, 0.5);

    @Override
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
            mc.player.setDeltaMovement(motion.x, -force.getValue(), motion.z);
        }
    }
}
