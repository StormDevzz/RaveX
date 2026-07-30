package ravex.modules.movement;
import ravex.event.client.TickEvent;
import ravex.event.network.PacketEvent;
import ravex.event.Subscribe;
import ravex.mixin.network.AccessorServerboundMovePlayerPacket;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.network.NetworkUtility;
import ravex.utility.movement.MoveUtility;
import ravex.utility.player.PlayerUtility;
import net.minecraft.network.protocol.Packet;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "NoFall", category = "Movement")
public class NoFall {
    @Parameter(name = "Mode", modes = {"Vanilla", "NCP", "Grim", "UNCP", "Verus"})
    public String mode = "Vanilla";

    private boolean wasOnGround = true;
    private int uncpCounter = 0;

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!Modules.enabled(NoFall.class) || !event.isSend()) return;
        Packet<?> packet = event.getPacket();
        if (!(packet instanceof net.minecraft.network.protocol.game.ServerboundMovePlayerPacket movePacket)) return;
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;

        if ("Grim".equals(mode) || "UNCP".equals(mode)) return;

        if (PlayerUtility.getFallDistance(player) <= 2.0) return;

        AccessorServerboundMovePlayerPacket accessor = (AccessorServerboundMovePlayerPacket) movePacket;
        accessor.setOnGround(true);
    }

    @Subscribe
    public void onTick(TickEvent.Client event) {
        if (!Modules.enabled(NoFall.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;

        String modeVal = mode;
        if ("Grim".equals(modeVal)) {
            if (wasOnGround && !PlayerUtility.isOnGround(player) && PlayerUtility.getFallDistance(player) > 0.5) {
                MoveUtility.setMotion(PlayerUtility.getDeltaMovement(player).x, 0.42, PlayerUtility.getDeltaMovement(player).z);
                PlayerUtility.setFallDistance(player, 0);
            }
            wasOnGround = PlayerUtility.isOnGround(player);
        } else if ("UNCP".equals(modeVal) && PlayerUtility.getFallDistance(player) > 0.5) {
            PlayerUtility.setFallDistance(player, 0);
            MoveUtility.setMotion(
                PlayerUtility.getDeltaMovement(player).x * 0.98,
                Math.min(PlayerUtility.getDeltaMovement(player).y, 0.0) * 0.5,
                PlayerUtility.getDeltaMovement(player).z * 0.98
            );
            uncpCounter++;
            if (uncpCounter % 3 == 0) {
                NetworkUtility.sendMoveRelative(
                    player.getX(), player.getY(), player.getZ(),
                    true, player.horizontalCollision
                );
            }
        }
    }

    public void onDisable() {
        uncpCounter = 0;
    }
}
