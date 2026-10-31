package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.RaveX;
import ravex.event.Subscribe;
import ravex.event.client.TickEvent;
import ravex.event.network.PacketEvent;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.utility.movement.MoveUtility;
import ravex.utility.network.NetworkUtility;

@Module(name = "Flight", category = "Movement")
public class Flight {
    @Parameter(name = "Mode", modes = {"Vanilla", "Creative", "NCP", "Jetpack", "VerusFlat", "VerusDamage"})
    public String mode = "Vanilla";
    @Parameter(name = "HorSpeed", min = 0.1, max = 20.0, step = 0.1, visible = "mode=Vanilla||mode=Creative||mode=NCP||mode=Jetpack||mode=VerusFlat")
    public double horSpeed = 1.0;
    @Parameter(name = "VerSpeed", min = 0.1, max = 5.0, step = 0.1, visible = "mode=Vanilla||mode=Creative||mode=NCP||mode=Jetpack")
    public double verSpeed = 1.0;
    @Parameter(name = "Glide", min = 0.0, max = 1.0, step = 0.05, visible = "mode=Vanilla||mode=Creative||mode=NCP||mode=Jetpack")
    public double glide = 0.1;
    @Parameter(name = "Timer", min = 0.5, max = 3.0, step = 0.1, visible = "mode=Vanilla||mode=Creative||mode=NCP||mode=Jetpack")
    public double timer = 1.0;
    @Parameter(name = "Acceleration", min = 0.1, max = 5.0, step = 0.1, visible = "mode=Vanilla||mode=Creative||mode=NCP||mode=Jetpack")
    public double acceleration = 1.0;
    @Parameter(name = "DamageSpeed", min = 1.0, max = 20.0, step = 0.5, visible = "mode=VerusDamage")
    public double damageSpeed = 9.95;
    // server kicks players who stay airborne too long,
    // the packet nudge resets its counter
    @Parameter(name = "AntiKick", modes = {"Off", "Packet"})
    public String antiKickMode = "Packet";
    @Parameter(name = "AntiKickDelay", min = 1, max = 200, step = 1, visible = "antiKickMode=Packet")
    public double antiKickDelay = 20;
    @Parameter(name = "AntiKickOffTime", min = 1, max = 20, step = 1, visible = "antiKickMode=Packet")
    public double antiKickOffTime = 1;
    private static final boolean nativeAvailable = false;
    private boolean gotDamage = false;
    private int damageTicks = 0;
    private boolean shouldStop = false;
    private int akDelayLeft = 20;
    private int akOffLeft = 1;
    private double akLastY = Double.MAX_VALUE;
    private float akLastYaw;
    private boolean akFlip;

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
        akDelayLeft = (int) antiKickDelay;
        akOffLeft = (int) antiKickOffTime;
        akLastY = Double.MAX_VALUE;
        akFlip = false;
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
        akLastY = Double.MAX_VALUE;
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!event.isSend()) return;
        if (!Modules.enabled(Flight.class)) return;
        var packet = event.getPacket();
        if (!NetworkUtility.isMovePacket(packet)) return;
        if ("VerusFlat".equals(mode)) {
            NetworkUtility.setMoveOnGround(packet, true);
        }
        if ("Packet".equals(antiKickMode)) {
            applyAntiKickPacket(packet);
        }
    }

    // a tiny descent keeps the floating counter low
    private void applyAntiKickPacket(net.minecraft.network.protocol.Packet<?> packet) {
        if (!NetworkUtility.isPositionMove(packet)) return;
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || player.onGround()) return;
        double currentY = NetworkUtility.movePacketY(packet);
        if (akDelayLeft <= 0 && akLastY != Double.MAX_VALUE && shouldFlyDown(currentY)) {
            NetworkUtility.setMovePacketY(packet, akLastY - 0.03130);
        } else {
            akLastY = currentY;
        }
    }

    private boolean shouldFlyDown(double currentY) {
        if (currentY >= akLastY) return true;
        return akLastY - currentY < 0.03130;
    }

    private void updateAntiKick(net.minecraft.client.player.LocalPlayer player) {
        if ("Off".equals(antiKickMode)) return;
        // frozen yaw trips some servers, nudge it
        float currentYaw = player.getYRot();
        if (player.fallDistance >= 3f && currentYaw == akLastYaw
                && player.getDeltaMovement().length() < 0.003d) {
            player.setYRot(currentYaw + (akFlip ? 1 : -1));
            akFlip = !akFlip;
        }
        akLastYaw = currentYaw;

        if (akDelayLeft > 0) akDelayLeft--;
        if (akOffLeft <= 0 && akDelayLeft <= 0) {
            akDelayLeft = (int) antiKickDelay;
            akOffLeft = (int) antiKickOffTime;
        } else if (akDelayLeft <= 0) {
            akOffLeft--;
        }
    }

    @Subscribe
    public void onTick(TickEvent.Client event) {
        if (!Modules.enabled(Flight.class)) return;

        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;

        updateAntiKick(player);

        if ("VerusFlat".equals(mode)) {
            double spd = horSpeed;
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
            player.setDeltaMovement(mx * damageSpeed, 0, mz * damageSpeed);
        }
    }
    private static double[] javaCalculateVelocity(String mode, double hSpeed, double vSpeed, double glide, double yaw, double pitch, boolean jump, boolean sneak) {
        double rad = Math.toRadians(yaw);
        // wasd only, sneak is not treated as backward
        double forward = 0;
        double strafe = 0;
        var mc = MinecraftWrapper.getInstance();
        if (mc.options != null) {
            if (mc.options.keyUp.isDown()) forward += 1;
            if (mc.options.keyDown.isDown()) forward -= 1;
            if (mc.options.keyLeft.isDown()) strafe += 1;
            if (mc.options.keyRight.isDown()) strafe -= 1;
        }
        double velX = (-Math.sin(rad) * forward + Math.cos(rad) * strafe) * hSpeed;
        double velZ = (Math.cos(rad) * forward + Math.sin(rad) * strafe) * hSpeed;
        double velY;
        switch (mode) {
            case "Creative":
            case "Vanilla":
                velY = jump ? vSpeed : (sneak ? -vSpeed : -glide);
                break;
            case "NCP":
                velY = jump ? vSpeed : (sneak ? -vSpeed : -glide);
                break;

            case "Jetpack":
                velY = jump ? vSpeed : -glide * 0.3;
                break;
            default:
                velY = 0;
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
