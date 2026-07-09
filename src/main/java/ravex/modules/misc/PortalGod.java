package ravex.modules.misc;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
public class PortalGod extends Module {

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!getEnabled() || !event.isSend()) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof ServerboundAcceptTeleportationPacket) {
            event.setCancelled(true);
        }
    }

    public static PortalGod itz() {
        return ModuleManager.get(PortalGod.class);
    }
}
