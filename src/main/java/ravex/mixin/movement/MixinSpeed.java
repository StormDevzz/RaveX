package ravex.mixin.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.combat.KillAura;
import ravex.modules.movement.Speed;

@Mixin(LocalPlayer.class)
public abstract class MixinSpeed {

    private static int ssStage = 1;
    private static double ssBaseSpeed = 0.2873;
    private static int ssTicks = 0;
    private static boolean ssWasMoving = false;

    private static float getForward() {
        Minecraft mc = Minecraft.getInstance();
        float f = 0;
        if (mc.options.keyUp.isDown()) f++;
        if (mc.options.keyDown.isDown()) f--;
        return f;
    }

    private static float getStrafe() {
        Minecraft mc = Minecraft.getInstance();
        float s = 0;
        if (mc.options.keyLeft.isDown()) s++;
        if (mc.options.keyRight.isDown()) s--;
        return s;
    }

    private static boolean isJumping() {
        return Minecraft.getInstance().options.keyJump.isDown();
    }

    private static double getMoveYaw(LocalPlayer player) {
        if (KillAura.hasSilentRotations()) {
            return Math.toRadians(KillAura.silentRotation.yaw);
        }
        return Math.toRadians(player.getYRot());
    }

    private static void applySpeedLimit(LocalPlayer player, double limit) {
        Vec3 m = player.getDeltaMovement();
        double horiz = Math.sqrt(m.x * m.x + m.z * m.z);
        if (horiz > limit) {
            double scale = limit / horiz;
            player.setDeltaMovement(m.x * scale, m.y, m.z * scale);
        }
    }

