package ravex.mixin.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.FreeCam;

@Mixin(LivingEntity.class)
public abstract class MixinFreeCamPlayer {

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void onTravel(Vec3 travelVector, CallbackInfo ci) {
        if (FreeCam.maybeEnabled()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && (Object) this == mc.player) {
                ci.cancel();
            }
        }
    }
}
