package ravex.mixin.player;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.player.FastBreak;

@Mixin(MultiPlayerGameMode.class)
public class MixinFastBreak {
    @Shadow private int destroyDelay;

    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void onStartDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (FastBreak.maybeEnabled()) {
            int d = ((Double) FastBreak.itz().delay.getValue()).intValue();
            if (destroyDelay > d) {
                destroyDelay = d;
            }
        }
    }
}
