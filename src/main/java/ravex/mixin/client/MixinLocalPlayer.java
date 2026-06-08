package ravex.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.NoRender;
import ravex.modules.combat.AutoCrystal;

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
        if (AutoCrystal.INSTANCE.getEnabled() && AutoCrystal.INSTANCE.rotate.getValue().equals("Silent") && AutoCrystal.hasSilentRotations()) {
            LocalPlayer player = (LocalPlayer) (Object) this;
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(AutoCrystal.silentYaw);
            player.setXRot(AutoCrystal.silentPitch);
        }
    }

    @Inject(method = "sendPosition", at = @At("TAIL"))
    private void onSendPositionTail(CallbackInfo ci) {
        if (AutoCrystal.INSTANCE.getEnabled() && AutoCrystal.INSTANCE.rotate.getValue().equals("Silent") && AutoCrystal.hasSilentRotations()) {
            LocalPlayer player = (LocalPlayer) (Object) this;
            player.setYRot(ravexSavedYaw);
            player.setXRot(ravexSavedPitch);
        }
    }
}
