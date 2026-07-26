package ravex.modules.movement;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
@ModuleInfo(name = "ReverseStep", category = "Movement")
public class ReverseStep extends ravex.modules.Module {
public final NumberParameter force = new NumberParameter("Force", 1.5, 1.0, 4.0, 0.5);
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
    public static ReverseStep itz() {
        return ravex.manager.ModuleManager.delegate(ReverseStep.class);
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