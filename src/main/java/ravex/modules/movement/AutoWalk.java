package ravex.modules.movement;

import ravex.modules.Category;
import ravex.modules.Module;
import net.minecraft.client.Minecraft;

public class AutoWalk extends Module {
    public static final AutoWalk INSTANCE = new AutoWalk();

    private AutoWalk() {
        super("AutoWalk", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.options.keyUp.setDown(true);
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        mc.options.keyUp.setDown(false);
    }
}
