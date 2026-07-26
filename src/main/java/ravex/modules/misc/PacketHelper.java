package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;

import net.minecraft.network.protocol.Packet;
import ravex.utility.network.NetworkUtility;
import java.util.List;
@ModuleInfo(name = "PacketHelper", category = "Misc")
public class PacketHelper implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"Logging", "Filter", "Cancel"})
    public String mode = "Logging";

    @Parameter(name = "Logging")
    public boolean loggingEnabled = false;
    @Parameter(name = "Filter")
    public boolean filterEnabled = false;
    @Parameter(name = "Cancel")
    public boolean cancelEnabled = false;

    @Parameter(name = "LogOutgoing")
    public boolean logOutgoing = true;
    @Parameter(name = "LogIncoming")
    public boolean logIncoming = false;
    @Parameter(name = "LogToChat")
    public boolean logToChat = true;

    @Parameter(name = "RateLimit", min = 10, max = 500, step = 5)
    public double rateLimit = 80;
    @Parameter(name = "Burst", min = 5, max = 50, step = 1)
    public double burst = 15;

    @Parameter(name = "FilterMove")
    public boolean filterMove = true;
    @Parameter(name = "FilterInteract")
    public boolean filterInteract = false;
    @Parameter(name = "FilterChat")
    public boolean filterChat = false;

    @Parameter(name = "CancelMove")
    public boolean cancelMove = false;
    @Parameter(name = "CancelInput")
    public boolean cancelInput = false;
    @Parameter(name = "CancelInteract")
    public boolean cancelInteract = false;
    @Parameter(name = "CancelSwing")
    public boolean cancelSwing = false;
    @Parameter(name = "CancelUse")
    public boolean cancelUse = false;

    private PacketHelper() {
        
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("PacketHelper").getEnabled()) return;
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
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("PacketHelper").getEnabled() || !loggingEnabled) return;
        String name = NetworkUtility.packetName(packet);
        String message = "§7[§6Packet§7] §d" + direction + " §e" + name;
        if (logToChat) {
            NetworkUtility.displayClientMessage(message);
        } else {
            System.out.println("[PacketHelper] " + direction + " " + packet.getClass().getName());
        }
    }

    public boolean shouldCancel(Packet<?> packet) {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("PacketHelper").getEnabled() || !cancelEnabled) return false;
        if (NetworkUtility.isMovePacket(packet) && cancelMove) return true;
        if (NetworkUtility.isInputPacket(packet) && cancelInput) return true;
        if (NetworkUtility.isSwingPacket(packet) && cancelSwing) return true;
        if (NetworkUtility.isInteractPacket(packet) && cancelInteract) return true;
        if (NetworkUtility.isUsePacket(packet) && cancelUse) return true;
        return false;
    }

    public static PacketHelper itz() {
        return ravex.manager.ModuleManager.delegate(PacketHelper.class);
    }


}