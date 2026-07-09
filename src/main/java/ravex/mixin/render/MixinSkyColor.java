package ravex.mixin.render;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.SkyColor;

@Mixin(SkyRenderer.class)
public class MixinSkyColor {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractRenderState(ClientLevel level, float partialTick, Camera camera,
                                      SkyRenderState state, CallbackInfo ci) {
<<<<<<< HEAD
        if (!SkyColor.maybeEnabled()) return;
        state.skyColor = SkyColor.itz().skyColor.getValue();
=======
        if (!SkyColor.INSTANCE.getEnabled()) return;
        state.skyColor = SkyColor.INSTANCE.skyColor.getValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        state.sunriseAndSunsetColor = 0;
        state.starBrightness = 0.0f;
        state.rainBrightness = 0.0f;
    }
}
