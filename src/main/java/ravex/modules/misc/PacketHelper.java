package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;

import net.minecraft.network.protocol.Packet;
import ravex.utility.network.NetworkUtility;
import ravex.modules.Modules;
@Module(name = "PacketHelper", category = "Misc")
public class PacketHelper {
    @Parameter(name = "Logging", group = true)
    public boolean groupLogging = true;
    @Parameter(name = "LogEnabled", visible = "groupLogging")
    public boolean loggingEnabled = false;
    @Parameter(name = "Outgoing", visible = "groupLogging && logEnabled")
    public boolean logOutgoing = true;
    @Parameter(name = "Incoming", visible = "groupLogging && logEnabled")
    public boolean logIncoming = false;
    @Parameter(name = "ToChat", visible = "groupLogging && logEnabled")
    public boolean logToChat = true;

    @Parameter(name = "Cancel", group = true)
    public boolean groupCancel = true;
    @Parameter(name = "CancelEnabled", visible = "groupCancel")
    public boolean cancelEnabled = false;
    @Parameter(name = "CancelMove", visible = "groupCancel && cancelEnabled")
    public boolean cancelMove = false;
    @Parameter(name = "CancelInput", visible = "groupCancel && cancelEnabled")
    public boolean cancelInput = false;
    @Parameter(name = "CancelInteract", visible = "groupCancel && cancelEnabled")
    public boolean cancelInteract = false;
    @Parameter(name = "CancelSwing", visible = "groupCancel && cancelEnabled")
    public boolean cancelSwing = false;
    @Parameter(name = "CancelUse", visible = "groupCancel && cancelEnabled")
    public boolean cancelUse = false;

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!Modules.enabled(PacketHelper.class)) return;
        Packet<?> packet = event.getPacket();
        if (event.isSend() && loggingEnabled && logOutgoing) {
            logPacket("C2S ->", packet);
        }
        if (event.isReceive() && loggingEnabled && logIncoming) {
            logPacket("S2C <-", packet);
        }
        if (event.isSend() && shouldCancel(packet)) {
            event.setCancelled(true);
        }
    }

    public void logPacket(String direction, Packet<?> packet) {
        if (!Modules.enabled(PacketHelper.class) || !loggingEnabled) return;
        String name = NetworkUtility.packetName(packet);
        String message = "§7[§6Packet§7] §d" + direction + " §e" + name;
        if (logToChat) {
            NetworkUtility.displayClientMessage(message);
        } else {
            System.out.println("[PacketHelper] " + direction + " " + packet.getClass().getName());
        }
    }

    public boolean shouldCancel(Packet<?> packet) {
        if (!Modules.enabled(PacketHelper.class) || !cancelEnabled) return false;
        if (NetworkUtility.isMovePacket(packet) && cancelMove) return true;
        if (NetworkUtility.isInputPacket(packet) && cancelInput) return true;
        if (NetworkUtility.isSwingPacket(packet) && cancelSwing) return true;
        if (NetworkUtility.isInteractPacket(packet) && cancelInteract) return true;
        if (NetworkUtility.isUsePacket(packet) && cancelUse) return true;
        return false;
    }
}
