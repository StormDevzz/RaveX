package ravex.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import ravex.RaveX;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;

import java.util.List;

public class ElytraFly extends Module {
    public static final ElytraFly INSTANCE = new ElytraFly();

    public final ModeParameter mode = new ModeParameter("Mode", "Vanilla",
        List.of("Vanilla", "Control", "Grim", "NCP", "Minemen", "Packet", "Boost", "TickShift"));
    public final NumberParameter hSpeed = new NumberParameter("H-Speed", 1.5, 0.1, 5.0, 0.1);
    public final NumberParameter vSpeed = new NumberParameter("V-Speed", 1.0, 0.1, 5.0, 0.1);
    public final NumberParameter glide = new NumberParameter("Glide", 0.005, 0.001, 0.1, 0.001);
    public final BooleanParameter autoFirework = new BooleanParameter("Auto Firework", false);
    public final NumberParameter fireworkDelay = new NumberParameter("Firework Delay", 10.0, 1.0, 30.0, 1.0);
    public final NumberParameter fireworkBoost = new NumberParameter("Firework Boost", 1.0, 0.5, 5.0, 0.1);
    public final BooleanParameter autoTakeoff = new BooleanParameter("Auto Takeoff", true);
    public final BooleanParameter speedControl = new BooleanParameter("Speed Control", true);
    public final BooleanParameter accelerate = new BooleanParameter("Accelerate", false);
    public final NumberParameter acceleration = new NumberParameter("Acceleration", 0.15, 0.01, 1.0, 0.01);
    public final NumberParameter timer = new NumberParameter("Timer", 1.0, 0.5, 3.0, 0.1);
    public final BooleanParameter fallBypass = new BooleanParameter("Fall Bypass", true);

    private static boolean nativeLoaded = false;
    static {
        try {
            nativeLoaded = ravex.utility.misc.NativeLoader.loadLibrary("ravex_elytraplusplus");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[ElytraFly JNI] Native lib load failed: " + e.getMessage());
        }
    }

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

    public Vec3 applyTimerAndAccel(Vec3 vel) {
        double t = timer.getValue();
        if (t != 1.0) {
            vel = new Vec3(vel.x * t, vel.y, vel.z * t);
        }
        if (accelerate.getValue()) {
            vel = new Vec3(vel.x * accelMul, vel.y * accelMul, vel.z * accelMul);
        }
        return vel;
    }

