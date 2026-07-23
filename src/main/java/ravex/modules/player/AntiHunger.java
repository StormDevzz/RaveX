package ravex.modules.player;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Minecraft;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.mixin.network.AccessorServerboundMovePlayerPacket;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import java.util.List;

public class AntiHunger extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "NCP", List.of("NCP", "NCPStrict"));

    private boolean canSprint() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && (p.getFoodData().getFoodLevel() > 5 || p.getAbilities().flying || p.getAbilities().mayfly);
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!getEnabled() || !event.isSend()) return;
        String m = mode.getValue();
        Packet<?> packet = event.getPacket();

        if (packet instanceof ServerboundMovePlayerPacket movePacket) {
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

    public static boolean maybeEnabled() {
        return maybeEnabled(AntiHunger.class);
    }

    public static AntiHunger itz() {
        return self(AntiHunger.class);
    }
}