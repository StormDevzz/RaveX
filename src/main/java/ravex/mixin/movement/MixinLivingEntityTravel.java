package ravex.mixin.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.combat.KillAura;

/**
 * Hooks into LivingEntity.travel() to apply silent rotation for KillAura.
 *
 * Why here and not in sendPosition?
 * sendPosition fires AFTER travel() has already computed velocity using the player's
 * current yaw. GrimAC uses the reported yaw to simulate the expected next position.
 * If the yaw sent in the packet differs from the yaw used for movement, GrimAC's
 * simulation diverges → Simulation flags.
 *
 * By setting the silent yaw at HEAD of travel() and restoring at TAIL, we ensure:
 *   1. Movement is computed with the silent (target-facing) yaw  → GrimAC simulation matches
 *   2. sendPosition packet is sent during travel() with the silent yaw → consistent
 *   3. After travel() returns the real camera yaw is restored → player sees no rotation
 */
@Mixin(LivingEntity.class)
public class MixinLivingEntityTravel {

    private float ravexTravelSavedYaw;
    private float ravexTravelSavedPitch;
    private boolean ravexTravelModified;

    @Inject(method = "travel", at = @At("HEAD"))
    private void onTravelHead(Vec3 movementInput, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        // Only apply to the local player entity
        if ((Object) this != mc.player) return;
        if (!KillAura.maybeEnabled() || !KillAura.hasSilentRotations()) return;

        LocalPlayer player = mc.player;
        ravexTravelSavedYaw   = player.getYRot();
        ravexTravelSavedPitch = player.getXRot();
        player.setYRot(KillAura.silentRotation.yaw);
        player.setXRot(KillAura.silentRotation.pitch);
        ravexTravelModified = true;
    }

    @Inject(method = "travel", at = @At("TAIL"))
    private void onTravelTail(Vec3 movementInput, CallbackInfo ci) {
        if (!ravexTravelModified) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if ((Object) this != mc.player) return;

        mc.player.setYRot(ravexTravelSavedYaw);
        mc.player.setXRot(ravexTravelSavedPitch);
        ravexTravelModified = false;
    }
}
