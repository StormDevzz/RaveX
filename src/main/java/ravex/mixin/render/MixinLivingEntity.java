package ravex.mixin.render;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.render.SwingAnimation;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
    private void onGetCurrentSwingDuration(CallbackInfoReturnable<Integer> cir) {
        if (!SwingAnimation.maybeEnabled()) return;
        String mode = SwingAnimation.itz().mode.getValue();
        if ("Default".equals(mode) || "Akrien".equals(mode) || "Swipe".equals(mode)) {
            float speed = SwingAnimation.itz().speed.getValue().floatValue();
            int duration = Math.max(1, (int) (15.0 / speed));
            cir.setReturnValue(duration);
        }
    }
}
