package ravex.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
<<<<<<< HEAD
import ravex.modules.misc.ChatHelper;
=======
import ravex.modules.misc.CoordLogger;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayerDeath {
    @Inject(method = "setShowDeathScreen", at = @At("HEAD"))
    private void onDeathScreen(boolean show, CallbackInfo ci) {
        if (!show) return;
        var self = (LocalPlayer)(Object)this;
<<<<<<< HEAD
        ChatHelper.itz().onDeath(
=======
        CoordLogger.INSTANCE.onDeath(
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            self.getX(), self.getY(), self.getZ(),
            self.level().dimension().identifier().toString()
        );
    }
}
