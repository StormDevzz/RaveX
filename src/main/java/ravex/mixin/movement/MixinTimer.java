package ravex.mixin.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.movement.Timer;
import ravex.modules.Modules;

@Mixin(LocalPlayer.class)
public class MixinTimer {

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void onAiStep(CallbackInfo ci) {
        if (!Modules.enabled(Timer.class)) return;

        LocalPlayer player = (LocalPlayer)(Object)this;
        float multiplier = (float) Modules.get(Timer.class).getEffectiveSpeed();

        if (multiplier == 1.0f) return;

        Vec3 motion = player.getDeltaMovement();
        double horizontalFactor = Math.sqrt(multiplier);
        player.setDeltaMovement(
            motion.x * horizontalFactor,
            motion.y,
            motion.z * horizontalFactor
        );
    }
}
