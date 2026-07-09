package ravex.modules.render;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import net.minecraft.client.Minecraft;
public class NoBob extends Module {
    private boolean originalBob = true;

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            originalBob = mc.options.bobView().get();
            mc.options.bobView().set(false);
        }
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        if (mc.options.bobView().get()) {
            mc.options.bobView().set(false);
        }
    }
    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        mc.options.bobView().set(originalBob);
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(NoBob.class);
    }

    public static NoBob itz() {
        return ModuleManager.get(NoBob.class);
    }
}
