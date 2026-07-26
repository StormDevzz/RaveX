package ravex.modules.misc;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import ravex.parameter.StringParameter;
@ModuleInfo(name = "NameProtect", category = "Misc")
public class NameProtect extends ravex.modules.Module {
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
        return ravex.manager.ModuleManager.INSTANCE.getByName("NameProtect").getEnabled();
    }

    public static NameProtect itz() {
        return ravex.manager.ModuleManager.delegate(NameProtect.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}