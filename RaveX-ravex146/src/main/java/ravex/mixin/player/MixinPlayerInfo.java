package ravex.mixin.player;

import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.exploit.PingSpoof;

@Mixin(PlayerInfo.class)
public class MixinPlayerInfo {

    @Inject(method = "getLatency", at = @At("RETURN"), cancellable = true)
    private void onGetLatency(CallbackInfoReturnable<Integer> cir) {
        int spoofed = PingSpoof.INSTANCE.getSpoofedPing();
        if (spoofed >= 0) {
            cir.setReturnValue(spoofed);
        }
    }
}
