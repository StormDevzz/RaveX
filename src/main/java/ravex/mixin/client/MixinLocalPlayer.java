package ravex.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.NoRender;
import ravex.modules.combat.AutoCrystal;
import ravex.modules.combat.Trap;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {
    @Shadow public float portalEffectIntensity;
    @Shadow public float oPortalEffectIntensity;

    private float ravexSavedYaw;
    private float ravexSavedPitch;

    @Inject(method = "handlePortalTransitionEffect", at = @At("RETURN"))
    private void onHandlePortalTransitionEffect(boolean inPortal, CallbackInfo ci) {
        if (NoRender.INSTANCE.getEnabled() && NoRender.INSTANCE.portal.getValue()) {
            this.portalEffectIntensity = 0.0f;
            this.oPortalEffectIntensity = 0.0f;
        }
    }

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void onSendPositionHead(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (AutoCrystal.INSTANCE.getEnabled() && AutoCrystal.INSTANCE.rotate.getValue().equals("Silent") && AutoCrystal.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(AutoCrystal.silentYaw);
            player.setXRot(AutoCrystal.silentPitch);
        } else if (Trap.INSTANCE.getEnabled() && Trap.INSTANCE.rotate.getValue().equals("Silent") && Trap.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(Trap.silentYaw);
            player.setXRot(Trap.silentPitch);
        }
    }

    @Inject(method = "sendPosition", at = @At("TAIL"))
    private void onSendPositionTail(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        boolean acActive = AutoCrystal.INSTANCE.getEnabled() && AutoCrystal.INSTANCE.rotate.getValue().equals("Silent") && AutoCrystal.hasSilentRotations();
        boolean trapActive = Trap.INSTANCE.getEnabled() && Trap.INSTANCE.rotate.getValue().equals("Silent") && Trap.hasSilentRotations();
        if (acActive || trapActive) {
            player.setYRot(ravexSavedYaw);
            player.setXRot(ravexSavedPitch);
        }
    }
}