    @Inject(method = "applyInput", at = @At("TAIL"))
    private void onApplyInput(CallbackInfo ci) {
        if (!Speed.maybeEnabled()) return;
        if (!"Matrix".equals(Speed.itz().mode.getValue())) return;

        LocalPlayer player = (LocalPlayer)(Object)this;
        float mul = (float)(double)Speed.itz().matrixInputMul.getValue();
        player.zza *= mul;
        player.xxa *= mul;
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void onAiStep(CallbackInfo ci) {
        if (!Speed.maybeEnabled()) return;

        LocalPlayer player = (LocalPlayer)(Object)this;
        if (player.horizontalCollision || player.isFallFlying()) return;

        String mode = Speed.itz().mode.getValue();
        double baseSpeed = Speed.itz().speed.getValue();
        double globalLimit = Speed.itz().speedLimit.getValue();

        Vec3 motion = player.getDeltaMovement();

        switch (mode) {
            case "Vanilla" -> {
                float forward = getForward();
                float strafe = getStrafe();
                if (forward == 0 && strafe == 0) return;

                double speedVal = baseSpeed * 0.15;
                double yaw = getMoveYaw(player);
                double velX = (-Math.sin(yaw) * forward + Math.cos(yaw) * strafe) * speedVal;
                double velZ = (Math.cos(yaw) * forward + Math.sin(yaw) * strafe) * speedVal;

                if (player.onGround()) {
                    player.setDeltaMovement(velX, motion.y, velZ);
                } else {
                    double factor = 0.8;
                    player.setDeltaMovement(
                        motion.x * factor + velX * (1 - factor),
                        motion.y,
                        motion.z * factor + velZ * (1 - factor)
                    );
                }
                applySpeedLimit(player, globalLimit);
            }
            case "Strafe" -> {
                if (!Speed.itz().strafeJump.getValue()) return;

                float forward = getForward();
                float strafe = getStrafe();
                if (forward == 0 && strafe == 0) return;

                if (player.onGround()) {
                    double speedVal = baseSpeed * 0.12;
                    double yaw = getMoveYaw(player);
                    double velX = (-Math.sin(yaw) * forward + Math.cos(yaw) * strafe) * speedVal;
                    double velZ = (Math.cos(yaw) * forward + Math.sin(yaw) * strafe) * speedVal;

                    double currentHorizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
                    double newHorizontal = Math.sqrt(velX * velX + velZ * velZ);

                    if (newHorizontal > currentHorizontal) {
                        player.setDeltaMovement(velX, motion.y, velZ);
                    }
                } else {
                    double speedVal = baseSpeed * 0.08;
                    double yaw = getMoveYaw(player);
                    double velX = (-Math.sin(yaw) * forward + Math.cos(yaw) * strafe) * speedVal;
                    double velZ = (Math.cos(yaw) * forward + Math.sin(yaw) * strafe) * speedVal;

                    double currentHorizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
                    double newHorizontal = Math.sqrt(velX * velX + velZ * velZ);

                    if (newHorizontal > currentHorizontal) {
                        double ratio = Math.min(1.0, currentHorizontal / Math.max(0.01, newHorizontal));
                        player.setDeltaMovement(
                            motion.x + velX * (1 - ratio) * 0.35,
                            motion.y,
                            motion.z + velZ * (1 - ratio) * 0.35
                        );
                    }
                }
                applySpeedLimit(player, globalLimit);
            }
            case "NCP" -> {
                if (player.onGround()) {
                    float forward = getForward();
                    float strafe = getStrafe();
                    if (forward == 0 && strafe == 0) return;

                    double speedVal = baseSpeed * 0.10;
                    double yaw = getMoveYaw(player);
                    double velX = (-Math.sin(yaw) * forward + Math.cos(yaw) * strafe) * speedVal;
                    double velZ = (Math.cos(yaw) * forward + Math.sin(yaw) * strafe) * speedVal;

                    player.setDeltaMovement(velX, motion.y, velZ);
                    if (isJumping()) {
                        player.setDeltaMovement(player.getDeltaMovement().x, 0.42, player.getDeltaMovement().z);
                    }
                } else if (player.getDeltaMovement().y < 0) {
                    double speedVal = baseSpeed * 0.06;
                    float forward = getForward();
                    float strafe = getStrafe();
                    double yaw = getMoveYaw(player);
                    double velX = (-Math.sin(yaw) * forward + Math.cos(yaw) * strafe) * speedVal;
                    double velZ = (Math.cos(yaw) * forward + Math.sin(yaw) * strafe) * speedVal;

                    player.setDeltaMovement(
                        motion.x * 0.91 + velX * 0.5,
                        motion.y,
                        motion.z * 0.91 + velZ * 0.5
                    );
                }
                applySpeedLimit(player, globalLimit);
            }
            case "NCPStrict" -> {
                if (player.onGround()) {
                    float forward = getForward();
                    float strafe = getStrafe();
                    if (forward == 0 && strafe == 0) return;

                    double speedVal = baseSpeed * 0.08;
                    double yaw = getMoveYaw(player);
                    double velX = (-Math.sin(yaw) * forward + Math.cos(yaw) * strafe) * speedVal;
                    double velZ = (Math.cos(yaw) * forward + Math.sin(yaw) * strafe) * speedVal;

                    player.setDeltaMovement(velX, motion.y, velZ);
                    if (isJumping()) {
                        player.setDeltaMovement(player.getDeltaMovement().x, 0.40, player.getDeltaMovement().z);
                    }
                } else if (player.getDeltaMovement().y < 0) {
                    double speedVal = baseSpeed * 0.04;
                    float forward = getForward();
                    float strafe = getStrafe();
                    double yaw = getMoveYaw(player);
                    double velX = (-Math.sin(yaw) * forward + Math.cos(yaw) * strafe) * speedVal;
                    double velZ = (Math.cos(yaw) * forward + Math.sin(yaw) * strafe) * speedVal;

                    player.setDeltaMovement(
                        motion.x * 0.91 + velX * 0.3,
                        motion.y,
                        motion.z * 0.91 + velZ * 0.3
                    );
                }
                applySpeedLimit(player, Math.min(globalLimit, 0.22));
            }
            case "StrafeStrict" -> {
                float forward = getForward();
                float strafe = getStrafe();
                if (forward == 0 && strafe == 0) {
                    if (ssWasMoving) {
                        ssStage = 1;
                        ssBaseSpeed = 0.2873;
                        ssWasMoving = false;
                    }
                    Speed.matrixTimer = 1.0f;
                    return;
                }
                ssWasMoving = true;

                if (Speed.itz().strafeStrictTimer.getValue()) {
                    Speed.matrixTimer = 1.088f;
                } else {
                    Speed.matrixTimer = 1.0f;
                }

                double baseMoveSpeed = 0.2873;
                if (player.hasEffect(MobEffects.SPEED)) {
                    int amp = player.getEffect(MobEffects.SPEED).getAmplifier();
                    baseMoveSpeed *= 1.0 + 0.2 * (amp + 1);
                }
                if (player.hasEffect(MobEffects.SLOWNESS)) {
                    int amp = player.getEffect(MobEffects.SLOWNESS).getAmplifier();
                    baseMoveSpeed /= 1.0 + 0.2 * (amp + 1);
                }

                if (ssStage == 1 && player.onGround() && !player.horizontalCollision) {
                    player.setDeltaMovement(player.getDeltaMovement().x, 0.42, player.getDeltaMovement().z);
                    ssBaseSpeed *= 2.149;
                    ssStage = 2;
                } else if (ssStage == 2) {
                    double currentSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
                    ssBaseSpeed = currentSpeed - (0.66 * (currentSpeed - baseMoveSpeed));
                    ssStage = 3;
                } else {
                    if (player.verticalCollision) {
                        ssStage = 1;
                    }
                    ssBaseSpeed = ssBaseSpeed - ssBaseSpeed / 159.0;
                }

                ssBaseSpeed = Math.max(ssBaseSpeed, baseMoveSpeed);

                double cap = Speed.itz().strafeStrictCap.getValue();
                ssBaseSpeed = Math.min(ssBaseSpeed, cap);

                float f = forward;
                float s = strafe;
                if (f != 0 && s != 0) {
                    f *= (float)Math.sin(Math.PI / 4);
                    s *= (float)Math.cos(Math.PI / 4);
                }
                double yawRad = getMoveYaw(player);
                double velX = f * ssBaseSpeed * -Math.sin(yawRad) + s * ssBaseSpeed * Math.cos(yawRad);
                double velZ = f * ssBaseSpeed * Math.cos(yawRad) - s * ssBaseSpeed * -Math.sin(yawRad);

                player.setDeltaMovement(velX, player.getDeltaMovement().y, velZ);
            }
            case "Matrix" -> {
            }
            case "Grim" -> {
                if (!player.onGround()) return;
                float forward = getForward();
                float strafe = getStrafe();
                if (forward == 0 && strafe == 0) return;

                double boost = Speed.itz().grimBoost.getValue();
                double currentHoriz = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
                double speedVal = baseSpeed * 0.04 * boost;
                double yaw = getMoveYaw(player);
                double velX = (-Math.sin(yaw) * forward + Math.cos(yaw) * strafe) * speedVal;
                double velZ = (Math.cos(yaw) * forward + Math.sin(yaw) * strafe) * speedVal;
                double newHoriz = Math.sqrt(velX * velX + velZ * velZ);

                if (newHoriz > currentHoriz) {
                    double addX = velX * 0.4;
                    double addZ = velZ * 0.4;
                    player.setDeltaMovement(motion.x + addX, motion.y, motion.z + addZ);
                }

                applySpeedLimit(player, Math.min(globalLimit, 0.22));
            }
            case "GrimStrict" -> {
                double grimB = Speed.itz().grimBoost.getValue();
                float currentSpeed = player.getAbilities().getWalkingSpeed();
                float targetSpeed = (float)(0.1 * (0.7 + grimB * 0.3));
                if (currentSpeed != targetSpeed) {
                    player.getAbilities().setWalkingSpeed(targetSpeed);
                }
            }
        }
    }
}
