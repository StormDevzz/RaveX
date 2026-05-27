package ravex.mixin.movement;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.movement.Velocity;

/**
 * Velocity — modifies knockback received by the local player.
 * Injects at the TAIL of knockback() and scales deltaMovement accordingly.
 *
 * Modes:
 *   Cancel — zeros horizontal, keeps a configured fraction of vertical
 *   Matrix  — reduces with slight noise (bypass Matrix AC)
 *   NCP     — reduces horizontal only, full vertical (bypass NCP)
 */
@Mixin(LivingEntity.class)
public abstract class MixinVelocity {

    @Inject(method = "knockback", at = @At("TAIL"))
    private void afterKnockback(double strength, double x, double z, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!Velocity.INSTANCE.getEnabled()) return;
        if (self != Minecraft.getInstance().player) return;

        Vec3 cur = self.getDeltaMovement();
        String mode = Velocity.INSTANCE.mode.getValue();
        double h = Velocity.INSTANCE.horizontal.getValue();
        double v = Velocity.INSTANCE.vertical.getValue();

        switch (mode) {
            case "Cancel" -> {
                // Full horizontal cancel, slight vertical retain
                self.setDeltaMovement(0.0, cur.y * 0.0, 0.0);
            }
            case "Matrix" -> {
                // Reduce + slight random noise to avoid pattern detection
                double noise = (Math.random() - 0.5) * 0.015;
                self.setDeltaMovement(
                    cur.x * h + noise,
                    cur.y * v,
                    cur.z * h + noise
                );
            }
            case "NCP" -> {
                // Reduce horizontal, keep full vertical (NCP checks upward motion)
                self.setDeltaMovement(
                    cur.x * h,
                    cur.y,        // full vertical to avoid NCP flag
                    cur.z * h
                );
            }
        }
    }
}
