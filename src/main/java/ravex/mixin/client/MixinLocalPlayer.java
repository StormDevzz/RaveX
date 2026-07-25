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
import ravex.modules.combat.KillAura;
import ravex.modules.combat.ShieldFucker;
import ravex.modules.combat.Breaker;
import ravex.modules.player.PacketMine;
import ravex.modules.player.AntiAim;
import ravex.modules.combat.BowAim;
import ravex.modules.combat.Quiver;
import ravex.modules.world.Scaffold;
import ravex.modules.world.SafeAnchor;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {
    @Shadow public float portalEffectIntensity;
    @Shadow public float oPortalEffectIntensity;

    private float ravexSavedYaw;
    private float ravexSavedPitch;

    @Inject(method = "handlePortalTransitionEffect", at = @At("RETURN"))
    private void onHandlePortalTransitionEffect(boolean inPortal, CallbackInfo ci) {
        if (        NoRender.maybeEnabled() && NoRender.itz().portal.getValue()) {
            this.portalEffectIntensity = 0.0f;
            this.oPortalEffectIntensity = 0.0f;
        }
    }

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void onSendPositionHead(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (Breaker.maybeEnabled() && Breaker.itz().rotate.getValue().equals("Silent") && Breaker.silentRotation.hasRotation) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(Breaker.silentRotation.yaw);
            player.setXRot(Breaker.silentRotation.pitch);
        } else if (PacketMine.maybeEnabled() && PacketMine.itz().rotate.getValue().equals("Silent") && PacketMine.silentRotation.hasRotation) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(PacketMine.silentRotation.yaw);
            player.setXRot(PacketMine.silentRotation.pitch);
        } else if (AutoCrystal.maybeEnabled() && AutoCrystal.itz().rotate.getValue().equals("Silent") && AutoCrystal.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(AutoCrystal.silentRotation.yaw);
            player.setXRot(AutoCrystal.silentRotation.pitch);
        } else if (Trap.maybeEnabled() && Trap.itz().rotate.getValue().equals("Silent") && Trap.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(Trap.silentRotation.yaw);
            player.setXRot(Trap.silentRotation.pitch);
        } else if (SelfTrap.maybeEnabled() && SelfTrap.itz().rotate.getValue().equals("Silent") && SelfTrap.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(SelfTrap.getSilentYaw());
            player.setXRot(SelfTrap.getSilentPitch());
        } else if (BasePlace.maybeEnabled() && BasePlace.itz().rotate.getValue().equals("Silent") && BasePlace.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(BasePlace.getSilentYaw());
            player.setXRot(BasePlace.getSilentPitch());
        } else if (AnchorAura.maybeEnabled() && AnchorAura.itz().rotate.getValue().equals("Silent") && AnchorAura.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(AnchorAura.getSilentYaw());
            player.setXRot(AnchorAura.getSilentPitch());
        } else if (AntiAim.maybeEnabled() && AntiAim.itz().silent.getValue()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(AntiAim.getSilentYaw());
            player.setXRot(AntiAim.getSilentPitch());
        } else if (BowAim.maybeEnabled() && BowAim.itz().rotate.getValue().equals("Silent") && BowAim.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(BowAim.silentRotation.yaw);
            player.setXRot(BowAim.silentRotation.pitch);
        } else if (Quiver.maybeEnabled() && Quiver.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(Quiver.silentRotation.yaw);
            player.setXRot(Quiver.silentRotation.pitch);
        } else if (Scaffold.maybeEnabled() && Scaffold.silentRotation.hasRotation) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(Scaffold.silentRotation.yaw);
            player.setXRot(Scaffold.silentRotation.pitch);
        } else if (KillAura.maybeEnabled() && KillAura.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(KillAura.silentRotation.yaw);
            player.setXRot(KillAura.silentRotation.pitch);
        } else if (ShieldFucker.maybeEnabled() && ShieldFucker.itz().rotate.getValue().equals("Silent") && ShieldFucker.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(ShieldFucker.silentRotation.yaw);
            player.setXRot(ShieldFucker.silentRotation.pitch);
        } else if (SafeAnchor.maybeEnabled() && SafeAnchor.itz().rotate.getValue().equals("Silent") && SafeAnchor.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(SafeAnchor.silentRotation.yaw);
            player.setXRot(SafeAnchor.silentRotation.pitch);
        }
    }

    @Inject(method = "sendPosition", at = @At("TAIL"))
    private void onSendPositionTail(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        boolean acActive = AutoCrystal.maybeEnabled() && AutoCrystal.itz().rotate.getValue().equals("Silent") && AutoCrystal.hasSilentRotations();
        boolean trapActive = Trap.maybeEnabled() && Trap.itz().rotate.getValue().equals("Silent") && Trap.hasSilentRotations();
        boolean selfTrapActive = SelfTrap.maybeEnabled() && SelfTrap.itz().rotate.getValue().equals("Silent") && SelfTrap.hasSilentRotations();
        boolean basePlaceActive = BasePlace.maybeEnabled() && BasePlace.itz().rotate.getValue().equals("Silent") && BasePlace.hasSilentRotations();
        boolean anchorAuraActive = AnchorAura.maybeEnabled() && AnchorAura.itz().rotate.getValue().equals("Silent") && AnchorAura.hasSilentRotations();
        boolean antiAimActive = AntiAim.maybeEnabled() && AntiAim.itz().silent.getValue();
        boolean bowAimActive = BowAim.maybeEnabled() && BowAim.itz().rotate.getValue().equals("Silent") && BowAim.hasSilentRotations();
        boolean quiverActive = Quiver.maybeEnabled() && Quiver.hasSilentRotations();
        boolean breakerActive = Breaker.maybeEnabled() && Breaker.itz().rotate.getValue().equals("Silent") && Breaker.silentRotation.hasRotation;
        boolean pmActive = PacketMine.maybeEnabled() && PacketMine.itz().rotate.getValue().equals("Silent") && PacketMine.silentRotation.hasRotation;
        boolean kaActive = KillAura.maybeEnabled() && KillAura.hasSilentRotations();
        boolean sfActive = ShieldFucker.maybeEnabled() && ShieldFucker.itz().rotate.getValue().equals("Silent") && ShieldFucker.hasSilentRotations();
        boolean scaffoldSilent = Scaffold.maybeEnabled() && Scaffold.silentRotation.hasRotation;
        boolean safeAnchorSilent = SafeAnchor.maybeEnabled() && SafeAnchor.itz().rotate.getValue().equals("Silent") && SafeAnchor.hasSilentRotations();
        if (acActive || trapActive || selfTrapActive || basePlaceActive || anchorAuraActive || antiAimActive || bowAimActive || quiverActive || breakerActive || pmActive || kaActive || sfActive || scaffoldSilent || safeAnchorSilent) {
            player.setYRot(ravexSavedYaw);
            player.setXRot(ravexSavedPitch);
        }
    }
}
