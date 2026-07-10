package ravex.mixin.movement;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.movement.NoSlow;

@Mixin(Player.class)
public abstract class MixinPlayerNoSlow {

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void onPlayerAiStep(CallbackInfo ci) {
        NoSlow ns = NoSlow.itz();
        if (!ns.isV3Active() || !ns.items.getValue()) return;

        Player player = (Player)(Object)this;
        float forward = ns.getV3Forward();
        float strafe = ns.getV3Strafe();

        if (ns.isInGrace()) {
            player.xxa *= (strafe / 0.2f);
            player.zza *= (forward / 0.2f);
        } else {
            player.xxa *= strafe;
            player.zza *= forward;
        }
    }
}
