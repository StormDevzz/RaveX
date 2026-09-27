package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.world.entity.MoverType;
import ravex.utility.movement.MoveUtility;
import ravex.RaveX;

import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.player.InventoryUtility;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "Elytra++", category = "Movement")
public class ElytraFly {
    @Parameter(name = "Mode", modes = {"Vanilla", "Control", "NCP", "Fireworks", "OldGrim"})
    public String mode = "Vanilla";
    @Parameter(name = "H-Speed", min = 0.1, max = 5.0, step = 0.1)
    public double hSpeed = 1.5;
    @Parameter(name = "V-Speed", min = 0.1, max = 5.0, step = 0.1)
    public double vSpeed = 1.0;
    @Parameter(name = "Glide", min = 0.001, max = 0.1, step = 0.001)
    public double glide = 0.005;
    @Parameter(name = "FireworkDelay", min = 1.0, max = 30.0, step = 1.0)
    public double fireworkDelay = 10.0;
    @Parameter(name = "GrimRotate")
    public boolean grimRotate = true;
    @Parameter(name = "GrimExtender")
    public boolean grimExtender = true;
    @Parameter(name = "AutoTakeoff")
    public boolean autoTakeoff = true;
    @Parameter(name = "SpeedControl")
    public boolean speedControl = true;
    @Parameter(name = "Accelerate")
    public boolean accelerate = false;
    @Parameter(name = "Acceleration", min = 0.01, max = 1.0, step = 0.01)
    public double acceleration = 0.15;
    @Parameter(name = "Timer", min = 0.5, max = 3.0, step = 0.1)
    public double timer = 1.0;
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_elytraplusplus");
    public static native void nativeCalculateVelocity(
        String mode, double hSpeed, double vSpeed, double glide,
        double yaw, double pitch, boolean jump, boolean sneak,
        double[] outVel
    );
    public static native void nativeApplyBypass(
        String mode, double[] motion, double yaw, double pitch,
        boolean jump, boolean sneak, boolean ground, double[] outMotion
    );
    private int fwTimer = 0;
    private double accelMul = 0.0;
    private double grimAccel = 0.0;
    private double grimAccelY = 0.0;
    private net.minecraft.world.phys.Vec3 grimZone = null;
    private long grimStartTime = 0L;
    private int elytraTicks = 0;
    public net.minecraft.world.phys.Vec3 applyTimerAndAccel(net.minecraft.world.phys.Vec3 vel) {
        double t = timer;
        if (t != 1.0) {
            vel = new net.minecraft.world.phys.Vec3(vel.x * t, vel.y, vel.z * t);
        }
        if (accelerate) {
            vel = new net.minecraft.world.phys.Vec3(vel.x * accelMul, vel.y * accelMul, vel.z * accelMul);
        }
        return vel;
    }
    private void updateAccelState() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getOptions() == null) return;
        boolean moving = mc.getOptions().keyUp.isDown() || mc.getOptions().keyDown.isDown() ||
                         mc.getOptions().keyLeft.isDown() || mc.getOptions().keyRight.isDown() ||
                         mc.getOptions().keyJump.isDown() || mc.getOptions().keyShift.isDown();
        if (accelerate) {
            if (moving) {
                accelMul = Math.min(accelMul + acceleration, 1.0);
            } else {
                accelMul = Math.max(accelMul - acceleration * 2.0, 0.0);
            }
        } else {
            accelMul = 1.0;
        }
    }
    public static double[] calculateVelocity(
        String mode, double hSpeed, double vSpeed, double glide,
        double yaw, double pitch, boolean jump, boolean sneak
    ) {
        if (NATIVE.isLoaded() && !mode.equals("Control")) {
            try {
                double[] out = new double[3];
                nativeCalculateVelocity(mode, hSpeed, vSpeed, glide, yaw, pitch, jump, sneak, out);
                return out;
            } catch (Exception e) { }
        }
        return javaCalculateVelocity(mode, hSpeed, vSpeed, glide, yaw, pitch, jump, sneak);
    }
    public static double[] applyBypass(
        String mode, double[] motion, double yaw, double pitch,
        boolean jump, boolean sneak, boolean ground
    ) {
        if (NATIVE.isLoaded() && !mode.equals("Control")) {
            try {
                double[] out = new double[3];
                nativeApplyBypass(mode, motion, yaw, pitch, jump, sneak, ground, out);
                return out;
            } catch (Exception e) { }
        }
        return javaApplyBypass(mode, motion, yaw, pitch, jump, sneak, ground);
    }
    private static double[] javaCalculateVelocity(
        String mode, double hSpeed, double vSpeed, double glide,
        double yaw, double pitch, boolean jump, boolean sneak
    ) {
        double rad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double forward = jump ? 1.0 : (sneak ? -1.0 : 0.0);
        float fwd = 0, str = 0;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getOptions() != null) {
            if (mc.getOptions().keyUp.isDown()) fwd++;
            if (mc.getOptions().keyDown.isDown()) fwd--;
            if (mc.getOptions().keyLeft.isDown()) str++;
            if (mc.getOptions().keyRight.isDown()) str--;
        }
        double velX, velY, velZ;
        switch (mode) {
            case "Control" -> {
                velX = (-Math.sin(rad) * fwd + Math.cos(rad) * str) * hSpeed;
                velY = jump ? vSpeed : (sneak ? -vSpeed : -glide);
                velZ = (Math.cos(rad) * fwd + Math.sin(rad) * str) * hSpeed;
            }
            case "Vanilla" -> {
                velX = -Math.sin(rad) * Math.cos(pitchRad) * hSpeed;
                velY = Math.sin(pitchRad) * hSpeed;
                velZ = Math.cos(rad) * Math.cos(pitchRad) * hSpeed;
                if (!jump && !sneak) {
                    velY = -glide;
                } else if (!jump && sneak) {
                    velY = -vSpeed;
                } else if (jump) {
                    velY = Math.sin(pitchRad) * vSpeed;
                }
            }
            case "NCP" -> {
                double forwardFactor = jump || sneak || mc.getOptions().keyUp.isDown() ? 1.0 : 0.0;
                velX = -Math.sin(rad) * Math.cos(pitchRad) * hSpeed * forwardFactor;
                velY = jump ? vSpeed : (sneak ? -vSpeed : -glide);
                velZ = Math.cos(rad) * Math.cos(pitchRad) * hSpeed * forwardFactor;
            }
            case "Fireworks" -> {
                velX = (-Math.sin(rad) * fwd + Math.cos(rad) * str) * hSpeed;
                velY = jump ? vSpeed : (sneak ? -vSpeed : -glide);
                velZ = (Math.cos(rad) * fwd + Math.sin(rad) * str) * hSpeed;
            }
            case "OldGrim" -> {
                velX = (-Math.sin(rad) * fwd + Math.cos(rad) * str) * hSpeed;
                velY = jump ? vSpeed : (sneak ? -vSpeed : -glide);
                velZ = (Math.cos(rad) * fwd + Math.sin(rad) * str) * hSpeed;
            }
            default -> {
                velX = 0; velY = 0; velZ = 0;
            }
        }
        return new double[]{velX, velY, velZ};
    }
    private static double[] javaApplyBypass(
        String mode, double[] motion, double yaw, double pitch,
        boolean jump, boolean sneak, boolean ground
    ) {
        double mx = motion[0], my = motion[1], mz = motion[2];
        var mc = MinecraftWrapper.getWrapper();
        float forward = 0, strafe = 0;
        if (mc.getOptions() != null && mc.getPlayer() != null) {
            if (mc.getOptions().keyUp.isDown()) forward++;
            if (mc.getOptions().keyDown.isDown()) forward--;
            if (mc.getOptions().keyLeft.isDown()) strafe++;
            if (mc.getOptions().keyRight.isDown()) strafe--;
        }
        switch (mode) {
            case "Control" -> {
                double fwd = forward;
                double str = strafe;
                if (fwd == 0 && str == 0 && !jump && !sneak) {
                    return new double[]{mx * 0.2, 0, mz * 0.2};
                }
                double rad = Math.toRadians(yaw);
                double targetX = (-Math.sin(rad) * fwd + Math.cos(rad) * str) * 0.3;
                double targetZ = (Math.cos(rad) * fwd + Math.sin(rad) * str) * 0.3;
                return new double[]{
                    mx + (targetX - mx) * 0.3,
                    jump ? 0.6 : (sneak ? -0.6 : my * 0.9),
                    mz + (targetZ - mz) * 0.3
                };
            }
            case "Vanilla" -> {
                return new double[]{mx * 0.99, my * 0.99, mz * 0.99};
            }
            case "NCP" -> {
                double factor = 0.99;
                return new double[]{mx * factor, my * factor, mz * factor};
            }
            case "Fireworks" -> {
                return new double[]{mx * 0.99, my * 0.99, mz * 0.99};
            }
            case "OldGrim" -> {
                return new double[]{mx * 0.99, my * 0.99, mz * 0.99};
            }
        }
        return new double[]{mx, my, mz};
    }
    public void onEnable() {
        RaveX.LOGGER.info("[Elytra++] Enabled with mode: {}", mode);
        fwTimer = 0;
        accelMul = 0.0;
        grimAccel = 0.0;
        grimAccelY = 0.0;
        elytraTicks = 0;
        grimStartTime = System.currentTimeMillis();
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() != null)
            grimZone = mc.getPlayer().position();
        else
            grimZone = null;
    }
    public void onDisable() {
        fwTimer = 0;
        grimAccel = 0.0;
        grimAccelY = 0.0;
        elytraTicks = 0;
        grimZone = null;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getGameMode() == null) return;
        updateAccelState();
        if (autoTakeoff && mc.getPlayer().onGround() && !mc.getPlayer().isFallFlying()) {
            if (mc.getOptions().keyJump.isDown()) {
                mc.getPlayer().jumpFromGround();
            }
        }
        if (!mc.getPlayer().isFallFlying()) {
            elytraTicks = 0;
            return;
        }
        elytraTicks++;
        double yaw = mc.getPlayer().getYRot();
        double pitch = mc.getPlayer().getXRot();
        boolean space = mc.getOptions().keyJump.isDown();
        boolean shift = mc.getOptions().keyShift.isDown();
        float forward = 0, strafe = 0;
        if (mc.getOptions().keyUp.isDown()) forward++;
        if (mc.getOptions().keyDown.isDown()) forward--;
        if (mc.getOptions().keyLeft.isDown()) strafe++;
        if (mc.getOptions().keyRight.isDown()) strafe--;
        if ("OldGrim".equals(mode)) {
            tickOldGrim(mc, space, shift, forward, strafe, yaw, pitch);
            return;
        }
        if (speedControl) {
            String curMode = mode;
            if (curMode.equals("Control") || curMode.equals("Fireworks")) {
                double rad = Math.toRadians(yaw);
                double targetX = (-Math.sin(rad) * forward + Math.cos(rad) * strafe) * hSpeed;
                double targetZ = (Math.cos(rad) * forward + Math.sin(rad) * strafe) * hSpeed;
                double targetY = space ? vSpeed : (shift ? -vSpeed : -glide);
                net.minecraft.world.phys.Vec3 vel;
                if (forward == 0 && strafe == 0 && !space && !shift) {
                    net.minecraft.world.phys.Vec3 m = mc.getPlayer().getDeltaMovement();
                    vel = new net.minecraft.world.phys.Vec3(m.x * 0.2, -glide, m.z * 0.2);
                } else {
                    vel = new net.minecraft.world.phys.Vec3(targetX, targetY, targetZ);
                }
                vel = applyTimerAndAccel(vel);
                MoveUtility.setMotion(vel);
                mc.getPlayer().move(MoverType.SELF, vel);
                if ("Fireworks".equals(curMode)) {
                    fwTimer++;
                    if (fwTimer >= (int) fireworkDelay) {
                        useFirework(mc);
                        fwTimer = 0;
                    }
                }
            } else {
                double[] vel = calculateVelocity(
                    curMode, hSpeed, vSpeed, glide,
                    yaw, pitch, space, shift
                );
                net.minecraft.world.phys.Vec3 v = new net.minecraft.world.phys.Vec3(vel[0], vel[1], vel[2]);
                v = applyTimerAndAccel(v);
                MoveUtility.setMotion(v);
                mc.getPlayer().move(MoverType.SELF, v);
            }
        }
    }


    private void tickOldGrim(MinecraftWrapper mc, boolean space, boolean shift, float forward, float strafe, double yaw, double pitch) {
        var player = mc.getPlayer();
        if (player == null) return;
        if (player.horizontalCollision || player.verticalCollision) {
            grimAccel = 0.0;
            grimAccelY = 0.0;
        }
        if (grimRotate && (forward != 0 || strafe != 0)) {
            double offset = 0.0;
            if (forward > 0) {
                offset = -strafe * 45.0;
            } else if (forward < 0) {
                offset = 180.0 + strafe * 45.0;
            } else {
                offset = -strafe * 90.0;
            }
            player.setYRot((float) (yaw + offset));
            yaw = player.getYRot();
        }
        if (elytraTicks < 4) {
            player.setXRot(-45f);
        } else if (space && forward == 0 && strafe == 0) {
            player.setXRot(-90f);
        } else if (space) {
            player.setXRot(-45f);
        } else if (shift) {
            player.setXRot(45f);
        }
        double ramp = Math.min((grimAccel += 9.0) / 100.0, 1.0);
        double rampY = Math.min((grimAccelY += 9.0) / 100.0, 1.0);
        if (!MoveUtility.isMoving() && !space && !shift)
            grimAccel = 0.0;
        double rad = Math.toRadians(player.getYRot());
        double targetX = (-Math.sin(rad) * forward + Math.cos(rad) * strafe) * hSpeed * ramp;
        double targetZ = (Math.cos(rad) * forward + Math.sin(rad) * strafe) * hSpeed * ramp;
        double targetY;
        if (space) {
            targetY = vSpeed * rampY;
        } else if (shift) {
            targetY = -vSpeed * rampY;
        } else {
            targetY = (player.tickCount % 2 == 0 ? 0.08 : -0.08);
        }
        net.minecraft.world.phys.Vec3 vel = new net.minecraft.world.phys.Vec3(targetX, targetY, targetZ);
        vel = applyTimerAndAccel(vel);
        MoveUtility.setMotion(vel);
        player.move(MoverType.SELF, vel);
        fwTimer++;
        if (fwTimer >= (int) fireworkDelay) {
            if (grimExtender && grimZone != null && System.currentTimeMillis() - grimStartTime < 50000L) {
                double dx = player.getX() - grimZone.x;
                double dz = player.getZ() - grimZone.z;
                if (dx * dx + dz * dz < 7000.0)
                    return;
            }
            useFirework(mc);
            fwTimer = 0;
            grimZone = player.position();
            grimStartTime = System.currentTimeMillis();
        }
    }

    private void useFirework(MinecraftWrapper mc) {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            if (InventoryUtility.isItem(InventoryUtility.getItem(mc.getPlayer(), i), "firework_rocket")) {
                slot = i;
                break;
            }
        }
        if (slot < 0) return;
        int prevSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
        InventoryUtility.selectSlot(mc.getPlayer(), slot);
        mc.getGameMode().useItem(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
        InventoryUtility.selectSlot(mc.getPlayer(), prevSlot);
    }


}