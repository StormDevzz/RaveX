package ravex.mixin.movement;

import net.minecraft.client.player.LocalPlayer;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.combat.KillAura;
import ravex.modules.Modules;

// sets the silent yaw before travel() computes the velocity
// grimac compares the sent yaw with the one used for the movement
// the camera yaw is restored at the tail
@Mixin(LivingEntity.class)
public class MixinLivingEntityTravel {

    private float ravexTravelSavedYaw;
    private float ravexTravelSavedPitch;
    private boolean ravexTravelModified;

    @Inject(method = "travel", at = @At("HEAD"))
    private void onTravelHead(Vec3 movementInput, CallbackInfo ci) {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null) return;

        if ((Object) this != mc.player) return;
        if (!Modules.enabled(KillAura.class) || !KillAura.hasSilentRotations()) return;
        KillAura ka = Modules.get(KillAura.class);
        if (ka == null || !ka.freeLook) return;

        LocalPlayer player = mc.player;
        ravexTravelSavedYaw   = player.getYRot();
        ravexTravelSavedPitch = player.getXRot();
        player.setYRot(KillAura.silentRotation.yaw);
        player.setXRot(KillAura.silentRotation.pitch);
        ravexTravelModified = true;

        if (Modules.enabled(ravex.modules.movement.Speed.class) && mc.options != null) {
            float forward = 0.0f;
            if (mc.options.keyUp.isDown()) forward++;
            if (mc.options.keyDown.isDown()) forward--;
            float strafe = 0.0f;
            if (mc.options.keyLeft.isDown()) strafe++;
            if (mc.options.keyRight.isDown()) strafe--;

            if (forward != 0.0f || strafe != 0.0f) {
                Vec3 motion = player.getDeltaMovement();
                double horiz = Math.hypot(motion.x, motion.z);
                if (horiz > 0.01) {
                    double yaw = KillAura.silentRotation.yaw;
                    double angle = 90.0 * Math.signum(strafe);
                    if (forward != 0.0f) angle *= Math.signum(forward) * 0.5;
                    yaw -= angle;
                    if (forward < 0.0f) yaw -= 180.0;
                    double rad = Math.toRadians(yaw);
                    player.setDeltaMovement(-Math.sin(rad) * horiz, motion.y, Math.cos(rad) * horiz);
                }
            }
        }
    }

    @Inject(method = "travel", at = @At("TAIL"))
    private void onTravelTail(Vec3 movementInput, CallbackInfo ci) {
        if (!ravexTravelModified) return;
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null) return;
        if ((Object) this != mc.player) return;

        mc.player.setYRot(ravexTravelSavedYaw);
        mc.player.setXRot(ravexTravelSavedPitch);
        ravexTravelModified = false;
    }
}
