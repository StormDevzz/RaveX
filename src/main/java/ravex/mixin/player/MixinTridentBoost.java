package ravex.mixin.player;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import ravex.utility.player.InventoryUtility;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.movement.TridentBoost;
import ravex.modules.Modules;

@Mixin(Entity.class)
public class MixinTridentBoost {
    @Inject(method = "isInWaterOrRain", at = @At("HEAD"), cancellable = true)
    private void onIsInWaterOrRain(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;
        if (self instanceof Player player && Modules.enabled(TridentBoost.class)
                && Modules.get(TridentBoost.class).mode.equals("Always")) {
            if (InventoryUtility.isHolding(player, net.minecraft.world.item.Items.TRIDENT) || InventoryUtility.isOffhand(player, net.minecraft.world.item.Items.TRIDENT)) {
                cir.setReturnValue(true);
            }
        }
    }
}
