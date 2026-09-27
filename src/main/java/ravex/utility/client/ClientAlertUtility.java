package ravex.utility.client;

import net.minecraft.network.chat.Component;
import ravex.manager.ModuleManager;
import ravex.manager.NotificationManager;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.client.Notifications;

public final class ClientAlertUtility {
    public static final String PREFIX = "§9[§bRaveX§9] ";

    private ClientAlertUtility() {}

    public static void alert(String message) {
        alert(message, 0xFF3880FF);
    }

    public static void alert(String message, int toastColor) {
        Notifications notif = null;
        try {
            notif = ModuleManager.get(Notifications.class);
        } catch (Exception ignored) {}

        String mode = notif != null ? notif.clientAlerts : "Text";

        if ("Toast".equalsIgnoreCase(mode)) {
            String clean = stripFormatting(message);
            if (clean.startsWith("[RaveX] ")) {
                clean = clean.substring(8);
            }
            float opacity = notif != null ? (float) notif.toastOpacity : 0.25f;
            int size = notif != null ? (int) notif.toastSize : 16;
            NotificationManager.addToast(clean, toastColor, NotificationManager.ToastType.INFO, opacity, size);
        } else {
            var mc = MinecraftWrapper.getWrapper();
            if (mc.getPlayer() != null) {
                String full = message.startsWith("§9[§bRaveX§9]") ? message : (PREFIX + message);
                mc.getPlayer().displayClientMessage(Component.literal(full), false);
            }
        }
    }

    public static String stripFormatting(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }
}
