package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.RaveX;
import ravex.event.EventBusHolder;
import ravex.event.Subscribe;
import ravex.event.client.TickEvent;
import ravex.event.network.PacketEvent;
import ravex.mixin.network.AccessorServerboundMovePlayerPacket;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.utility.movement.MoveUtility;
import ravex.utility.network.NetworkUtility;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

@Module(name = "Flight", category = "Movement")
public class Flight {
    @Parameter(name = "Mode", modes = {"Vanilla", "Creative", "NCP", "Minemen", "Jetpack", "VerusFlat", "VerusDamage"})
    public String mode = "Vanilla";
    @Parameter(name = "Speed", min = 0.5, max = 20.0, step = 0.1)
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
    private boolean gotDamage = false;
    private int damageTicks = 0;
    private boolean shouldStop = false;

    public static double[] calculateVelocity(String mode, double hSpeed, double vSpeed, double glide, double yaw, double pitch, boolean jump, boolean sneak) {
        return javaCalculateVelocity(mode, hSpeed, vSpeed, glide, yaw, pitch, jump, sneak);
    }
    public static double handleAirFriction(String mode, double currentSpeed, double acceleration, double friction) {
        return javaHandleAirFriction(mode, currentSpeed, acceleration, friction);
    }
    public void onEnable() {
        RaveX.LOGGER.info("[Flight] Enabled with mode: {}", mode);
        gotDamage = false;
        damageTicks = 0;
        shouldStop = false;
        EventBusHolder.get().subscribe(this);
        if ("VerusDamage".equals(mode)) {
            var mc = MinecraftWrapper.getWrapper();
            var player = mc.getPlayer();
            if (player == null) return;
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();
            boolean hc = player.horizontalCollision;
            NetworkUtility.sendMoveRelative(x, y, z, false, hc);
            NetworkUtility.sendMoveRelative(x, y + 3.25, z, false, hc);
            NetworkUtility.sendMoveRelative(x, y, z, false, hc);
            NetworkUtility.sendMoveRelative(x, y, z, true, hc);
        }
    }
    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player != null) {
            player.getAbilities().flying = false;
            player.getAbilities().invulnerable = false;
        }
        if (player != null) {
            if ("VerusFlat".equals(mode)) {
                MoveUtility.setMotion(0, player.getDeltaMovement().y, 0);
                NetworkUtility.sendMoveRelative(
                    player.getX(), player.getY() - 0.5, player.getZ(),
                    false, player.horizontalCollision
                );
            }
            if ("VerusDamage".equals(mode)) {
                MoveUtility.setMotion(0, 0, 0);
            }
        }
        gotDamage = false;
        damageTicks = 0;
        shouldStop = false;
        EventBusHolder.get().unsubscribe(this);
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!event.isSend()) return;
        if (!Modules.enabled(Flight.class)) return;
        if (!"VerusFlat".equals(mode)) return;
        var packet = event.getPacket();
        if (packet instanceof ServerboundMovePlayerPacket move) {
            AccessorServerboundMovePlayerPacket accessor = (AccessorServerboundMovePlayerPacket) move;
            accessor.setOnGround(true);
        }
    }

    @Subscribe
    public void onTick(TickEvent.Client event) {
        if (!Modules.enabled(Flight.class)) return;

        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;

        if ("VerusFlat".equals(mode)) {
            double spd = speed;
            var input = player.input.keyPresses;
            double forward = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
            double strafe = (input.right() ? 1 : 0) - (input.left() ? 1 : 0);
            if (forward == 0.0 && strafe == 0.0) {
                MoveUtility.setMotion(0, 0, 0);
                return;
            }
            double yaw = player.getYRot();
            double sin = Math.sin(Math.toRadians(yaw));
            double cos = Math.cos(Math.toRadians(yaw));
            double mx = strafe * cos - forward * sin;
            double mz = forward * cos + strafe * sin;
            double len = Math.sqrt(mx * mx + mz * mz);
            if (len > 0.0) {
                mx /= len;
                mz /= len;
            }
            MoveUtility.setMotion(mx * spd, 0, mz * spd);
            return;
        }

        if ("VerusDamage".equals(mode)) {
            if (player.hurtTime > 0) {
                gotDamage = true;
            }

            if (!gotDamage) {
                return;
            }

            damageTicks++;
            if (damageTicks > 20 || shouldStop) {
                Modules.setEnabled(Flight.class, false);
                return;
            }

            var input = player.input.keyPresses;
            double forward = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
            double strafe = (input.right() ? 1 : 0) - (input.left() ? 1 : 0);
            double yaw = player.getYRot();
            double sin = Math.sin(Math.toRadians(yaw));
            double cos = Math.cos(Math.toRadians(yaw));
            double mx = strafe * cos - forward * sin;
            double mz = forward * cos + strafe * sin;
            double len = Math.sqrt(mx * mx + mz * mz);
            if (len > 0.0) {
                mx /= len;
                mz /= len;
            }
            player.setDeltaMovement(mx * 9.95, 0, mz * 9.95);
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
