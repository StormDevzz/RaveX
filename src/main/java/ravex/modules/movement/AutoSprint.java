package ravex.modules.movement;

import ravex.modules.Category;
import ravex.modules.Module;
import net.minecraft.client.Minecraft;

public class AutoSprint extends Module {
    public static final AutoSprint INSTANCE = new AutoSprint();

    private AutoSprint() {
        super("AutoSprint", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (mc.player.isSprinting()) return;
        if (mc.player.isUsingItem()) return;
        if (!mc.options.keyUp.isDown()) return;
        if (mc.player.getFoodData().getFoodLevel() <= 6 && !mc.player.isCreative()) return;

        mc.player.setSprinting(true);
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.setSprinting(false);
        }
    }
}
