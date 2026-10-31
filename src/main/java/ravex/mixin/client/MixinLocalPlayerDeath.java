package ravex.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.misc.ChatHelper;
import ravex.modules.Modules;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayerDeath {
    @Inject(method = "setShowDeathScreen", at = @At("HEAD"))
    private void onDeathScreen(boolean show, CallbackInfo ci) {
        if (!show) return;
        var self = (LocalPlayer)(Object)this;
        Modules.get(ChatHelper.class).onDeath(
            self.getX(), self.getY(), self.getZ(),
            self.level().dimension().identifier().toString()
        );
    }

    // autoRespawn: no death screen, vanilla respawns right away
    @Inject(method = "shouldShowDeathScreen", at = @At("HEAD"), cancellable = true)
    private void ravexAutoRespawn(CallbackInfoReturnable<Boolean> cir) {
        if (Modules.enabled(ravex.modules.player.AutoRespawn.class)) {
            cir.setReturnValue(false);
        }
    }
}
