package volthack.mixin.render;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class MixinPlayer {
    @Inject(method = "getBlockInteractionRange", at = @At("HEAD"), cancellable = true)
    private void onGetBlockInteractionRange(CallbackInfoReturnable<Double> cir) {
        if ((Object)this instanceof net.minecraft.client.player.LocalPlayer) {
            if (volthack.modules.player.Reach.INSTANCE.getEnabled()) {
                cir.setReturnValue((double) volthack.modules.player.Reach.INSTANCE.getBlockReach());
            }
        }
    }

    @Inject(method = "getEntityInteractionRange", at = @At("HEAD"), cancellable = true)
    private void onGetEntityInteractionRange(CallbackInfoReturnable<Double> cir) {
        if ((Object)this instanceof net.minecraft.client.player.LocalPlayer) {
            if (volthack.modules.player.Reach.INSTANCE.getEnabled()) {
                cir.setReturnValue((double) volthack.modules.player.Reach.INSTANCE.getEntityReach());
            }
        }
    }
}
