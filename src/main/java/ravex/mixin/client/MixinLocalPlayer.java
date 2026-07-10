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
<<<<<<< HEAD
import ravex.modules.combat.Breaker;
import ravex.modules.player.PacketMine;
import ravex.modules.player.AntiAim;
import ravex.modules.combat.BowAim;
import ravex.modules.combat.Quiver;
import ravex.modules.world.Scaffold;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

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
<<<<<<< HEAD
        if (Breaker.maybeEnabled() && Breaker.itz().rotate.getValue().equals("Silent") && Breaker.silentRotation.hasRotation) {
=======
        if (ravex.modules.combat.Breaker.INSTANCE.getEnabled() && ravex.modules.combat.Breaker.INSTANCE.rotate.getValue().equals("Silent") && ravex.modules.combat.Breaker.hasSilentRotations) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(ravex.modules.combat.Breaker.silentYaw);
            player.setXRot(ravex.modules.combat.Breaker.silentPitch);
        } else if (ravex.modules.player.PacketMine.INSTANCE.getEnabled() && ravex.modules.player.PacketMine.INSTANCE.rotate.getValue().equals("Silent") && ravex.modules.player.PacketMine.hasSilentRotations) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(ravex.modules.player.PacketMine.silentYaw);
            player.setXRot(ravex.modules.player.PacketMine.silentPitch);
        } else if (AutoCrystal.INSTANCE.getEnabled() && AutoCrystal.INSTANCE.rotate.getValue().equals("Silent") && AutoCrystal.hasSilentRotations()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
<<<<<<< HEAD
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
=======
            player.setYRot(ravex.modules.combat.Quiver.silentYaw);
            player.setXRot(ravex.modules.combat.Quiver.silentPitch);
        } else if (ravex.modules.world.Scaffold.INSTANCE.getEnabled() && ravex.modules.world.Scaffold.hasSilentRotation) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(ravex.modules.world.Scaffold.silentYaw);
            player.setXRot(ravex.modules.world.Scaffold.silentPitch);
        } else if (KillAura.INSTANCE.getEnabled() && KillAura.INSTANCE.rotate.getValue().equals("Silent") && KillAura.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(KillAura.silentYaw);
            player.setXRot(KillAura.silentPitch);
        } else if (ShieldFucker.INSTANCE.getEnabled() && ShieldFucker.INSTANCE.rotate.getValue().equals("Silent") && ShieldFucker.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(ShieldFucker.silentYaw);
            player.setXRot(ShieldFucker.silentPitch);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        }
    }

    @Inject(method = "sendPosition", at = @At("TAIL"))
    private void onSendPositionTail(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
<<<<<<< HEAD
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
=======
        boolean acActive = AutoCrystal.INSTANCE.getEnabled() && AutoCrystal.INSTANCE.rotate.getValue().equals("Silent") && AutoCrystal.hasSilentRotations();
        boolean trapActive = Trap.INSTANCE.getEnabled() && Trap.INSTANCE.rotate.getValue().equals("Silent") && Trap.hasSilentRotations();
        boolean selfTrapActive = SelfTrap.INSTANCE.getEnabled() && SelfTrap.INSTANCE.rotate.getValue().equals("Silent") && SelfTrap.hasSilentRotations();
        boolean basePlaceActive = BasePlace.INSTANCE.getEnabled() && BasePlace.INSTANCE.rotate.getValue().equals("Silent") && BasePlace.hasSilentRotations();
        boolean anchorAuraActive = AnchorAura.INSTANCE.getEnabled() && AnchorAura.INSTANCE.rotate.getValue().equals("Silent") && AnchorAura.hasSilentRotations();
        boolean antiAimActive = ravex.modules.player.AntiAim.INSTANCE.getEnabled() && ravex.modules.player.AntiAim.INSTANCE.silent.getValue();
        boolean bowAimActive = ravex.modules.combat.BowAim.INSTANCE.getEnabled() && ravex.modules.combat.BowAim.INSTANCE.rotate.getValue().equals("Silent") && ravex.modules.combat.BowAim.hasSilentRotations();
        boolean quiverActive = ravex.modules.combat.Quiver.INSTANCE.getEnabled() && ravex.modules.combat.Quiver.hasSilentRotations();
        boolean breakerActive = ravex.modules.combat.Breaker.INSTANCE.getEnabled() && ravex.modules.combat.Breaker.INSTANCE.rotate.getValue().equals("Silent") && ravex.modules.combat.Breaker.hasSilentRotations;
        boolean pmActive = ravex.modules.player.PacketMine.INSTANCE.getEnabled() && ravex.modules.player.PacketMine.INSTANCE.rotate.getValue().equals("Silent") && ravex.modules.player.PacketMine.hasSilentRotations;
        boolean kaActive = KillAura.INSTANCE.getEnabled() && KillAura.INSTANCE.rotate.getValue().equals("Silent") && KillAura.hasSilentRotations();
        boolean sfActive = ShieldFucker.INSTANCE.getEnabled() && ShieldFucker.INSTANCE.rotate.getValue().equals("Silent") && ShieldFucker.hasSilentRotations();
        boolean scaffoldSilent = ravex.modules.world.Scaffold.INSTANCE.getEnabled() && ravex.modules.world.Scaffold.hasSilentRotation;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (acActive || trapActive || selfTrapActive || basePlaceActive || anchorAuraActive || antiAimActive || bowAimActive || quiverActive || breakerActive || pmActive || kaActive || sfActive || scaffoldSilent) {
            player.setYRot(ravexSavedYaw);
            player.setXRot(ravexSavedPitch);
        }
    }
}
