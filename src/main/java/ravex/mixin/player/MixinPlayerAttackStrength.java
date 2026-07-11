package ravex.mixin.player;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.render.SwingAnimation;

@Mixin(Player.class)
public class MixinPlayerAttackStrength {

    @Inject(method = "getAttackStrengthScale", at = @At("HEAD"), cancellable = true)
    private void onGetAttackStrengthScale(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (SwingAnimation.maybeEnabled()) {
            String mode = SwingAnimation.itz().mode.getValue();
            if ("Default".equals(mode) || "Akrien".equals(mode)) {
                cir.setReturnValue(1.0f);
            }
        }
    }
}
