package ravex.modules.movement;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.network.NetworkUtility;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Module(name = "PacketFly", category = "Movement")
public class PacketFly {
    @Parameter(name = "Mode", modes = {"NCP", "Custom"})
    public String mode = "NCP";
    @Parameter(name = "Factor", min = 0.1, max = 5.0, step = 0.1)
    public double factor = 1.3;
    @Parameter(name = "Speed", min = 0.01, max = 1.0, step = 0.01, visible = "mode=Custom")
    public double speed = 0.2873;
    @Parameter(name = "Vertical", min = 0.01, max = 0.2, step = 0.001, visible = "mode=Custom")
    public double vertical = 0.0624;
    @Parameter(name = "AntiKick", visible = "mode=Custom")
    public boolean antiKick = true;

    private int tpId = -1;
    private final Set<Packet<?>> allowedMoves = new HashSet<>();
    private final Map<Integer, double[]> allowedTeleports = new HashMap<>();

    public void onEnable() {
        tpId = -1;
        allowedMoves.clear();
        allowedTeleports.clear();
    }

    public void onDisable() {
        tpId = -1;
        allowedMoves.clear();
        allowedTeleports.clear();
        var player = MinecraftWrapper.getWrapper().getPlayer();
        if (player != null) {
            player.noPhysics = false;
            player.setNoGravity(false);
            player.setDeltaMovement(0, 0, 0);
        }
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getConnection() == null) return;

        allowedMoves.clear();
        player.noPhysics = true;
        player.setNoGravity(true);
        player.setDeltaMovement(0, 0, 0);

        boolean ncp = "NCP".equals(mode);
        boolean moving = mc.isForwardKeyDown() || mc.isBackKeyDown() || mc.isLeftKeyDown() || mc.isRightKeyDown();
        boolean walls = player.isInWall();

        double motionY = 0.0;
        if ((ncp || antiKick) && player.tickCount % 10 == 0 && !walls) {
            motionY = -0.04;
        } else if (mc.isJumpKeyDown()) {
            motionY = ncp ? 0.0624 : vertical;
        } else if (mc.isSneakKeyDown()) {
            motionY = ncp ? -0.0624 : -vertical;
        }

        double motionH;
        if (walls) {
            motionH = 0.0624;
            if (motionY != 0) {
                double m = 1.0 / Math.sqrt(2.0);
                motionY *= m;
                motionH *= m;
            }
        } else {
            motionH = ncp ? 0.2873 : speed;
            if (ncp && moving && motionY > 0) motionY = 0;
        }

        double inputX = 0.0;
        double inputZ = 0.0;
        if (moving) {
            float forward = 0;
            float strafe = 0;
            if (mc.isForwardKeyDown()) forward++;
            if (mc.isBackKeyDown()) forward--;
            if (mc.isLeftKeyDown()) strafe++;
            if (mc.isRightKeyDown()) strafe--;
            double yaw = Math.toRadians(player.getYRot());
            inputX = -Math.sin(yaw) * forward + Math.cos(yaw) * strafe;
            inputZ = Math.cos(yaw) * forward + Math.sin(yaw) * strafe;
            double len = Math.sqrt(inputX * inputX + inputZ * inputZ);
            if (len > 1.0E-4) {
                inputX /= len;
                inputZ /= len;
            } else {
                inputX = 0;
                inputZ = 0;
            }
        }

        double motionX = inputX * motionH;
        double motionZ = inputZ * motionH;
        if (motionX == 0 && motionY == 0 && motionZ == 0) return;

        int factorInt = (int) Math.floor(factor);
        if (player.tickCount % 10 < 10 * (factor - Math.floor(factor))) factorInt++;
        if (factorInt < 1) return;

        double baseX = player.getX();
        double baseY = player.getY();
        double baseZ = player.getZ();
        boolean onGround = player.onGround();
        boolean hCollision = player.horizontalCollision;

        for (int i = 1; i <= factorInt; i++) {
            double px = baseX + motionX * i;
            double py = baseY + motionY * i;
            double pz = baseZ + motionZ * i;

            ServerboundMovePlayerPacket.Pos packet = new ServerboundMovePlayerPacket.Pos(px, py, pz, onGround, hCollision);
            ServerboundMovePlayerPacket.Pos bounds = new ServerboundMovePlayerPacket.Pos(px, py + 512, pz, true, hCollision);
            allowedMoves.add(packet);
            allowedMoves.add(bounds);
            NetworkUtility.sendPacket(packet);
            NetworkUtility.sendPacket(bounds);

            if (tpId < 0) break;

            tpId++;
            NetworkUtility.sendTeleportConfirm(tpId);
            allowedTeleports.put(tpId, new double[]{px, py, pz});
        }

        player.setPos(baseX + motionX * factorInt, baseY + motionY * factorInt, baseZ + motionZ * factorInt);
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!Modules.enabled(PacketFly.class)) return;

        if (event.isSend()) {
            Packet<?> packet = event.getPacket();
            if (packet instanceof ServerboundMovePlayerPacket move && move.hasPosition()) {
                if (!allowedMoves.contains(packet)) {
                    event.setCancelled(true);
                }
            }
            return;
        }

        Packet<?> packet = event.getPacket();
        if (packet instanceof ClientboundPlayerPositionPacket posPacket) {
            int id = posPacket.id();
            var position = posPacket.change().position();
            double[] saved = allowedTeleports.get(id);
            if (saved != null
                && Math.abs(position.x - saved[0]) < 1.0E-4
                && Math.abs(position.y - saved[1]) < 1.0E-4
                && Math.abs(position.z - saved[2]) < 1.0E-4) {
                allowedTeleports.remove(id);
                NetworkUtility.sendTeleportConfirm(id);
                event.setCancelled(true);
                return;
            }
            tpId = id;
            NetworkUtility.sendTeleportConfirm(id);
        }
    }
}
