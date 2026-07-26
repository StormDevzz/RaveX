package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.network.chat.Component;

import ravex.parameter.StringParameter;
@ModuleInfo(name = "DeathText", category = "Render")
public class DeathText extends ravex.modules.Module {
public final StringParameter deathText = new StringParameter("Text", "JustFuckedUp");
    public static String lastCustomText = "";
    private DeathText() {
        
        lastCustomText = deathText.getValue();
    }
    public static Component getDeathComponent() {
        if (!ravex.manager.ModuleManager.delegate(DeathText.class).getEnabled()) return null;
        String text = ravex.manager.ModuleManager.delegate(DeathText.class).deathText.getValue();
        if (text == null || text.isEmpty()) return null;
        return Component.literal(text);
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("DeathText").getEnabled();
    }

    public static DeathText itz() {
        return ravex.manager.ModuleManager.delegate(DeathText.class);
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