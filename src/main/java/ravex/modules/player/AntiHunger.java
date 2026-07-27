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
    @Parameter(name = "Mode", modes = {"NCP", "NCPStrict"})
    public String mode = "NCP";

    private boolean canSprint() {
        net.minecraft.client.player.LocalPlayer p = MinecraftWrapper.getInstance().player;
        return p != null && (p.getFoodData().getFoodLevel() > 5 || p.getAbilities().flying || p.getAbilities().mayfly);
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!Modules.enabled(AntiHunger.class) || !event.isSend()) return;
        String m = mode;
        Packet<?> packet = event.getPacket();

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






}