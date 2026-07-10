package ravex.mixin.movement;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.movement.NoSlow;

@Mixin(LocalPlayer.class)
public abstract class MixinNoSlowDown {

    private int grimStrictTicks = 0;
    @Inject(method = "isSlowDueToUsingItem", at = @At("HEAD"), cancellable = true)
    private void onIsSlowDueToUsingItem(CallbackInfoReturnable<Boolean> cir) {
        NoSlow ns = NoSlow.itz();
        if (!ns.getEnabled() || !ns.items.getValue()) return;
        String mode = ns.mode.getValue();
        if ("GrimStrict".equals(mode) && Minecraft.getInstance().player != null && !Minecraft.getInstance().player.isUsingItem()) {
            cir.setReturnValue(false);
            return;
        }
        if ("GrimAlternative".equals(mode)) {
            cir.setReturnValue(false);
            return;
        }
        if ("GrimV3".equals(mode)) {
            if (ns.isInGrace()) return;
            cir.setReturnValue(false);
            return;
        }
        cir.setReturnValue(false);
    }

    @ModifyExpressionValue(method = "modifyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean redirectUsingItem(boolean isUsingItem) {
        NoSlow ns = NoSlow.itz();
        if (!ns.getEnabled() || !ns.items.getValue()) return isUsingItem;
        String mode = ns.mode.getValue();
        if ("GrimStrict".equals(mode)) {
            if (!isUsingItem) return false;
            grimStrictTicks++;
            if (grimStrictTicks >= 3) {
                grimStrictTicks = 0;
                return false;
            }
            return true;
        }
        if ("GrimAlternative".equals(mode)) return ns.isSlowPhase();
        if ("GrimV3".equals(mode)) return false;
        return false;
    }

    @Inject(method = "isMovingSlowly", at = @At("HEAD"), cancellable = true)
    private void onIsMovingSlowly(CallbackInfoReturnable<Boolean> cir) {
        if (NoSlow.maybeEnabled() && NoSlow.itz().sneaking.getValue()) {
            cir.setReturnValue(false);
        }
    }
}
