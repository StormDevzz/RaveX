package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.event.Subscribe;
import ravex.event.client.TickEvent;
import ravex.event.network.PacketEvent;
import ravex.utility.network.NetworkUtility;
import ravex.utility.movement.MoveUtility;
import ravex.mixin.network.AccessorServerboundMovePlayerPacket;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

@Module(name = "Fly", category = "Movement")
public class Fly {
    @Parameter(name = "Mode", modes = {"VerusFlat", "VerusDamage"})
    public String mode = "VerusFlat";

    @Parameter(name = "Speed", min = 1.0, max = 20.0, step = 1.0, visible = "mode=VerusFlat")
    public double speed = 5.0;

    private boolean gotDamage = false;
    private int damageTicks = 0;
    private boolean shouldStop = false;

    public void onEnable() {
        gotDamage = false;
        damageTicks = 0;
        shouldStop = false;

        String m = mode;
        if ("VerusDamage".equals(m)) {
            var mc = MinecraftWrapper.getInstance();
            var player = mc.player;
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
        var mc = MinecraftWrapper.getInstance();
        var player = mc.player;
        if (player == null) return;

        String m = mode;
        if ("VerusFlat".equals(m)) {
            MoveUtility.setMotion(0, player.getDeltaMovement().y, 0);
            NetworkUtility.sendMoveRelative(
                player.getX(), player.getY() - 0.5, player.getZ(),
                false, player.horizontalCollision
            );
        }
        if ("VerusDamage".equals(m)) {
            MoveUtility.setMotion(0, 0, 0);
        }

        gotDamage = false;
        damageTicks = 0;
        shouldStop = false;
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!event.isSend()) return;
        if (!Modules.enabled(Fly.class)) return;

        String m = mode;
        if (!"VerusFlat".equals(m)) return;

        var packet = event.getPacket();
        if (packet instanceof ServerboundMovePlayerPacket move) {
            AccessorServerboundMovePlayerPacket accessor = (AccessorServerboundMovePlayerPacket) move;
            accessor.setOnGround(true);
        }
    }

    @Subscribe
    public void onTick(TickEvent.Client event) {
        if (!Modules.enabled(Fly.class)) return;

        String m = mode;
        var mc = MinecraftWrapper.getInstance();
        var player = mc.player;
        if (player == null) return;

        if ("VerusFlat".equals(m)) {
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

        if ("VerusDamage".equals(m)) {
            if (player.hurtTime > 0) {
                gotDamage = true;
            }

            if (!gotDamage) {
                return;
            }

            damageTicks++;
            if (damageTicks > 20 || shouldStop) {
                Modules.setEnabled(Fly.class, false);
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
}
