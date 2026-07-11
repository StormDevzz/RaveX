package ravex.modules.movement;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import ravex.event.Subscribe;
import ravex.event.client.TickEvent;
import ravex.event.network.PacketEvent;
import ravex.mixin.network.AccessorServerboundMovePlayerPacket;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import java.util.List;
public class NoFall extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Vanilla", List.of("Vanilla", "NCP", "Grim"));

    private boolean wasOnGround = true;

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!getEnabled() || !event.isSend()) return;
        Packet<?> packet = event.getPacket();
        if (!(packet instanceof ServerboundMovePlayerPacket movePacket)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if ("Grim".equals(mode.getValue())) return;

        if (mc.player.fallDistance <= 2.0) return;

        AccessorServerboundMovePlayerPacket accessor = (AccessorServerboundMovePlayerPacket) movePacket;
        accessor.setOnGround(true);
    }

    @Subscribe
    public void onTick(TickEvent.Client event) {
        if (!getEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String modeVal = mode.getValue();
        if ("Grim".equals(modeVal)) {
            if (wasOnGround && !mc.player.onGround() && mc.player.fallDistance > 0.5) {
                mc.player.setDeltaMovement(
                    mc.player.getDeltaMovement().x,
                    0.42,
                    mc.player.getDeltaMovement().z
                );
                mc.player.fallDistance = 0;
            }
            wasOnGround = mc.player.onGround();
        }
    }

    public static NoFall itz() {
        return ModuleManager.get(NoFall.class);
    }
}
