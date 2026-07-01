package ravex.modules.client;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ColorParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class Notifications extends Module {
    public static final Notifications INSTANCE = new Notifications();

    public final ColorParameter messageColor = new ColorParameter("Message Color", 0xFF0066FF);

    private Notifications() {
        super("Notifications", Category.CLIENT);
        addParameter(messageColor);
        setEnabled(true);
    }

    private static String argbToMcHex(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return "§x" +
            "§" + Character.forDigit((r >> 4) & 0xF, 16) +
            "§" + Character.forDigit(r & 0xF, 16) +
            "§" + Character.forDigit((g >> 4) & 0xF, 16) +
            "§" + Character.forDigit(g & 0xF, 16) +
            "§" + Character.forDigit((b >> 4) & 0xF, 16) +
            "§" + Character.forDigit(b & 0xF, 16);
    }

    public static void notifyToggle(Module module, boolean enabled) {
        if (!INSTANCE.getEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            String c = argbToMcHex(INSTANCE.messageColor.getValue());
            String status = c + (enabled ? "Enabled" : "Disabled");
            String msg = "§7[" + c + "RaveX§7] Module " + c + module.getName() + " §7has been " + status + "§7.";
            mc.player.displayClientMessage(Component.literal(msg), false);
        }
    }
}
