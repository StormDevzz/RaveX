package ravex.modules.client;

import ravex.modules.Category;
import ravex.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class Notifications extends Module {
    public static final Notifications INSTANCE = new Notifications();

    private Notifications() {
        super("Notifications", Category.CLIENT);
        setEnabled(true); // Enabled by default
    }

    public static void notifyToggle(Module module, boolean enabled) {
        if (!INSTANCE.getEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            String color = enabled ? "§aEnabled" : "§cDisabled";
            String msg = "§7[§cRaveX§7] Module §f" + module.getName() + " §7has been " + color + "§7.";
            mc.player.displayClientMessage(Component.literal(msg), false);
        }
    }
}
