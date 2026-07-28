package ravex.modules.movement;
import ravex.event.client.TickEvent;
import ravex.event.network.PacketEvent;
import ravex.event.Subscribe;
import ravex.mixin.network.AccessorServerboundMovePlayerPacket;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.network.NetworkUtility;
import ravex.utility.movement.MoveUtility;
import net.minecraft.network.protocol.Packet;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;





@Module(name = "NoFall", category = "Movement")
public class NoFall {
    @Parameter(name = "Mode", modes = {"Vanilla", "NCP", "Grim", "UNCP"})
    public String mode = "Vanilla";

    private boolean wasOnGround = true;
    private int uncpCounter = 0;

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!Modules.enabled(NoFall.class) || !event.isSend()) return;
        Packet<?> packet = event.getPacket();
        if (!(packet instanceof net.minecraft.network.protocol.game.ServerboundMovePlayerPacket movePacket)) return;
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null) return;

        if ("Grim".equals(mode) || "UNCP".equals(mode)) return;

        if (mc.player.fallDistance <= 2.0) return;

        AccessorServerboundMovePlayerPacket accessor = (AccessorServerboundMovePlayerPacket) movePacket;
        accessor.setOnGround(true);
    }

    @Subscribe
    public void onTick(TickEvent.Client event) {
        if (!Modules.enabled(NoFall.class)) return;
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null) return;

        String modeVal = mode;
        if ("Grim".equals(modeVal)) {
            if (wasOnGround && !mc.player.onGround() && mc.player.fallDistance > 0.5) {
                MoveUtility.setMotion(mc.player.getDeltaMovement().x, 0.42, mc.player.getDeltaMovement().z);
                mc.player.fallDistance = 0;
            }
            wasOnGround = mc.player.onGround();
        } else if ("UNCP".equals(modeVal) && mc.player.fallDistance > 0.5) {
            mc.player.fallDistance = 0;
            MoveUtility.setMotion(
                mc.player.getDeltaMovement().x * 0.98,
                Math.min(mc.player.getDeltaMovement().y, 0.0) * 0.5,
                mc.player.getDeltaMovement().z * 0.98
            );
            uncpCounter++;
            if (uncpCounter % 3 == 0) {
                NetworkUtility.sendMoveRelative(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    true, mc.player.horizontalCollision
                );
            }
        }
    }

    public void onDisable() {
        uncpCounter = 0;
    }



}
