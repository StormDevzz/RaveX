package ravex.modules.misc;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.ModeParameter;
import net.minecraft.network.protocol.Packet;
import ravex.utility.network.NetworkUtility;
import java.util.List;
public class PacketHelper extends Module {

    public final ModeParameter mode = new ModeParameter("Mode", "Logging", List.of("Logging", "Filter", "Cancel"));

    public final BooleanParameter loggingEnabled = new BooleanParameter("Logging", false);
    public final BooleanParameter filterEnabled = new BooleanParameter("Filter", false);
    public final BooleanParameter cancelEnabled = new BooleanParameter("Cancel", false);

    public final BooleanParameter logOutgoing = new BooleanParameter("LogOutgoing", true);
    public final BooleanParameter logIncoming = new BooleanParameter("LogIncoming", false);
    public final BooleanParameter logToChat = new BooleanParameter("LogToChat", true);

    public final NumberParameter rateLimit = new NumberParameter("RateLimit", 80, 10, 500, 5);
    public final NumberParameter burst = new NumberParameter("Burst", 15, 5, 50, 1);

    public final BooleanParameter filterMove = new BooleanParameter("FilterMove", true);
    public final BooleanParameter filterInteract = new BooleanParameter("FilterInteract", false);
    public final BooleanParameter filterChat = new BooleanParameter("FilterChat", false);

    public final BooleanParameter cancelMove = new BooleanParameter("CancelMove", false);
    public final BooleanParameter cancelInput = new BooleanParameter("CancelInput", false);
    public final BooleanParameter cancelInteract = new BooleanParameter("CancelInteract", false);
    public final BooleanParameter cancelSwing = new BooleanParameter("CancelSwing", false);
    public final BooleanParameter cancelUse = new BooleanParameter("CancelUse", false);

    private PacketHelper() {
        super("PacketHelper");
        loggingEnabled.setVisible(() -> "Logging".equals(mode.getValue()));
        logOutgoing.setVisible(() -> "Logging".equals(mode.getValue()));
        logIncoming.setVisible(() -> "Logging".equals(mode.getValue()));
        logToChat.setVisible(() -> "Logging".equals(mode.getValue()));
        rateLimit.setVisible(() -> "Logging".equals(mode.getValue()));
        burst.setVisible(() -> "Logging".equals(mode.getValue()));
        filterEnabled.setVisible(() -> "Filter".equals(mode.getValue()));
        filterMove.setVisible(() -> "Filter".equals(mode.getValue()));
        filterInteract.setVisible(() -> "Filter".equals(mode.getValue()));
        filterChat.setVisible(() -> "Filter".equals(mode.getValue()));
        cancelEnabled.setVisible(() -> "Cancel".equals(mode.getValue()));
        cancelMove.setVisible(() -> "Cancel".equals(mode.getValue()));
        cancelInput.setVisible(() -> "Cancel".equals(mode.getValue()));
        cancelInteract.setVisible(() -> "Cancel".equals(mode.getValue()));
        cancelSwing.setVisible(() -> "Cancel".equals(mode.getValue()));
        cancelUse.setVisible(() -> "Cancel".equals(mode.getValue()));
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!getEnabled()) return;
        Packet<?> packet = event.getPacket();
        if (event.isSend() && loggingEnabled.getValue() && logOutgoing.getValue()) {
            logPacket("C2S ->", packet);
        }
        if (event.isReceive() && loggingEnabled.getValue() && logIncoming.getValue()) {
            logPacket("S2C <-", packet);
        }
        if (event.isSend() && shouldCancel(packet)) {
            event.setCancelled(true);
        }
    }

    public void logPacket(String direction, Packet<?> packet) {
        if (!getEnabled() || !loggingEnabled.getValue()) return;
        String name = NetworkUtility.packetName(packet);
        String message = "§7[§6Packet§7] §d" + direction + " §e" + name;
        if (logToChat.getValue()) {
            NetworkUtility.displayClientMessage(message);
        } else {
            System.out.println("[PacketHelper] " + direction + " " + packet.getClass().getName());
        }
    }

    public boolean shouldCancel(Packet<?> packet) {
        if (!getEnabled() || !cancelEnabled.getValue()) return false;
        if (NetworkUtility.isMovePacket(packet) && cancelMove.getValue()) return true;
        if (NetworkUtility.isInputPacket(packet) && cancelInput.getValue()) return true;
        if (NetworkUtility.isSwingPacket(packet) && cancelSwing.getValue()) return true;
        if (NetworkUtility.isInteractPacket(packet) && cancelInteract.getValue()) return true;
        if (NetworkUtility.isUsePacket(packet) && cancelUse.getValue()) return true;
        return false;
    }

    public static PacketHelper itz() {
        return ModuleManager.get(PacketHelper.class);
    }
}
