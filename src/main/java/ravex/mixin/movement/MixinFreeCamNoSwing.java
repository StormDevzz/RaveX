package ravex.mixin.movement;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.FreeCam;
import ravex.modules.Modules;

@Mixin(LivingEntity.class)
public abstract class MixinFreeCamNoSwing {

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;)V", at = @At("HEAD"), cancellable = true)
    private void onSwing(InteractionHand hand, CallbackInfo ci) {
        if (Modules.enabled(FreeCam.class) && Modules.get(FreeCam.class).noSwing) {
            ci.cancel();
        }
    }
}
