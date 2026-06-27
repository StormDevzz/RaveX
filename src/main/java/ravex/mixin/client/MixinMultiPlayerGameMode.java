package ravex.mixin.client;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.exploit.GrimInstantMine;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {
    @Inject(method = "startPrediction", at = @At("HEAD"), cancellable = true)
    private void onStartDestroyBlock(BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> cir) {
        if (GrimInstantMine.INSTANCE.getEnabled()) {
            boolean handled = GrimInstantMine.INSTANCE.onBlockClick(pos, facing);
            if (handled) {
                cir.setReturnValue(true);
            }
        }
    }
}
