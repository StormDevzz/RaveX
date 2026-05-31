package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import net.minecraft.client.Minecraft;

public class Fullbright extends Module {
    public static final Fullbright INSTANCE = new Fullbright();
    private double oldGamma = 1.0;
    private boolean gammaSaved = false;

    private Fullbright() {
        super("Fullbright", Category.RENDER);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;

        if (!gammaSaved) {
            oldGamma = mc.options.gamma().get();
            gammaSaved = true;
        }

        mc.options.gamma().set(1.0);
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            mc.options.gamma().set(oldGamma);
        }
        gammaSaved = false;
    }
}
