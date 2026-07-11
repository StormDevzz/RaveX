package ravex.mixin.render;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.player.Swing;
import ravex.modules.render.SwingAnimation;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
    private void onGetCurrentSwingDuration(CallbackInfoReturnable<Integer> cir) {
        if (SwingAnimation.maybeEnabled()) {
            float speed = SwingAnimation.itz().speed.getValue().floatValue();
            int duration = Math.max(1, (int) (12.0 / speed));
            cir.setReturnValue(duration);
            return;
        }

        if (Swing.maybeEnabled()) {
            String mode = Swing.itz().mode.getValue();
            int duration = switch (mode) {
                case "1.8" -> 6;
                case "1.12.2" -> 8;
                case "Custom" -> Swing.itz().duration.getValue().intValue();
                default -> 6;
            };
            cir.setReturnValue(duration);
        }
    }
}
