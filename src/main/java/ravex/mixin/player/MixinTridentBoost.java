package ravex.mixin.player;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.movement.TridentBoost;

@Mixin(Entity.class)
public class MixinTridentBoost {
    @Inject(method = "isInWaterOrRain", at = @At("HEAD"), cancellable = true)
    private void onIsInWaterOrRain(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;
        if (self instanceof Player player && TridentBoost.maybeEnabled()
                && TridentBoost.itz().mode.getValue().equals("Always")) {
            if (player.getMainHandItem().is(Items.TRIDENT) || player.getOffhandItem().is(Items.TRIDENT)) {
                cir.setReturnValue(true);
            }
        }
    }
}
