package volthack.mixin.render;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import volthack.modules.player.FastBreak;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {
    @Shadow
    private int destroyDelay;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (FastBreak.INSTANCE.getEnabled()) {
            this.destroyDelay = 0;
        }
    }
}
