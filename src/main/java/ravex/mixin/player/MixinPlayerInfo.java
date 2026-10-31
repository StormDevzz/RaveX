package ravex.mixin.player;

import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.client.FastLatency;

@Mixin(PlayerInfo.class)
public class MixinPlayerInfo {

    @Inject(method = "getLatency", at = @At("RETURN"), cancellable = true)
    private void onGetLatency(CallbackInfoReturnable<Integer> cir) {
        int measured = FastLatency.overrideLatency((PlayerInfo) (Object) this);
        if (measured >= 0) {
            cir.setReturnValue(measured);
        }
    }
}
