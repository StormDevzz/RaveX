package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.RaveX;

import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "Flight", category = "Movement")
public class Flight {
    @Parameter(name = "Mode", modes = {"Vanilla", "Creative", "NCP", "Minemen", "Jetpack"})
    public String mode = "Vanilla";
    @Parameter(name = "Speed", min = 0.5, max = 10.0, step = 0.1)
    public double speed = 2.0;
    @Parameter(name = "VerticalSpeed", min = 0.1, max = 5.0, step = 0.1)
    public double verticalSpeed = 1.0;
    @Parameter(name = "Glide", min = 0.0, max = 1.0, step = 0.05)
    public double glide = 0.0;
    @Parameter(name = "Timer", min = 0.5, max = 3.0, step = 0.1)
    public double timer = 1.0;
    @Parameter(name = "Acceleration", min = 0.1, max = 5.0, step = 0.1)
    public double acceleration = 1.0;
    @Parameter(name = "AutoSneak")
    public boolean autoSneak = false;
    @Parameter(name = "DamageBoost")
    public boolean damageBoost = false;
    @Parameter(name = "DamageMultiplier", min = 1.0, max = 5.0, step = 0.1)
    public double damageMultiplier = 1.5;
    private static final boolean nativeAvailable = false;
    public static double[] calculateVelocity(String mode, double hSpeed, double vSpeed, double glide, double yaw, double pitch, boolean jump, boolean sneak) {
        return javaCalculateVelocity(mode, hSpeed, vSpeed, glide, yaw, pitch, jump, sneak);
    }
    public static double handleAirFriction(String mode, double currentSpeed, double acceleration, double friction) {
        return javaHandleAirFriction(mode, currentSpeed, acceleration, friction);
    }
    public void onEnable() {
        RaveX.LOGGER.info("[Flight] Enabled with mode: {}", mode);
    }
    public void onDisable() {
        var mc = MinecraftWrapper.getInstance();
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

            case "Jetpack":
                velY = jump ? vSpeed : -glide * 0.3;
                break;
        }
        return new double[]{velX, velY, velZ};
    }


    private static double javaHandleAirFriction(String mode, double currentSpeed, double acceleration, double friction) {
        if (mode.equals("NCP")) {
            return currentSpeed * (1.0 - friction * 0.05);
        }
        return Math.min(currentSpeed + acceleration, currentSpeed * (1.0 + acceleration * 0.1));
    }


}