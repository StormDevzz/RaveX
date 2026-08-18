package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.network.chat.Component;

import ravex.modules.Modules;
import org.jetbrains.annotations.Nullable;
@Module(name = "DeathText", category = "Render")
public class DeathText {
    @Parameter(name = "Text")
    public String deathText = "JustFuckedUp";
    public static String lastCustomText = "";
    private DeathText() {
        
        lastCustomText = deathText;
    }
    @Nullable
    public static Component getDeathComponent() {
        if (!Modules.enabled(DeathText.class)) return null;
        String text = Modules.get(DeathText.class).deathText;
        if (text == null || text.isEmpty()) return null;
        return Component.literal(text);
    }





}