package ravex.mixin.movement;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.FreeCam;

@Mixin(LivingEntity.class)
public abstract class MixinFreeCamNoSwing {

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;)V", at = @At("HEAD"), cancellable = true)
    private void onSwing(InteractionHand hand, CallbackInfo ci) {
        if (FreeCam.maybeEnabled() && FreeCam.itz().noSwing.getValue()) {
            ci.cancel();
        }
    }
}
