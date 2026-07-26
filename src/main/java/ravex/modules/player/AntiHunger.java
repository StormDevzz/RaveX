package ravex.modules.player;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Minecraft;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.mixin.network.AccessorServerboundMovePlayerPacket;

import ravex.parameter.ModeParameter;
import java.util.List;

@ModuleInfo(name = "AntiHunger", category = "net.minecraft.world.entity.player.Player")
public class AntiHunger extends ravex.modules.Module {
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
        return ravex.manager.ModuleManager.INSTANCE.getByName("AntiHunger").getEnabled();
    }

    public static AntiHunger itz() {
        return ravex.manager.ModuleManager.delegate(AntiHunger.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}