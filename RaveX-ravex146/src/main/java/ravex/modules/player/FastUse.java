package ravex.modules.player;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.mixin.client.AccessorMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;

public class FastUse extends Module {
    public static final FastUse INSTANCE = new FastUse();

    public final NumberParameter delay = new NumberParameter("Delay", 0.0, 0.0, 4.0, 1.0);
    public final BooleanParameter blocks = new BooleanParameter("Blocks", true);
    public final BooleanParameter items = new BooleanParameter("Items", true);

    private FastUse() {
        super("FastUse", Category.PLAYER);
        addParameter(delay);
        addParameter(blocks);
        addParameter(items);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack stack = mc.player.getMainHandItem();
        boolean isBlock = !stack.isEmpty() && stack.getItem() instanceof BlockItem;

        boolean shouldApply = false;
        if (isBlock && blocks.getValue()) {
            shouldApply = true;
        } else if (!isBlock && items.getValue()) {
            shouldApply = true;
        }

        if (shouldApply) {
            AccessorMinecraft accessor = (AccessorMinecraft) mc;
            int currentDelay = accessor.getRightClickDelay();
            int targetDelay = (int) delay.getValue().doubleValue();
            if (currentDelay > targetDelay) {
                accessor.setRightClickDelay(targetDelay);
            }
        }
    }
}
