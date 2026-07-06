package ravex.modules.render;
import net.minecraft.network.chat.Component;
import ravex.modules.Module;
import ravex.parameter.StringParameter;
public class DeathText extends Module {
    public static final DeathText INSTANCE = new DeathText();
    public final StringParameter deathText = new StringParameter("Text", "JustFuckedUp");
    public static String lastCustomText = "";
    private DeathText() {
        super("DeathText");
        lastCustomText = deathText.getValue();
    }
    public static Component getDeathComponent() {
        if (!INSTANCE.getEnabled()) return null;
        String text = INSTANCE.deathText.getValue();
        if (text == null || text.isEmpty()) return null;
        return Component.literal(text);
    }
}
