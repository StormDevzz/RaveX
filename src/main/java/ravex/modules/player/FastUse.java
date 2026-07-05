package ravex.modules.player;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.InventoryUtility;
import ravex.mixin.client.AccessorMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
public class FastUse extends Module {
    public static final FastUse INSTANCE = new FastUse();
    public final NumberParameter delay = new NumberParameter("Delay", 0.0, 0.0, 4.0, 1.0);
    public final BooleanParameter blocks = new BooleanParameter("Blocks", true);
    public final BooleanParameter items = new BooleanParameter("Items", true);

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        boolean isBlock = InventoryUtility.isHoldingBlock(p);
        if ((isBlock && blocks.getValue()) || (!isBlock && items.getValue())) {
            AccessorMinecraft accessor = (AccessorMinecraft) mc;
            int target = (int) delay.getValue().doubleValue();
            if (accessor.getRightClickDelay() > target)
                accessor.setRightClickDelay(target);
        }
    }
}
