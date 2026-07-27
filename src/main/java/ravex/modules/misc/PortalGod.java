package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.event.network.PacketEvent;
import ravex.event.Subscribe;
import ravex.modules.annotations.Module;
import net.minecraft.network.protocol.Packet;
import ravex.modules.Modules;




@Module(name = "PortalGod", category = "Misc")
public class PortalGod {
@Subscribe
    public void onPacket(PacketEvent event) {
        if (!Modules.enabled(PortalGod.class) || !event.isSend()) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket) {
            event.setCancelled(true);
        }
    }




}