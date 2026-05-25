package volthack.mixin.render;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import volthack.modules.misc.TabUtils;

@Mixin(PlayerTabOverlay.class)
public class MixinPlayerTabOverlay {
    @Inject(method = "getNameForDisplay", at = @At("HEAD"), cancellable = true)
    private void onGetNameForDisplay(PlayerInfo playerInfo, CallbackInfoReturnable<Component> cir) {
        if (TabUtils.INSTANCE.getEnabled()) {
            cir.setReturnValue(TabUtils.INSTANCE.getTabName(playerInfo, playerInfo.getTabListDisplayName()));
        }
    }
}
