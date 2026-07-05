package ravex.modules.render;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
public class Fullbright extends Module {
    public static final Fullbright INSTANCE = new Fullbright();
    public final NumberParameter brightness = new NumberParameter("Brightness", 1.0, 0.0, 1.0, 0.05);
    public final NumberParameter darknessMult = new NumberParameter("Darkness Mult", 0.0, 0.0, 1.0, 0.05);

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
