package ravex.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
}
