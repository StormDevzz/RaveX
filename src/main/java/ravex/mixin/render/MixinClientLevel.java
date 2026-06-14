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
        if (level.isClientSide() && Weather.INSTANCE.getEnabled()) {
            String mode = Weather.INSTANCE.mode.getValue();
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
        if (level.isClientSide() && Weather.INSTANCE.getEnabled()) {
            String mode = Weather.INSTANCE.mode.getValue();
            if ("Thunder".equals(mode)) {
                cir.setReturnValue(1.0f);
            } else if ("Clear".equals(mode) || "Rain".equals(mode) || "Snow".equals(mode)) {
                cir.setReturnValue(0.0f);
            }
        }
    }
}
