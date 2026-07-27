package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.network.chat.Component;

import ravex.parameter.StringParameter;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "NameProtect", category = "Misc")
public class NameProtect {
    @Parameter(name = "ReplaceWith")
    public String replaceText = "RaveX";

    public Component protectComponent(Component component) {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null) return component;
        String name = mc.player.getName().getString();
        if (name == null || name.isEmpty()) return component;
        String text = component.getString();
        if (!text.contains(name)) return component;
        return Component.literal(text.replace(name, replaceText)).setStyle(component.getStyle());
    }






}