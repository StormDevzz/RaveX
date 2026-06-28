package ravex.mixin.movement;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.movement.NoSlowDown;

@Mixin(LocalPlayer.class)
public abstract class MixinNoSlowDown {

    @Inject(method = "isSlowDueToUsingItem", at = @At("HEAD"), cancellable = true)
    private void onIsSlowDueToUsingItem(CallbackInfoReturnable<Boolean> cir) {
        if (NoSlowDown.INSTANCE.getEnabled() && NoSlowDown.INSTANCE.items.getValue()) {
            cir.setReturnValue(false);
        }
    }

    @ModifyExpressionValue(method = "modifyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean redirectUsingItem(boolean isUsingItem) {
        if (NoSlowDown.INSTANCE.getEnabled() && NoSlowDown.INSTANCE.items.getValue()) {
            return false;
        }
        return isUsingItem;
    }

    @Inject(method = "isMovingSlowly", at = @At("HEAD"), cancellable = true)
    private void onIsMovingSlowly(CallbackInfoReturnable<Boolean> cir) {
        if (NoSlowDown.INSTANCE.getEnabled() && NoSlowDown.INSTANCE.sneaking.getValue()) {
            cir.setReturnValue(false);
        }
    }
}
