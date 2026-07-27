package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
@ModuleInfo(name = "Fullbright", category = "Render")
public class Fullbright implements ModuleAccess {
    @Parameter(name = "Brightness", min = 0.0, max = 1.0, step = 0.05)
    public double brightness = 1.0;
    @Parameter(name = "DarknessMult", min = 0.0, max = 1.0, step = 0.05)
    public double darknessMult = 0.0;
    public void onEnable() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }
    public void onDisable() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Fullbright").getEnabled();
    }

    public static Fullbright itz() {
        return ravex.manager.ModuleManager.delegate(Fullbright.class);
    }


}