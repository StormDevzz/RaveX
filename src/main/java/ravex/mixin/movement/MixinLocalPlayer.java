package ravex.mixin.movement;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.movement.NoSlowDown;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer {

    private float ravex$prevYaw;
    private float ravex$prevPitch;

    @Redirect(
        method = "modifyInput",
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

    /**
     * Silent rotations hook: Temporarily swaps client rotations with silent rotations right before sending the position package,
     * so that the server receives correct silent rotations while the client-side camera/render remains completely unaffected!
     */
    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void onSendPositionHead(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        ravex$prevYaw = player.getYRot();
        ravex$prevPitch = player.getXRot();

        if (ravex.manager.RotationManager.INSTANCE.isRotating()) {
            player.setYRot(ravex.manager.RotationManager.INSTANCE.getYaw());
            player.setXRot(ravex.manager.RotationManager.INSTANCE.getPitch());
        }
    }

    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void onSendPositionReturn(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        player.setYRot(ravex$prevYaw);
        player.setXRot(ravex$prevPitch);
    }

    @Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
    private void onAiStep(CallbackInfo ci) {
        if (ravex.modules.render.FreeCam.INSTANCE.getEnabled()) {
            LocalPlayer player = (LocalPlayer) (Object) this;
            player.setDeltaMovement(0, 0, 0);
            ci.cancel();
        }
    }
}
