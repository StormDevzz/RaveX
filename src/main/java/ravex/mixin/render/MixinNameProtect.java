package ravex.mixin.render;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.misc.NameProtect;
import ravex.modules.Modules;

@Mixin(Player.class)
public class MixinNameProtect {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void onGetDisplayName(CallbackInfoReturnable<Component> cir) {
        if (!Modules.enabled(NameProtect.class)) return;
        cir.setReturnValue(Modules.get(NameProtect.class).protectComponent(cir.getReturnValue()));
    }
}
