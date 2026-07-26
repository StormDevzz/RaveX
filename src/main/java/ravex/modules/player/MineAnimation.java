package ravex.modules.player;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;

import ravex.parameter.BooleanParameter;
@ModuleInfo(name = "MineAnimation", category = "net.minecraft.world.entity.player.Player")
public class MineAnimation extends ravex.modules.Module {
public final BooleanParameter hideSwing = new BooleanParameter("HideHandSwing", true);
    public final BooleanParameter hideCracks = new BooleanParameter("HideBlockCracks", true);

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!getEnabled() || !event.isSend()) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof ServerboundSwingPacket && hideSwing.getValue()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null && mc.gameMode.isDestroying()) {
                event.setCancelled(true);
            }
        }
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("MineAnimation").getEnabled();
    }
    public static MineAnimation itz() {
        return ravex.manager.ModuleManager.delegate(MineAnimation.class);
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