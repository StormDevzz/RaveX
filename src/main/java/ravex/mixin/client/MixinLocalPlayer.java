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
import ravex.modules.combat.AutoCart;
import ravex.modules.combat.Breaker;
import ravex.modules.player.PacketMine;
import ravex.modules.player.AntiAim;
import ravex.modules.combat.BowAim;
import ravex.modules.combat.Quiver;
import ravex.modules.world.Scaffold;
import ravex.modules.Modules;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {
    @Shadow public float portalEffectIntensity;
    @Shadow public float oPortalEffectIntensity;

    private float ravexSavedYaw;
    private float ravexSavedPitch;

    @Inject(method = "handlePortalTransitionEffect", at = @At("RETURN"))
    private void onHandlePortalTransitionEffect(boolean inPortal, CallbackInfo ci) {
        if (        Modules.enabled(NoRender.class) && Modules.get(NoRender.class).portal) {
            this.portalEffectIntensity = 0.0f;
            this.oPortalEffectIntensity = 0.0f;
        }
    }

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void onSendPositionHead(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (Modules.enabled(Breaker.class) && Modules.get(Breaker.class).rotate.equals("Silent") && Breaker.silentRotation.hasRotation) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(Breaker.silentRotation.yaw);
            player.setXRot(Breaker.silentRotation.pitch);
        } else if (Modules.enabled(PacketMine.class) && Modules.get(PacketMine.class).rotate.equals("Silent") && PacketMine.silentRotation.hasRotation) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(PacketMine.silentRotation.yaw);
            player.setXRot(PacketMine.silentRotation.pitch);
        } else if (Modules.enabled(AutoCrystal.class) && Modules.get(AutoCrystal.class).rotate.equals("Silent") && AutoCrystal.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(AutoCrystal.silentRotation.yaw);
            player.setXRot(AutoCrystal.silentRotation.pitch);
        } else if (Modules.enabled(Trap.class) && Modules.get(Trap.class).rotate.equals("Silent") && Trap.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(Trap.silentRotation.yaw);
            player.setXRot(Trap.silentRotation.pitch);
        } else if (Modules.enabled(SelfTrap.class) && Modules.get(SelfTrap.class).rotate.equals("Silent") && SelfTrap.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(SelfTrap.getSilentYaw());
            player.setXRot(SelfTrap.getSilentPitch());
        } else if (Modules.enabled(BasePlace.class) && Modules.get(BasePlace.class).rotate.equals("Silent") && BasePlace.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(BasePlace.getSilentYaw());
            player.setXRot(BasePlace.getSilentPitch());
        } else if (Modules.enabled(AnchorAura.class) && Modules.get(AnchorAura.class).rotate.equals("Silent") && AnchorAura.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(AnchorAura.getSilentYaw());
            player.setXRot(AnchorAura.getSilentPitch());
        } else if (Modules.enabled(AntiAim.class) && Modules.get(AntiAim.class).silent) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(AntiAim.getSilentYaw());
            player.setXRot(AntiAim.getSilentPitch());
        } else if (Modules.enabled(BowAim.class) && Modules.get(BowAim.class).rotate.equals("Silent") && BowAim.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(BowAim.silentRotation.yaw);
            player.setXRot(BowAim.silentRotation.pitch);
        } else if (Modules.enabled(Quiver.class) && Quiver.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(Quiver.silentRotation.yaw);
            player.setXRot(Quiver.silentRotation.pitch);
        } else if (Modules.enabled(AutoCart.class) && Modules.get(AutoCart.class).bypass.equals("NCP") && AutoCart.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(AutoCart.silentRotation.yaw);
            player.setXRot(AutoCart.silentRotation.pitch);
        } else if (Modules.enabled(Scaffold.class) && Scaffold.silentRotation.hasRotation) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(KillAura.silentRotation.yaw);
            player.setXRot(KillAura.silentRotation.pitch);
        } else if (Modules.enabled(ShieldFucker.class) && Modules.get(ShieldFucker.class).rotate.equals("Silent") && ShieldFucker.hasSilentRotations()) {
            ravexSavedYaw = player.getYRot();
            ravexSavedPitch = player.getXRot();
            player.setYRot(ShieldFucker.silentRotation.yaw);
            player.setXRot(ShieldFucker.silentRotation.pitch);
        }
    }

    @Inject(method = "sendPosition", at = @At("TAIL"))
    private void onSendPositionTail(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        boolean acActive = Modules.enabled(AutoCrystal.class) && Modules.get(AutoCrystal.class).rotate.equals("Silent") && AutoCrystal.hasSilentRotations();
        boolean trapActive = Modules.enabled(Trap.class) && Modules.get(Trap.class).rotate.equals("Silent") && Trap.hasSilentRotations();
        boolean selfTrapActive = Modules.enabled(SelfTrap.class) && Modules.get(SelfTrap.class).rotate.equals("Silent") && SelfTrap.hasSilentRotations();
        boolean basePlaceActive = Modules.enabled(BasePlace.class) && Modules.get(BasePlace.class).rotate.equals("Silent") && BasePlace.hasSilentRotations();
        boolean anchorAuraActive = Modules.enabled(AnchorAura.class) && Modules.get(AnchorAura.class).rotate.equals("Silent") && AnchorAura.hasSilentRotations();
        boolean antiAimActive = Modules.enabled(AntiAim.class) && Modules.get(AntiAim.class).silent;
        boolean bowAimActive = Modules.enabled(BowAim.class) && Modules.get(BowAim.class).rotate.equals("Silent") && BowAim.hasSilentRotations();
        boolean quiverActive = Modules.enabled(Quiver.class) && Quiver.hasSilentRotations();
        boolean breakerActive = Modules.enabled(Breaker.class) && Modules.get(Breaker.class).rotate.equals("Silent") && Breaker.silentRotation.hasRotation;
        boolean pmActive = Modules.enabled(PacketMine.class) && Modules.get(PacketMine.class).rotate.equals("Silent") && PacketMine.silentRotation.hasRotation;
        boolean kaActive = Modules.enabled(KillAura.class) && KillAura.hasSilentRotations();
        boolean sfActive = Modules.enabled(ShieldFucker.class) && Modules.get(ShieldFucker.class).rotate.equals("Silent") && ShieldFucker.hasSilentRotations();
        boolean autoCartActive = Modules.enabled(AutoCart.class) && Modules.get(AutoCart.class).bypass.equals("NCP") && AutoCart.hasSilentRotations();
        boolean scaffoldSilent = Modules.enabled(Scaffold.class) && Scaffold.silentRotation.hasRotation;
        if (acActive || trapActive || selfTrapActive || basePlaceActive || anchorAuraActive || antiAimActive || bowAimActive || quiverActive || breakerActive || pmActive || kaActive || sfActive || autoCartActive || scaffoldSilent) {
            player.setYRot(ravexSavedYaw);
            player.setXRot(ravexSavedPitch);
        }
    }
}
