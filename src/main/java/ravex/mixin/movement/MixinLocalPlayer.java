package ravex.mixin.movement;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ravex.modules.movement.NoSlowDown;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer {

    @Redirect(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"
        )
    )
    private boolean onIsUsingItem(LocalPlayer player) {
        if (NoSlowDown.INSTANCE.getEnabled() && NoSlowDown.INSTANCE.items.getValue()) {
            return false;
        }
        return player.isUsingItem();
    }
}
