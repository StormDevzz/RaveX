package ravex.utility.network;

import net.minecraft.client.Minecraft;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.network.chat.Component;

public class ChatUtility {
    public static void sendMsg(MinecraftWrapper mc, String msg) {
        var _mc = mc.getRaw();
        if (_mc.player != null)
            _mc.player.displayClientMessage(Component.literal(msg), false);
    }

    public static void sendMsg(MinecraftWrapper mc, String prefix, String msg) {
        var _mc = mc.getRaw();
        sendMsg(mc, prefix + msg);
    }

    public static void sendPrefixed(MinecraftWrapper mc, String prefix, String msg) {
        var _mc = mc.getRaw();
        sendMsg(mc, "§8[" + prefix + "§8] §7" + msg);
    }
}
