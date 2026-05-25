package volthack.mixin.render;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import volthack.modules.player.MultiTask;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {
    @Inject(method = "isUsingItem", at = @At("HEAD"), cancellable = true)
    private void onIsUsingItem(CallbackInfoReturnable<Boolean> cir) {
        if (MultiTask.INSTANCE.getEnabled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getBlockInteractionRange", at = @At("HEAD"), cancellable = true)
    private void onGetBlockInteractionRange(CallbackInfoReturnable<Double> cir) {
        if (volthack.modules.player.Reach.INSTANCE.getEnabled()) {
            cir.setReturnValue((double) volthack.modules.player.Reach.INSTANCE.getBlockReach());
        }
    }

    @Inject(method = "getEntityInteractionRange", at = @At("HEAD"), cancellable = true)
    private void onGetEntityInteractionRange(CallbackInfoReturnable<Double> cir) {
        if (volthack.modules.player.Reach.INSTANCE.getEnabled()) {
            cir.setReturnValue((double) volthack.modules.player.Reach.INSTANCE.getEntityReach());
        }
    }
}
