package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "Fullbright", category = "Render")
public class Fullbright {
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





}