package ravex.mixin.movement;

import net.minecraft.client.Minecraft;
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
        if (!ns.items.getValue()) return;

        // GrimV3 input scaling
        if (ns.isV3Active()) {
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
            return;
        }

        // Matrix input scaling: boost input to compensate for item use slowdown
        if (ns.isMatrixActive() && Minecraft.getInstance().player != null) {
            Player player = (Player)(Object)this;
            float mul = ns.getMatrixInputScale();
            if (mul != 1.0f) {
                player.xxa *= mul;
                player.zza *= mul;
            }
        }
    }
}
