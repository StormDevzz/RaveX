package ravex.mixin.render;

import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.movement.Speed;

@Mixin(DeltaTracker.Timer.class)
public class MixinDeltaTrackerTimer {
    @Shadow private float deltaTicks;

    @Inject(method = "advanceGameTime", at = @At(value = "FIELD", target = "Lnet/minecraft/client/DeltaTracker$Timer;lastMs:J", ordinal = 1, shift = At.Shift.BEFORE))
    private void onAdvanceGameTime(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        float mt = Speed.matrixTimer;
        if (mt != 1.0f) {
            this.deltaTicks *= mt;
        }
    }
}
