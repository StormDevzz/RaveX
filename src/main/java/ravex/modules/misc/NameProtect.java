package ravex.modules.misc;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import ravex.modules.Module;
import ravex.manager.ModuleManager;
import ravex.parameter.StringParameter;
public class NameProtect extends Module {
    public final StringParameter replaceText = new StringParameter("ReplaceWith", "RaveX");

    public Component protectComponent(Component component) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return component;
        String name = mc.player.getName().getString();
        if (name == null || name.isEmpty()) return component;
        String text = component.getString();
        if (!text.contains(name)) return component;
        return Component.literal(text.replace(name, replaceText.getValue())).setStyle(component.getStyle());
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(NameProtect.class);
    }

    public static NameProtect itz() {
        return ModuleManager.get(NameProtect.class);
    }
}