    private void updateAccelState() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        boolean moving = mc.options.keyUp.isDown() || mc.options.keyDown.isDown() ||
                         mc.options.keyLeft.isDown() || mc.options.keyRight.isDown() ||
                         mc.options.keyJump.isDown() || mc.options.keyShift.isDown();
        if (accelerate.getValue()) {
            if (moving) {
                accelMul = Math.min(accelMul + acceleration.getValue(), 1.0);
            } else {
                accelMul = Math.max(accelMul - acceleration.getValue() * 2.0, 0.0);
            }
        } else {
            accelMul = 1.0;
        }
    }

    private ElytraFly() {
        super("Elytra++", Category.MOVEMENT);
        addParameter(mode);
        addParameter(hSpeed);
        addParameter(vSpeed);
        addParameter(glide);
        addParameter(autoFirework);
        addParameter(fireworkDelay);
        addParameter(fireworkBoost);
        addParameter(autoTakeoff);
        addParameter(speedControl);
        addParameter(accelerate);
        addParameter(acceleration);
        addParameter(timer);
        addParameter(fallBypass);
        fireworkDelay.setVisible(() -> autoFirework.getValue());
        fireworkBoost.setVisible(() -> autoFirework.getValue());
        acceleration.setVisible(() -> accelerate.getValue());
    }

    public static double[] calculateVelocity(
        String mode, double hSpeed, double vSpeed, double glide,
        double yaw, double pitch, boolean jump, boolean sneak
    ) {
        if (nativeLoaded && !mode.equals("Control")) {
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
        if (nativeLoaded && !mode.equals("Control")) {
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            if (mc.options.keyUp.isDown()) fwd++;
            if (mc.options.keyDown.isDown()) fwd--;
            if (mc.options.keyLeft.isDown()) str++;
            if (mc.options.keyRight.isDown()) str--;
        }

        double velX, velY, velZ;

        switch (mode) {
            case "Control" -> {
                velX = (-Math.sin(rad) * fwd + Math.cos(rad) * str) * hSpeed;
                velY = jump ? vSpeed : (sneak ? -vSpeed : -glide);
                velZ = (Math.cos(rad) * fwd + Math.sin(rad) * str) * hSpeed;
            }
            case "Vanilla", "Grim", "NCP", "Minemen" -> {
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
            case "Packet" -> {
                velX = -Math.sin(rad) * hSpeed * forward;
                velY = jump ? vSpeed : (sneak ? -vSpeed : -glide);
                velZ = Math.cos(rad) * hSpeed * forward;
            }
            case "Boost" -> {
                velX = -Math.sin(rad) * Math.cos(pitchRad) * hSpeed;
                velY = -Math.sin(pitchRad) * hSpeed;
                velZ = Math.cos(rad) * Math.cos(pitchRad) * hSpeed;
                if (forward == 0) {
                    velY = -glide;
                }
            }
            case "TickShift" -> {
                velX = -Math.sin(rad) * Math.cos(pitchRad) * hSpeed * 0.5;
                velY = -Math.sin(pitchRad) * hSpeed * 0.3;
                velZ = Math.cos(rad) * Math.cos(pitchRad) * hSpeed * 0.5;
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
        Minecraft mc = Minecraft.getInstance();
        float forward = 0, strafe = 0;
        if (mc.options != null && mc.player != null) {
            if (mc.options.keyUp.isDown()) forward++;
            if (mc.options.keyDown.isDown()) forward--;
            if (mc.options.keyLeft.isDown()) strafe++;
            if (mc.options.keyRight.isDown()) strafe--;
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
            case "Grim" -> {
                double factor = 0.98;
                return new double[]{mx * factor, Math.max(my, -0.1) * factor, mz * factor};
            }
            case "NCP" -> {
                double factor = 0.95;
                return new double[]{mx * factor, my * factor, mz * factor};
            }
            case "Minemen" -> {
                double factor = 0.97;
                return new double[]{mx * factor, Math.min(my, 0.0) * factor, mz * factor};
            }
            case "Packet" -> {
                return new double[]{mx, my, mz};
            }
            case "Boost" -> {
                return new double[]{mx * 0.95, Math.max(my, -0.2), mz * 0.95};
            }
            case "TickShift" -> {
                return new double[]{mx * 0.92, my * 0.96, mz * 0.92};
            }
        }
        return new double[]{mx, my, mz};
    }

    @Override
    protected void onEnable() {
        RaveX.LOGGER.info("[Elytra++] Enabled with mode: {}", mode.getValue());
        fwTimer = 0;
        accelMul = 0.0;
    }

    @Override
    protected void onDisable() {
        fwTimer = 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;

        updateAccelState();

        if (autoTakeoff.getValue() && mc.player.onGround() && !mc.player.isFallFlying()) {
            if (mc.options.keyJump.isDown()) {
                mc.player.jumpFromGround();
            }
        }

        if (!mc.player.isFallFlying()) return;

        double yaw = mc.player.getYRot();
        double pitch = mc.player.getXRot();
        boolean space = mc.options.keyJump.isDown();
        boolean shift = mc.options.keyShift.isDown();
        float forward = 0, strafe = 0;
        if (mc.options.keyUp.isDown()) forward++;
        if (mc.options.keyDown.isDown()) forward--;
        if (mc.options.keyLeft.isDown()) strafe++;
        if (mc.options.keyRight.isDown()) strafe--;

        if (speedControl.getValue()) {
            String curMode = mode.getValue();

            if (curMode.equals("Control")) {
                double rad = Math.toRadians(yaw);
                double targetX = (-Math.sin(rad) * forward + Math.cos(rad) * strafe) * hSpeed.getValue();
                double targetZ = (Math.cos(rad) * forward + Math.sin(rad) * strafe) * hSpeed.getValue();
                double targetY = space ? vSpeed.getValue() : (shift ? -vSpeed.getValue() : -glide.getValue());
                Vec3 vel;
                if (forward == 0 && strafe == 0 && !space && !shift) {
                    Vec3 m = mc.player.getDeltaMovement();
                    vel = new Vec3(m.x * 0.2, -glide.getValue(), m.z * 0.2);
                } else {
                    vel = new Vec3(targetX, targetY, targetZ);
                }
                vel = applyTimerAndAccel(vel);
                mc.player.setDeltaMovement(vel);
                mc.player.move(MoverType.SELF, vel);
            } else {
                double[] vel = calculateVelocity(
                    curMode, hSpeed.getValue(), vSpeed.getValue(), glide.getValue(),
                    yaw, pitch, space, shift
                );
                Vec3 v = new Vec3(vel[0], vel[1], vel[2]);
                v = applyTimerAndAccel(v);
                mc.player.setDeltaMovement(v);
                mc.player.move(MoverType.SELF, v);
            }
        }

        if (autoFirework.getValue()) {
            fwTimer++;
            if (fwTimer >= fireworkDelay.getValue().intValue()) {
                useFirework(mc);
                fwTimer = 0;
            }
        }
    }

    private void useFirework(Minecraft mc) {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.FIREWORK_ROCKET)) {
                slot = i;
                break;
            }
        }
        if (slot < 0) return;

        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        mc.player.getInventory().setSelectedSlot(prevSlot);
    }
}
