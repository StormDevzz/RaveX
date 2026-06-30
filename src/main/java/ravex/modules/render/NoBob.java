package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import net.minecraft.client.Minecraft;

public class NoBob extends Module {
    public static final NoBob INSTANCE = new NoBob();
    
    private boolean originalBob = true;

    private NoBob() {
        super("NoBob", Category.RENDER);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        originalBob = mc.options.bobView().get();
        mc.options.bobView().set(false);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.bobView().get()) {
            mc.options.bobView().set(false);
        }
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        mc.options.bobView().set(originalBob);
    }
}
