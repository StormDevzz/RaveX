package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import ravex.parameter.StringParameter;
@ModuleInfo(name = "NameProtect", category = "Misc")
public class NameProtect implements ModuleAccess {
    @Parameter(name = "ReplaceWith")
    public String replaceText = "RaveX";

    public Component protectComponent(Component component) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return component;
        String name = mc.player.getName().getString();
        if (name == null || name.isEmpty()) return component;
        String text = component.getString();
        if (!text.contains(name)) return component;
        return Component.literal(text.replace(name, replaceText)).setStyle(component.getStyle());
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("NameProtect").getEnabled();
    }

    public static NameProtect itz() {
        return ravex.manager.ModuleManager.delegate(NameProtect.class);
    }


}