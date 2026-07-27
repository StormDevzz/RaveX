package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "NoBob", category = "Render")
public class NoBob {
private boolean originalBob = true;
    public void onEnable() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.options != null) {
            originalBob = mc.options.bobView().get();
            mc.options.bobView().set(false);
        }
    }
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.options == null) return;
        if (mc.options.bobView().get()) {
            mc.options.bobView().set(false);
        }
    }
    public void onDisable() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.options == null) return;
        mc.options.bobView().set(originalBob);
    }





}