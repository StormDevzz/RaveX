package ravex.mixin.render;

import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.state.WeatherRenderState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Camera;
import net.minecraft.server.level.ParticleStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.NoRender;

@Mixin(WeatherEffectRenderer.class)
public class MixinWeatherEffectRenderer {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(MultiBufferSource multiBufferSource, Vec3 vec3, WeatherRenderState weatherRenderState, CallbackInfo ci) {
        if (NoRender.INSTANCE.getEnabled() && NoRender.INSTANCE.weather.getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "tickRainParticles", at = @At("HEAD"), cancellable = true)
    private void onTickRainParticles(ClientLevel clientLevel, Camera camera, int i, ParticleStatus particleStatus, int j, CallbackInfo ci) {
        if (NoRender.INSTANCE.getEnabled() && NoRender.INSTANCE.weather.getValue()) {
            ci.cancel();
        }
    }
}
