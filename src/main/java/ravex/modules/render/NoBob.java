package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "NoBob", category = "Render")
public class NoBob {
private boolean originalBob = true;
    public void onEnable() {
        var mc = MinecraftWrapper.getWrapper();
        var options = mc.getOptions();
        if (options != null) {
            originalBob = options.bobView().get();
            options.bobView().set(false);
        }
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var options = mc.getOptions();
        if (options == null) return;
        if (options.bobView().get()) {
            options.bobView().set(false);
        }
    }
    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        var options = mc.getOptions();
        if (options == null) return;
        options.bobView().set(originalBob);
    }
}
