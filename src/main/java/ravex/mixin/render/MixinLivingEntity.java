package ravex.mixin.render;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.render.SwingAnimation;
import ravex.modules.Modules;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
    private void onGetCurrentSwingDuration(CallbackInfoReturnable<Integer> cir) {
        if (!Modules.enabled(SwingAnimation.class)) return;
        String mode = Modules.get(SwingAnimation.class).mode;
        float speed = (float) Modules.get(SwingAnimation.class).speed;
        if ("Vanilla".equals(mode)) {
            // keep the exact vanilla duration when Speed is untouched
            if (!((Object) this instanceof net.minecraft.world.entity.player.Player)) return;
            if (Math.abs(speed - 1.0f) < 0.001f) return;
            cir.setReturnValue(Math.max(1, (int) Math.round(6.0 / speed)));
            return;
        }
        if ("Default".equals(mode) || "Akrien".equals(mode) || "Swipe".equals(mode)) {
            int duration = Math.max(1, (int) (15.0 / speed));
            cir.setReturnValue(duration);
        }
    }
}
