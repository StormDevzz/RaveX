package ravex.modules.misc;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;

@ModuleInfo(name = "PortalGod", category = "Misc")
public class PortalGod extends ravex.modules.Module {
@Subscribe
    public void onPacket(PacketEvent event) {
        if (!getEnabled() || !event.isSend()) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof ServerboundAcceptTeleportationPacket) {
            event.setCancelled(true);
        }
    }

    public static PortalGod itz() {
        return ravex.manager.ModuleManager.delegate(PortalGod.class);
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