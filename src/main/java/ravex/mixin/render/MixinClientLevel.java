package ravex.mixin.render;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.render.Weather;

@Mixin(Level.class)
public class MixinClientLevel {
    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void onGetRainLevel(float f, CallbackInfoReturnable<Float> cir) {
        Level level = (Level) (Object) this;
<<<<<<< HEAD
        if (level.isClientSide() && Weather.maybeEnabled()) {
            String mode = Weather.itz().mode.getValue();
=======
        if (level.isClientSide() && Weather.INSTANCE.getEnabled()) {
            String mode = Weather.INSTANCE.mode.getValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if ("Rain".equals(mode) || "Snow".equals(mode) || "Thunder".equals(mode)) {
                cir.setReturnValue(1.0f);
            } else if ("Clear".equals(mode)) {
                cir.setReturnValue(0.0f);
            }
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void onGetThunderLevel(float f, CallbackInfoReturnable<Float> cir) {
        Level level = (Level) (Object) this;
<<<<<<< HEAD
        if (level.isClientSide() && Weather.maybeEnabled()) {
            String mode = Weather.itz().mode.getValue();
=======
        if (level.isClientSide() && Weather.INSTANCE.getEnabled()) {
            String mode = Weather.INSTANCE.mode.getValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if ("Thunder".equals(mode)) {
                cir.setReturnValue(1.0f);
            } else if ("Clear".equals(mode) || "Rain".equals(mode) || "Snow".equals(mode)) {
                cir.setReturnValue(0.0f);
            }
        }
    }
}
