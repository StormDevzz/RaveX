package ravex.mixin.entity;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.misc.RideExploit;
import ravex.modules.Modules;

@Mixin(Mob.class)
public class MixinMob {
    @Inject(method = "isSaddled()Z", at = @At("HEAD"), cancellable = true)
    private void onIsSaddled(CallbackInfoReturnable<Boolean> cir) {
        if (Modules.enabled(RideExploit.class)) {
            cir.setReturnValue(true);
        }
    }
}
