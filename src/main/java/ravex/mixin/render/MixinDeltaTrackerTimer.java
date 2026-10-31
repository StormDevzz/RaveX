package ravex.mixin.render;

import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.movement.Speed;
import ravex.modules.movement.Timer;
import ravex.modules.Modules;

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

    @Redirect(method = "advanceGameTime", at = @At(value = "INVOKE",
        target = "Lit/unimi/dsi/fastutil/floats/FloatUnaryOperator;apply(F)F", remap = false))
    private float ravexTimerMspt(it.unimi.dsi.fastutil.floats.FloatUnaryOperator provider, float mspt) {
        float base = provider.apply(mspt);
        if (Modules.enabled(Timer.class)) {
            double mult = Modules.get(Timer.class).getEffectiveSpeed();
            if (mult > 0.05 && mult != 1.0) {
                return (float) (base / mult);
            }
        }
        return base;
    }
}