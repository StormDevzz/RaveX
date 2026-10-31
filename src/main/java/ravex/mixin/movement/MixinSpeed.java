package ravex.mixin.movement;

import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import ravex.utility.misc.PotionUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.combat.KillAura;
import ravex.modules.movement.Speed;
import ravex.modules.Modules;

@Mixin(LocalPlayer.class)
public abstract class MixinSpeed {

    private static int ssStage = 1;
    private static double ssBaseSpeed = 0.2873;
    private static int ssTicks = 0;
    private static boolean ssWasMoving = false;

    // own state, both strafe modes stay independent
    private static int stStage = 0;
    private static double stSpeed = 0.2873;
    private static long stLimitTimer = 0L;

    private static float getForward() {
        var mc = MinecraftWrapper.getInstance();
        float f = 0;
        if (mc.options.keyUp.isDown()) f++;
        if (mc.options.keyDown.isDown()) f--;
        return f;
    }

    private static float getStrafe() {
        var mc = MinecraftWrapper.getInstance();
        float s = 0;
        if (mc.options.keyLeft.isDown()) s++;
        if (mc.options.keyRight.isDown()) s--;
        return s;
    }

    private static boolean isJumping() {
        return MinecraftWrapper.getInstance().options.keyJump.isDown();
    }

    private static double getMoveYaw(LocalPlayer player) {
        if (Modules.enabled(KillAura.class) && KillAura.hasSilentRotations()) {
            return Math.toRadians(KillAura.silentRotation.yaw);
        }
        return Math.toRadians(player.getYRot());
    }

    private static double getHop(LocalPlayer player, double height) {
        var jumpBoost = player.getEffect(net.minecraft.world.effect.MobEffects.JUMP_BOOST);
        if (jumpBoost != null) {
            height += (jumpBoost.getAmplifier() + 1) * 0.1;
        }
        return height;
    }

