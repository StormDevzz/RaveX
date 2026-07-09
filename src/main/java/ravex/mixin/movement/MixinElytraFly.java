package ravex.mixin.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.movement.ElytraFly;

@Mixin(LivingEntity.class)
public abstract class MixinElytraFly {

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void onTravel(Vec3 travelVector, CallbackInfo ci) {
<<<<<<< HEAD
        if (!ElytraFly.maybeEnabled()) return;
=======
        if (!ElytraFly.INSTANCE.getEnabled()) return;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        LivingEntity entity = (LivingEntity)(Object)this;
        if (!(entity instanceof LocalPlayer)) return;
        LocalPlayer player = (LocalPlayer) entity;
        if (!player.isFallFlying()) return;
        ci.cancel();
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void onAiStep(CallbackInfo ci) {
<<<<<<< HEAD
        if (!ElytraFly.maybeEnabled()) return;
=======
        if (!ElytraFly.INSTANCE.getEnabled()) return;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

        LivingEntity entity = (LivingEntity)(Object)this;
        if (!(entity instanceof LocalPlayer)) return;
        LocalPlayer player = (LocalPlayer) entity;
        if (!player.isFallFlying()) return;

<<<<<<< HEAD
        if (ElytraFly.itz().speedControl.getValue()) return;

        Minecraft mc = Minecraft.getInstance();
        String mode = ElytraFly.itz().mode.getValue();
        double hSpeed = ElytraFly.itz().hSpeed.getValue();
        double vSpeed = ElytraFly.itz().vSpeed.getValue();
        double glide = ElytraFly.itz().glide.getValue();
=======
        if (ElytraFly.INSTANCE.speedControl.getValue()) return;

        Minecraft mc = Minecraft.getInstance();
        String mode = ElytraFly.INSTANCE.mode.getValue();
        double hSpeed = ElytraFly.INSTANCE.hSpeed.getValue();
        double vSpeed = ElytraFly.INSTANCE.vSpeed.getValue();
        double glide = ElytraFly.INSTANCE.glide.getValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

        boolean space = mc.options.keyJump.isDown();
        boolean shift = mc.options.keyShift.isDown();
        float forward = 0, strafe = 0;
        if (mc.options.keyUp.isDown()) forward++;
        if (mc.options.keyDown.isDown()) forward--;
        if (mc.options.keyLeft.isDown()) strafe++;
        if (mc.options.keyRight.isDown()) strafe--;

        Vec3 motion = player.getDeltaMovement();
        double yaw = player.getYRot();

        Vec3 vel;

        if (mode.equals("Control")) {
            double rad = Math.toRadians(yaw);
            double targetX = (-Math.sin(rad) * forward + Math.cos(rad) * strafe) * hSpeed;
            double targetZ = (Math.cos(rad) * forward + Math.sin(rad) * strafe) * hSpeed;
            double targetY = space ? vSpeed : (shift ? -vSpeed : -glide);
            if (forward == 0 && strafe == 0 && !space && !shift) {
                vel = new Vec3(motion.x * 0.2, -glide, motion.z * 0.2);
            } else {
                vel = new Vec3(targetX, targetY, targetZ);
            }
        } else {
            double[] v = ElytraFly.calculateVelocity(mode, hSpeed, vSpeed, glide,
                yaw, player.getXRot(), space, shift);
            vel = new Vec3(v[0], v[1], v[2]);
        }

<<<<<<< HEAD
        vel = ElytraFly.itz().applyTimerAndAccel(vel);
=======
        vel = ElytraFly.INSTANCE.applyTimerAndAccel(vel);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        player.setDeltaMovement(vel);
        player.move(MoverType.SELF, vel);
    }
}
