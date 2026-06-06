package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import net.minecraft.client.Minecraft;

public class Fullbright extends Module {
    public static final Fullbright INSTANCE = new Fullbright();

    private Fullbright() {
        super("Fullbright", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }
}