    // velocity points along the input direction
    private static double[] transformStrafe(LocalPlayer player, double speed, float forward, float strafe) {
        float f = Math.signum(forward);
        float s = Math.signum(strafe);
        if (f == 0.0f && s == 0.0f) return new double[]{0.0, 0.0};
        double yaw = Math.toDegrees(getMoveYaw(player));
        double angle = 90.0 * s;
        if (f != 0.0f) angle *= f * 0.5;
        yaw -= angle;
        if (f < 0.0f) yaw -= 180.0;
        double rad = Math.toRadians(yaw);
        return new double[]{-Math.sin(rad) * speed, Math.cos(rad) * speed};
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
        if (!Modules.enabled(Speed.class)) return;
        if (!"Matrix".equals(Modules.get(Speed.class).mode)) return;

        LocalPlayer player = (LocalPlayer)(Object)this;
        float mul = (float)(double)Modules.get(Speed.class).matrixInputMul;
        player.zza *= mul;
        player.xxa *= mul;
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void onAiStep(CallbackInfo ci) {
        if (!Modules.enabled(Speed.class)) return;

        LocalPlayer player = (LocalPlayer)(Object)this;
        if (player.horizontalCollision || player.isFallFlying()) return;

        String mode = Modules.get(Speed.class).mode;
        double baseSpeed = Modules.get(Speed.class).speed;
        double globalLimit = Modules.get(Speed.class).speedLimit;

        Vec3 motion = player.getDeltaMovement();

        switch (mode) {
            case "Vanilla" -> {
                float forward = getForward();
                float strafe = getStrafe();
                if (forward == 0 && strafe == 0) return;

                double speedVal = baseSpeed * 0.15;
                double[] dir = transformStrafe(player, speedVal, forward, strafe);
                double velX = dir[0];
                double velZ = dir[1];

                if (player.onGround()) {
                    player.setDeltaMovement(velX, motion.y, velZ);
                } else {
                    double factor = (Modules.enabled(KillAura.class) && KillAura.hasSilentRotations()) ? 0.35 : 0.8;
                    player.setDeltaMovement(
                        motion.x * factor + velX * (1 - factor),
                        motion.y,
                        motion.z * factor + velZ * (1 - factor)
                    );
                }
                applySpeedLimit(player, globalLimit);
            }
            case "Strafe" -> {
                // speed rebuilt from the distance walked
                float forward = getForward();
                float strafe = getStrafe();
                if (forward == 0 && strafe == 0) {
                    // crisp stop, packet forced out now
                    if (stStage > 0) {
                        player.setDeltaMovement(0.0, player.getDeltaMovement().y, 0.0);
                        player.hurtMarked = true;
                    }
                    stStage = 0;
                    stSpeed = 0.2873;
                    return;
                }
                if (player.isFallFlying() || player.onClimbable() || player.getVehicle() != null
                        || player.isShiftKeyDown() || player.isInWater() || player.isInLava()) {
                    return;
                }

                double defSpeed = 0.2873;
                if (PotionUtility.hasSpeed(player)) {
                    int amp = PotionUtility.getSpeedAmplifier(player);
                    defSpeed *= 1.0 + 0.2 * (amp + 1);
                }
                if (PotionUtility.hasSlowness(player)) {
                    int amp = PotionUtility.getSlownessAmplifier(player);
                    defSpeed /= 1.0 + 0.2 * (amp + 1);
                }

                double distance = Math.sqrt((player.getX() - player.xo) * (player.getX() - player.xo)
                    + (player.getZ() - player.zo) * (player.getZ() - player.zo));

                if (player.onGround() && stStage >= 3) {
                    stStage = 0;
                }

                switch (stStage) {
                    case 0 -> { // reset: wind up and hop immediately if on ground
                        stSpeed = 1.18 * defSpeed - 0.01;
                        if (player.onGround()) {
                            player.setDeltaMovement(player.getDeltaMovement().x, getHop(player, 0.40123128), player.getDeltaMovement().z);
                            stSpeed *= Modules.get(Speed.class).strafeSpeed;
                            stStage = 2;
                        } else if (player.getDeltaMovement().y > 0.2) {
                            stSpeed *= Modules.get(Speed.class).strafeSpeed;
                            stStage = 2;
                        } else {
                            stStage = 1;
                        }
                    }
                    case 1 -> { // hop
                        if (player.onGround()) {
                            player.setDeltaMovement(player.getDeltaMovement().x, getHop(player, 0.40123128), player.getDeltaMovement().z);
                            stSpeed *= Modules.get(Speed.class).strafeSpeed;
                            stStage = 2;
                        } else if (player.getDeltaMovement().y > 0.2) {
                            stSpeed *= Modules.get(Speed.class).strafeSpeed;
                            stStage = 2;
                        }
                    }
                    case 2 -> { // slowdown after the hop, predicted from distance
                        stSpeed = distance - 0.76 * (distance - defSpeed);
                        stStage = 3;
                    }
                    default -> { // decay, reset on collision
                        if (!player.level().noCollision(player.getBoundingBox().move(0.0, player.getDeltaMovement().y, 0.0))
                                || player.verticalCollision || player.onGround()) {
                            stStage = 0;
                        }
                        stSpeed = distance - (distance / 159.0);
                    }
                }

                stSpeed = Math.max(stSpeed, defSpeed);

                if (Modules.get(Speed.class).strafeLimit) {
                    long now = System.currentTimeMillis();
                    if (now - stLimitTimer > 2500L) {
                        stLimitTimer = now;
                    }
                    stSpeed = Math.min(stSpeed, now - stLimitTimer > 1250L ? 0.44 : 0.43);
                }

                double[] change = transformStrafe(player, stSpeed, forward, strafe);
                player.setDeltaMovement(change[0], player.getDeltaMovement().y, change[1]);
            }
            case "NCP" -> {
                float forward = getForward();
                float strafe = getStrafe();
                if (forward == 0 && strafe == 0) return;

                if (player.onGround()) {
                    double speedVal = baseSpeed * 0.10;
                    double[] dir = transformStrafe(player, speedVal, forward, strafe);
                    player.setDeltaMovement(dir[0], motion.y, dir[1]);
                    if (isJumping()) {
                        player.setDeltaMovement(player.getDeltaMovement().x, Modules.get(Speed.class).ncpJump, player.getDeltaMovement().z);
                    }
                } else if (Modules.enabled(KillAura.class) && KillAura.hasSilentRotations()) {
                    double horiz = Math.hypot(motion.x, motion.z);
                    double boost = player.getDeltaMovement().y < 0 ? baseSpeed * 0.03 : 0.0;
                    double targetSpd = Math.max(horiz * 0.96 + boost, 0.24);
                    double[] dir = transformStrafe(player, targetSpd, forward, strafe);
                    player.setDeltaMovement(dir[0], motion.y, dir[1]);
                } else if (player.getDeltaMovement().y < 0) {
                    double speedVal = baseSpeed * 0.06;
                    double[] dir = transformStrafe(player, speedVal, forward, strafe);
                    player.setDeltaMovement(
                        motion.x * 0.91 + dir[0] * 0.5,
                        motion.y,
                        motion.z * 0.91 + dir[1] * 0.5
                    );
                }
                applySpeedLimit(player, globalLimit);
            }
            case "NCPStrict" -> {
                float forward = getForward();
                float strafe = getStrafe();
                if (forward == 0 && strafe == 0) return;

                if (player.onGround()) {
                    double speedVal = baseSpeed * 0.08;
                    double[] dir = transformStrafe(player, speedVal, forward, strafe);
                    player.setDeltaMovement(dir[0], motion.y, dir[1]);
                    if (isJumping()) {
                        player.setDeltaMovement(player.getDeltaMovement().x, Modules.get(Speed.class).strictJump, player.getDeltaMovement().z);
                    }
                } else if (Modules.enabled(KillAura.class) && KillAura.hasSilentRotations()) {
                    double horiz = Math.hypot(motion.x, motion.z);
                    double targetSpd = Math.max(horiz * 0.96, 0.20);
                    double[] dir = transformStrafe(player, targetSpd, forward, strafe);
                    player.setDeltaMovement(dir[0], motion.y, dir[1]);
                } else if (player.getDeltaMovement().y < 0) {
                    double speedVal = baseSpeed * 0.04;
                    double[] dir = transformStrafe(player, speedVal, forward, strafe);
                    player.setDeltaMovement(
                        motion.x * 0.91 + dir[0] * 0.3,
                        motion.y,
                        motion.z * 0.91 + dir[1] * 0.3
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

                if (Modules.get(Speed.class).strafeStrictTimer) {
                    Speed.matrixTimer = 1.088f;
                } else {
                    Speed.matrixTimer = 1.0f;
                }

                double baseMoveSpeed = 0.2873;
                if (PotionUtility.hasSpeed(player)) {
                    int amp = PotionUtility.getSpeedAmplifier(player);
                    baseMoveSpeed *= 1.0 + 0.2 * (amp + 1);
                }
                if (PotionUtility.hasSlowness(player)) {
                    int amp = PotionUtility.getSlownessAmplifier(player);
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

                double cap = Modules.get(Speed.class).strafeStrictCap;
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

                double boost = Modules.get(Speed.class).grimBoost;
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
                double grimB = Modules.get(Speed.class).grimBoost;
                float currentSpeed = player.getAbilities().getWalkingSpeed();
                float targetSpeed = (float)(0.1 * (0.7 + grimB * 0.3));
                if (currentSpeed != targetSpeed) {
                    player.getAbilities().setWalkingSpeed(targetSpeed);
                }
            }
            case "Verus" -> {
                if (!player.onGround()) return;
                float forward = getForward();
                float strafe = getStrafe();
                if (forward == 0 && strafe == 0) return;
                double speedVal = Modules.get(Speed.class).verusSpeed * 0.15;
                double yaw = getMoveYaw(player);
                double velX = (-Math.sin(yaw) * forward + Math.cos(yaw) * strafe) * speedVal;
                double velZ = (Math.cos(yaw) * forward + Math.sin(yaw) * strafe) * speedVal;
                player.setDeltaMovement(velX, motion.y, velZ);
                double limit = Modules.get(Speed.class).verusSpeed;
                applySpeedLimit(player, Math.min(globalLimit, limit));
            }
        }
    }
}
