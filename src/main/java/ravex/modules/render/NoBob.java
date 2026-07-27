package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.mcwrapper.MinecraftWrapper;
@ModuleInfo(name = "NoBob", category = "Render")
public class NoBob implements ModuleAccess {
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
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("NoBob").getEnabled();
    }

    public static NoBob itz() {
        return ravex.manager.ModuleManager.delegate(NoBob.class);
    }


}