package ravex.modules.player;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import ravex.mixin.client.AccessorMinecraft;
import net.minecraft.client.Minecraft;

public class FastUse extends Module {
    public static final FastUse INSTANCE = new FastUse();

    public final NumberParameter delay = new NumberParameter("Delay", 0.0, 0.0, 4.0, 1.0);

    private FastUse() {
        super("FastUse", Category.PLAYER);
        addParameter(delay);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        AccessorMinecraft accessor = (AccessorMinecraft) mc;
        int currentDelay = accessor.getRightClickDelay();
        int targetDelay = (int) delay.getValue().doubleValue();
        if (currentDelay > targetDelay) {
            accessor.setRightClickDelay(targetDelay);
        }
    }
}
