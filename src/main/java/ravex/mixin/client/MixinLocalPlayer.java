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
import ravex.modules.combat.SelfTrap;
import ravex.modules.combat.BasePlace;
import ravex.modules.combat.AnchorAura;

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
        } else if (SelfTrap.INSTANCE.getEnabled() && SelfTrap.INSTANCE.rotate.getValue().equals("Silent") && SelfTrap.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(SelfTrap.getSilentYaw());
            player.setXRot(SelfTrap.getSilentPitch());
        } else if (BasePlace.INSTANCE.getEnabled() && BasePlace.INSTANCE.rotate.getValue().equals("Silent") && BasePlace.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(BasePlace.getSilentYaw());
            player.setXRot(BasePlace.getSilentPitch());
        } else if (AnchorAura.INSTANCE.getEnabled() && AnchorAura.INSTANCE.rotate.getValue().equals("Silent") && AnchorAura.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(AnchorAura.getSilentYaw());
            player.setXRot(AnchorAura.getSilentPitch());
        } else if (ravex.modules.player.AntiAim.INSTANCE.getEnabled() && ravex.modules.player.AntiAim.INSTANCE.silent.getValue()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(ravex.modules.player.AntiAim.getSilentYaw());
            player.setXRot(ravex.modules.player.AntiAim.getSilentPitch());
        } else if (ravex.modules.combat.BowAim.INSTANCE.getEnabled() && ravex.modules.combat.BowAim.INSTANCE.rotate.getValue().equals("Silent") && ravex.modules.combat.BowAim.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(ravex.modules.combat.BowAim.silentYaw);
            player.setXRot(ravex.modules.combat.BowAim.silentPitch);
        } else if (ravex.modules.combat.Quiver.INSTANCE.getEnabled() && ravex.modules.combat.Quiver.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(ravex.modules.combat.Quiver.silentYaw);
            player.setXRot(ravex.modules.combat.Quiver.silentPitch);
        } else if (ravex.modules.combat.Breaker.INSTANCE.getEnabled() && ravex.modules.combat.Breaker.INSTANCE.rotate.getValue().equals("Silent") && ravex.modules.combat.Breaker.hasSilentRotations) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(ravex.modules.combat.Breaker.silentYaw);
            player.setXRot(ravex.modules.combat.Breaker.silentPitch);
        }
    }

    @Inject(method = "sendPosition", at = @At("TAIL"))
    private void onSendPositionTail(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        boolean acActive = AutoCrystal.INSTANCE.getEnabled() && AutoCrystal.INSTANCE.rotate.getValue().equals("Silent") && AutoCrystal.hasSilentRotations();
        boolean trapActive = Trap.INSTANCE.getEnabled() && Trap.INSTANCE.rotate.getValue().equals("Silent") && Trap.hasSilentRotations();
        boolean selfTrapActive = SelfTrap.INSTANCE.getEnabled() && SelfTrap.INSTANCE.rotate.getValue().equals("Silent") && SelfTrap.hasSilentRotations();
        boolean basePlaceActive = BasePlace.INSTANCE.getEnabled() && BasePlace.INSTANCE.rotate.getValue().equals("Silent") && BasePlace.hasSilentRotations();
        boolean anchorAuraActive = AnchorAura.INSTANCE.getEnabled() && AnchorAura.INSTANCE.rotate.getValue().equals("Silent") && AnchorAura.hasSilentRotations();
        boolean antiAimActive = ravex.modules.player.AntiAim.INSTANCE.getEnabled() && ravex.modules.player.AntiAim.INSTANCE.silent.getValue();
        boolean bowAimActive = ravex.modules.combat.BowAim.INSTANCE.getEnabled() && ravex.modules.combat.BowAim.INSTANCE.rotate.getValue().equals("Silent") && ravex.modules.combat.BowAim.hasSilentRotations();
        boolean quiverActive = ravex.modules.combat.Quiver.INSTANCE.getEnabled() && ravex.modules.combat.Quiver.hasSilentRotations();
        boolean breakerActive = ravex.modules.combat.Breaker.INSTANCE.getEnabled() && ravex.modules.combat.Breaker.INSTANCE.rotate.getValue().equals("Silent") && ravex.modules.combat.Breaker.hasSilentRotations;
        if (acActive || trapActive || selfTrapActive || basePlaceActive || anchorAuraActive || antiAimActive || bowAimActive || quiverActive || breakerActive) {
            player.setYRot(ravexSavedYaw);
            player.setXRot(ravexSavedPitch);
        }
    }
}


