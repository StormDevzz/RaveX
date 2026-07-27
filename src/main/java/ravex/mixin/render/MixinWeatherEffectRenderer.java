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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.render.NoRender;
import ravex.modules.render.Weather;
import ravex.modules.Modules;

@Mixin(WeatherEffectRenderer.class)
public class MixinWeatherEffectRenderer {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(MultiBufferSource multiBufferSource, Vec3 vec3, WeatherRenderState weatherRenderState, CallbackInfo ci) {
        if (Modules.enabled(NoRender.class) && Modules.get(NoRender.class).weather) {
            ci.cancel();
        }
    }

    @Inject(method = "tickRainParticles", at = @At("HEAD"), cancellable = true)
    private void onTickRainParticles(ClientLevel clientLevel, Camera camera, int i, ParticleStatus particleStatus, int j, CallbackInfo ci) {
        if (Modules.enabled(NoRender.class) && Modules.get(NoRender.class).weather) {
            ci.cancel();
        }
    }

    @Inject(method = "getPrecipitationAt", at = @At("HEAD"), cancellable = true)
    private void onGetPrecipitationAt(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, CallbackInfoReturnable<net.minecraft.world.level.biome.Biome.Precipitation> cir) {
        if (Modules.enabled(Weather.class)) {
            String modeValue = Modules.get(Weather.class).mode;
            if ("Rain".equals(modeValue)) {
                cir.setReturnValue(net.minecraft.world.level.biome.Biome.Precipitation.RAIN);
            } else if ("Snow".equals(modeValue)) {
                cir.setReturnValue(net.minecraft.world.level.biome.Biome.Precipitation.SNOW);
            } else if ("Clear".equals(modeValue)) {
                cir.setReturnValue(net.minecraft.world.level.biome.Biome.Precipitation.NONE);
            }
        }
    }
}
