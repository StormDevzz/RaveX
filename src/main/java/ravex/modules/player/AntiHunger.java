package ravex.modules.player;
import ravex.event.network.PacketEvent;
import ravex.event.Subscribe;
import ravex.mixin.network.AccessorServerboundMovePlayerPacket;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.Packet;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "AntiHunger", category = "net.minecraft.world.entity.player.Player")
public class AntiHunger {
    @Parameter(name = "Mode", modes = {"NCP", "NCPStrict", "UNCP"})
    public String mode = "NCP";

    @Parameter(name = "UNCPDelay", min = 1, max = 20, step = 1, visible = "mode=UNCP")
    public int uncpDelay = 3;

    private int uncpCounter = 0;

    private boolean canSprint() {
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        return p != null && (p.getFoodData().getFoodLevel() > 5 || p.getAbilities().flying || p.getAbilities().mayfly);
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!Modules.enabled(AntiHunger.class) || !event.isSend()) return;
        String m = mode;
        Packet<?> packet = event.getPacket();

        if ("UNCP".equals(m)) {
            if (packet instanceof net.minecraft.network.protocol.game.ServerboundMovePlayerPacket movePacket) {
                uncpCounter++;
                if (uncpCounter % uncpDelay != 0) {
                    ((AccessorServerboundMovePlayerPacket) movePacket).setOnGround(false);
                }
            }
            if (packet instanceof ServerboundPlayerCommandPacket) {
                event.setCancelled(true);
            }
            return;
        }

        if (packet instanceof net.minecraft.network.protocol.game.ServerboundMovePlayerPacket movePacket) {
            ((AccessorServerboundMovePlayerPacket) movePacket).setOnGround(false);
        }

        if (packet instanceof ServerboundPlayerCommandPacket cmd) {
            var action = cmd.getAction();
            boolean sprintAction = action == ServerboundPlayerCommandPacket.Action.START_SPRINTING
                                || action == ServerboundPlayerCommandPacket.Action.STOP_SPRINTING;
            if (!sprintAction) return;

            if ("NCP".equals(m)) {
                event.setCancelled(true);
            } else if ("NCPStrict".equals(m) && !canSprint()) {
                event.setCancelled(true);
            }
        }
    }

    public void onDisable() {
        uncpCounter = 0;
    }
}
