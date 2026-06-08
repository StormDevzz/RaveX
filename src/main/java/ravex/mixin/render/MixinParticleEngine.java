package ravex.mixin.render;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.render.NoRender;

import java.util.Map;

@Mixin(ParticleEngine.class)
public class MixinParticleEngine {
    @Shadow private java.util.Map<ParticleRenderType, ParticleGroup<?>> particles;

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void onCreateParticle(ParticleOptions options, double x, double y, double z, double dx, double dy, double dz, CallbackInfoReturnable<Particle> cir) {
        if (NoRender.INSTANCE.getEnabled()) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            net.minecraft.world.entity.Entity cam = mc.getCameraEntity();
            if (cam != null) {
                double camX = cam.getX();
                double camY = cam.getY();
                double camZ = cam.getZ();
                if (NoRender.shouldCull(x, y, z, camX, camY, camZ, 16.0)) {
                    cir.setReturnValue(null);
                    return;
                }
            }

            if (NoRender.INSTANCE.blockParticles.getValue() && (
                options.getType() == ParticleTypes.BLOCK ||
                options.getType() == ParticleTypes.BLOCK_MARKER ||
                options.getType() == ParticleTypes.BLOCK_CRUMBLE ||
                options.getType() == ParticleTypes.FALLING_DUST
            )) {
                cir.setReturnValue(null);
            } else if (NoRender.INSTANCE.explosions.getValue() && (
                options.getType() == ParticleTypes.EXPLOSION ||
                options.getType() == ParticleTypes.EXPLOSION_EMITTER
            )) {
                cir.setReturnValue(null);
            } else if (NoRender.INSTANCE.sprint.getValue() && options.getType() == ParticleTypes.CLOUD) {
                cir.setReturnValue(null);
            }
        }
    }

    private int ravex$getActiveParticles() {
        int sum = 0;
        if (this.particles != null) {
            for (ParticleGroup<?> group : this.particles.values()) {
                if (group != null) {
                    sum += group.size();
                }
            }
        }
        return sum;
    }

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void onAdd(Particle particle, CallbackInfo ci) {
        if (NoRender.INSTANCE.getEnabled()) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            int currentFps = mc.getFps();
            int active = ravex$getActiveParticles();
            int allowed = NoRender.optimizeBudget(active, currentFps, 60);
            if (active >= allowed && allowed < active) {
                ci.cancel();
            }
        }
    }
}
