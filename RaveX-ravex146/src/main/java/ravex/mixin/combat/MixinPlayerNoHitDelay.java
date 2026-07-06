package ravex.mixin.combat;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.combat.NoHitDelay;

@Mixin(Player.class)
public abstract class MixinPlayerNoHitDelay {

    @Inject(method = "getAttackStrengthScale", at = @At("RETURN"), cancellable = true)
    private void onGetAttackStrengthScale(float baseTime, CallbackInfoReturnable<Float> cir) {
        if (NoHitDelay.INSTANCE.getEnabled() && NoHitDelay.INSTANCE.alwaysFull.getValue()) {
            cir.setReturnValue(1.0F);
        }
    }
}
