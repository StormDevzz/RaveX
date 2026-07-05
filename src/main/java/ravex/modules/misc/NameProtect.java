package ravex.modules.misc;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.StringParameter;
public class NameProtect extends Module {
    public static final NameProtect INSTANCE = new NameProtect();
    public final StringParameter replaceText = new StringParameter("Replace With", "RaveX");

    public Component protectComponent(Component component) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return component;
        String name = mc.player.getName().getString();
        if (name == null || name.isEmpty()) return component;
        String text = component.getString();
        if (!text.contains(name)) return component;
        return Component.literal(text.replace(name, replaceText.getValue())).setStyle(component.getStyle());
    }
}
