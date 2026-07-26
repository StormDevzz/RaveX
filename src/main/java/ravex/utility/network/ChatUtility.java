package ravex.utility.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ChatUtility {
    public static void sendMsg(Minecraft mc, String msg) {
        if (mc.player != null)
            mc.player.displayClientMessage(Component.literal(msg), false);
    }

    public static void sendMsg(Minecraft mc, String prefix, String msg) {
        sendMsg(mc, prefix + msg);
    }

    public static void sendPrefixed(Minecraft mc, String prefix, String msg) {
        sendMsg(mc, "§8[" + prefix + "§8] §7" + msg);
    }
}
