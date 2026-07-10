package ravex.modules.misc;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class PacketLogger extends Module {
    public static final PacketLogger INSTANCE = new PacketLogger();

    public final BooleanParameter outgoing = new BooleanParameter("Outgoing", true);
    public final BooleanParameter incoming = new BooleanParameter("Incoming", false);
    public final BooleanParameter chat = new BooleanParameter("To Chat", true);

    private PacketLogger() {
        super("PacketLogger", Category.MISC);
        addParameter(outgoing);
        addParameter(incoming);
        addParameter(chat);
    }

    public void logPacket(String direction, Object packet) {
        if (!getEnabled()) return;
        String name = packet.getClass().getSimpleName();
        String message = "§7[§6Packet§7] §d" + direction + " §e" + name;

        Minecraft mc = Minecraft.getInstance();
        if (chat.getValue() && mc.player != null) {
            mc.player.displayClientMessage(Component.literal(message), false);
        } else {
            System.out.println("[PacketLogger] " + direction + " " + packet.getClass().getName());
        }
    }
}
