package ravex.modules.movement;

import ravex.RaveX;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;

import java.util.List;

public class Flight extends Module {
    public static final Flight INSTANCE = new Flight();

    public final ModeParameter mode = new ModeParameter("Mode", "Vanilla",
        List.of("Vanilla", "Creative", "NCP", "Grim", "Minemen", "Jetpack"));
    public final NumberParameter speed = new NumberParameter("Speed", 2.0, 0.5, 10.0, 0.1);
    public final NumberParameter verticalSpeed = new NumberParameter("Vertical Speed", 1.0, 0.1, 5.0, 0.1);
    public final NumberParameter glide = new NumberParameter("Glide", 0.0, 0.0, 1.0, 0.05);
    public final NumberParameter timer = new NumberParameter("Timer", 1.0, 0.5, 3.0, 0.1);
    public final NumberParameter acceleration = new NumberParameter("Acceleration", 1.0, 0.1, 5.0, 0.1);
    public final BooleanParameter autoSneak = new BooleanParameter("Auto Sneak", false);
    public final BooleanParameter damageBoost = new BooleanParameter("Damage Boost", false);
    public final NumberParameter damageMultiplier = new NumberParameter("Damage Multiplier", 1.5, 1.0, 5.0, 0.1);

    private static final boolean nativeAvailable = false;

    public static double[] calculateVelocity(String mode, double hSpeed, double vSpeed, double glide, double yaw, double pitch, boolean jump, boolean sneak) {
        return javaCalculateVelocity(mode, hSpeed, vSpeed, glide, yaw, pitch, jump, sneak);
    }

    public static double handleAirFriction(String mode, double currentSpeed, double acceleration, double friction) {
        return javaHandleAirFriction(mode, currentSpeed, acceleration, friction);
    }

    private Flight() {
        super("Flight", Category.MOVEMENT);
        addParameter(mode);
        addParameter(speed);
        addParameter(verticalSpeed);
        addParameter(glide);
        addParameter(timer);
        addParameter(acceleration);
        addParameter(autoSneak);
        addParameter(damageBoost);
        addParameter(damageMultiplier);
        damageMultiplier.setVisible(() -> damageBoost.getValue());
    }

    @Override
    public void onEnable() {
        RaveX.LOGGER.info("[Flight] Enabled with mode: {}", mode.getValue());
    }

    @Override
    public void onDisable() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().invulnerable = false;
        }
    }

    private static double[] javaCalculateVelocity(String mode, double hSpeed, double vSpeed, double glide, double yaw, double pitch, boolean jump, boolean sneak) {
        double rad = Math.toRadians(yaw);
        double forward = 0, strafe = 0;
        if (jump) forward = 1;
        if (sneak) forward = -1;

        double velX = -Math.sin(rad) * forward * hSpeed;
        double velZ = Math.cos(rad) * forward * hSpeed;
        double velY = 0;

        switch (mode) {
            case "Creative":
            case "Vanilla":
                velY = jump ? vSpeed : (sneak ? -vSpeed : -glide);
                break;
            case "NCP":
            case "Minemen":
                velY = jump ? vSpeed : (sneak ? -vSpeed : -glide);
                break;
            case "Grim":
                velY = jump ? vSpeed * 0.8 : (sneak ? -vSpeed * 0.8 : -glide * 0.5);
                break;
            case "Jetpack":
                velY = jump ? vSpeed : -glide * 0.3;
                break;
        }
        return new double[]{velX, velY, velZ};
    }

    private static double javaHandleAirFriction(String mode, double currentSpeed, double acceleration, double friction) {
        if (mode.equals("NCP") || mode.equals("Grim")) {
            return currentSpeed * (1.0 - friction * 0.05);
        }
        return Math.min(currentSpeed + acceleration, currentSpeed * (1.0 + acceleration * 0.1));
    }
}
