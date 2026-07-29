package ravex.mixin.movement;

import net.minecraft.world.entity.LivingEntity;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.FreeCam;
import ravex.modules.Modules;

@Mixin(LivingEntity.class)
public abstract class MixinFreeCamPlayer {

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void onTravel(Vec3 travelVector, CallbackInfo ci) {
        if (Modules.enabled(FreeCam.class)) {
            var mc = MinecraftWrapper.getInstance();
            if (mc.player != null && (Object) this == mc.player) {
                ci.cancel();
            }
        }
    }
}
