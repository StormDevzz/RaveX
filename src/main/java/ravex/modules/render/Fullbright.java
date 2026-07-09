package ravex.modules.render;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
public class Fullbright extends Module {
<<<<<<< HEAD
=======
    public static final Fullbright INSTANCE = new Fullbright();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final NumberParameter brightness = new NumberParameter("Brightness", 1.0, 0.0, 1.0, 0.05);
    public final NumberParameter darknessMult = new NumberParameter("DarknessMult", 0.0, 0.0, 1.0, 0.05);

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
    public static boolean maybeEnabled() {
        return maybeEnabled(Fullbright.class);
    }

    public static Fullbright itz() {
        return ModuleManager.get(Fullbright.class);
    }
}
