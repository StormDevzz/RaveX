package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.network.chat.Component;

import ravex.parameter.StringParameter;
import ravex.modules.Modules;
@Module(name = "DeathText", category = "Render")
public class DeathText {
    @Parameter(name = "Text")
    public String deathText = "JustFuckedUp";
    public static String lastCustomText = "";
    private DeathText() {
        
        lastCustomText = deathText;
    }
    public static Component getDeathComponent() {
        if (!Modules.enabled(DeathText.class)) return null;
        String text = Modules.get(DeathText.class).deathText;
        if (text == null || text.isEmpty()) return null;
        return Component.literal(text);
    }





}