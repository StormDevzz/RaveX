package ravex.modules.render;
import ravex.manager.ModuleManager;
import net.minecraft.network.chat.Component;
import ravex.modules.Module;
import ravex.parameter.StringParameter;
public class DeathText extends Module {
    public final StringParameter deathText = new StringParameter("Text", "JustFuckedUp");
    public static String lastCustomText = "";
    private DeathText() {
        super("DeathText");
        lastCustomText = deathText.getValue();
    }
    public static Component getDeathComponent() {
        if (!ModuleManager.get(DeathText.class).getEnabled()) return null;
        String text = ModuleManager.get(DeathText.class).deathText.getValue();
        if (text == null || text.isEmpty()) return null;
        return Component.literal(text);
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(DeathText.class);
    }

    public static DeathText itz() {
        return ModuleManager.get(DeathText.class);
    }
}
