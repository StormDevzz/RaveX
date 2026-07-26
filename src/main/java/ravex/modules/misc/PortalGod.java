package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.event.network.PacketEvent;
import ravex.event.Subscribe;
import ravex.modules.annotations.ModuleInfo;
import net.minecraft.network.protocol.Packet;




@ModuleInfo(name = "PortalGod", category = "Misc")
public class PortalGod implements ModuleAccess {
@Subscribe
    public void onPacket(PacketEvent event) {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("PortalGod").getEnabled() || !event.isSend()) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket) {
            event.setCancelled(true);
        }
    }

    public static PortalGod itz() {
        return ravex.manager.ModuleManager.delegate(PortalGod.class);
    }


}