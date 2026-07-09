package ravex.mixin.player;

import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.misc.PingSpoof;

@Mixin(PlayerInfo.class)
public class MixinPlayerInfo {

    @Inject(method = "getLatency", at = @At("RETURN"), cancellable = true)
    private void onGetLatency(CallbackInfoReturnable<Integer> cir) {
<<<<<<< HEAD
        int spoofed = PingSpoof.itz().getSpoofedPing();
=======
        int spoofed = PingSpoof.INSTANCE.getSpoofedPing();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (spoofed >= 0) {
            cir.setReturnValue(spoofed);
        }
    }
}
