package ravex.modules.player;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.InventoryUtility;
import ravex.mixin.client.AccessorMinecraft;
import ravex.mixin.client.AccessorLivingEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
public class NoDelay extends Module {
    public final NumberParameter delay = new NumberParameter("Delay", 0.0, 0.0, 4.0, 1.0);
    public final BooleanParameter blocks = new BooleanParameter("Blocks", true);
    public final BooleanParameter items = new BooleanParameter("Items", true);
    public final BooleanParameter noJumpDelay = new BooleanParameter("NoJumpDelay", false);

    public NoDelay() {
        super("NoDelay");
    }

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
        if (noJumpDelay.getValue()) {
            ((AccessorLivingEntity) p).setNoJumpDelay(0);
        }
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(NoDelay.class);
    }
    public static NoDelay itz() {
        return ModuleManager.get(NoDelay.class);
    }

}