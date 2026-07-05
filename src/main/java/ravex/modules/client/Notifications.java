package ravex.modules.client;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.manager.NotificationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.List;
public class Notifications extends Module {
    public static final Notifications INSTANCE = new Notifications();
    public final ModeParameter mode = new ModeParameter("Mode", "Text", List.of("Text", "Toast"));
    public final ColorParameter messageColor = new ColorParameter("MessageColor", 0xFF0066FF);
    public final NumberParameter toastOpacity = new NumberParameter("ToastOpacity", 0.85, 0.25, 1.0, 0.05);
    public final NumberParameter toastSize = new NumberParameter("ToastSize", 16.0, 12.0, 32.0, 1.0);
    private Notifications() {
        super("Notifications");
        setEnabled(true);
        toastOpacity.setVisible(() -> mode.getValue().equals("Toast"));
        toastSize.setVisible(() -> mode.getValue().equals("Toast"));
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
        int color = INSTANCE.messageColor.getValue();
        String action = enabled ? "Enabled" : "Disabled";
        if (INSTANCE.mode.getValue().equals("Toast")) {
            NotificationManager.addToast(module.getName() + " " + action, color, enabled, INSTANCE.toastOpacity.getValue().floatValue(), INSTANCE.toastSize.getValue().intValue());
            return;
        }
        if (mc.player != null) {
            Component message = Component.literal("[")
                .withStyle(style -> style.withColor(0x7F7F7F))
                .append(Component.literal("RaveX").withStyle(style -> style.withColor(color)))
                .append(Component.literal("] Module ").withStyle(style -> style.withColor(0x7F7F7F)))
                .append(Component.literal(module.getName()).withStyle(style -> style.withColor(color)))
                .append(Component.literal(" has been ").withStyle(style -> style.withColor(0x7F7F7F)))
                .append(Component.literal(action).withStyle(style -> style.withColor(color)))
                .append(Component.literal(".").withStyle(style -> style.withColor(0x7F7F7F)));
            mc.player.displayClientMessage(message, false);
        }
    }
}
