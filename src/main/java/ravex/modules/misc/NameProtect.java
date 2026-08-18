package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.network.chat.Component;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "NameProtect", category = "Misc")
public class NameProtect {
    @Parameter(name = "ReplaceWith")
    public String replaceText = "RaveX";

    public Component protectComponent(Component component) {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return component;
        String name = player.getName().getString();
        if (name == null || name.isEmpty()) return component;
        String text = component.getString();
        if (!text.contains(name)) return component;
        return Component.literal(text.replace(name, replaceText)).setStyle(component.getStyle());
    }
}
