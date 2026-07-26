package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.network.chat.Component;

import ravex.parameter.StringParameter;
@ModuleInfo(name = "DeathText", category = "Render")
public class DeathText implements ModuleAccess {
    @Parameter(name = "Text")
    public String deathText = "JustFuckedUp";
    public static String lastCustomText = "";
    private DeathText() {
        
        lastCustomText = deathText;
    }
    public static Component getDeathComponent() {
        if (!ravex.manager.ModuleManager.delegate(DeathText.class).getEnabled()) return null;
        String text = ravex.manager.ModuleManager.delegate(DeathText.class).deathText;
        if (text == null || text.isEmpty()) return null;
        return Component.literal(text);
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("DeathText").getEnabled();
    }

    public static DeathText itz() {
        return ravex.manager.ModuleManager.delegate(DeathText.class);
    }


}