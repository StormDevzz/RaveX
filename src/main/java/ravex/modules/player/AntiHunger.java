package ravex.modules.player;
import ravex.manager.ModuleManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.mixin.network.AccessorServerboundMovePlayerPacket;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import java.util.List;
public class AntiHunger extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Full", List.of("Full", "OnGround", "Sprint"));

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!getEnabled() || !event.isSend()) return;
        String currentMode = mode.getValue();
        Packet<?> packet = event.getPacket();

        if (packet instanceof ServerboundMovePlayerPacket movePacket) {
            if ("Full".equals(currentMode) || "OnGround".equals(currentMode)) {
                ((AccessorServerboundMovePlayerPacket) movePacket).setOnGround(false);
            }
        }

        if (packet instanceof ServerboundPlayerCommandPacket commandPacket) {
            var action = commandPacket.getAction();
            if (action == ServerboundPlayerCommandPacket.Action.START_SPRINTING ||
                action == ServerboundPlayerCommandPacket.Action.STOP_SPRINTING) {
                if ("Full".equals(currentMode) || "Sprint".equals(currentMode)) {
                    event.setCancelled(true);
                }
            }
        }
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(AntiHunger.class);
    }
    public static AntiHunger itz() {
        return ModuleManager.get(AntiHunger.class);
    }

}