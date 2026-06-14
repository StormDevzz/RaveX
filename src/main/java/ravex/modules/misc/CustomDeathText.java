package ravex.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.StringParameter;

public class CustomDeathText extends Module {
    public static final CustomDeathText INSTANCE = new CustomDeathText();

    public final StringParameter deathText = new StringParameter("Text", "Just fucked up");

    public static String lastCustomText = "";

    private CustomDeathText() {
        super("CustomDeathText", Category.MISC);
        addParameter(deathText);
        lastCustomText = deathText.getValue();
    }

    public static Component getDeathComponent() {
        if (!INSTANCE.getEnabled()) return null;
        String text = INSTANCE.deathText.getValue();
        if (text == null || text.isEmpty()) return null;
        return Component.literal(text);
    }
}
