package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.mixin.client.AccessorLivingEntity;
import ravex.mixin.client.AccessorMinecraft;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.InventoryUtility;
import ravex.mcwrapper.MinecraftWrapper;



@Module(name = "NoDelay", category = "net.minecraft.world.entity.player.Player")
public class NoDelay {
    @Parameter(name = "Delay", min = 0.0, max = 4.0, step = 1.0)
    public double delay = 0.0;
    @Parameter(name = "net.minecraft.world.level.block.Blocks")
    public boolean blocks = true;
    @Parameter(name = "Items")
    public boolean items = true;
    @Parameter(name = "NoJumpDelay")
    public boolean noJumpDelay = false;

    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        net.minecraft.client.player.LocalPlayer p = mc.player;
        if (p == null) return;
        boolean isBlock = InventoryUtility.isHoldingBlock(p);
        if ((isBlock && blocks) || (!isBlock && items)) {
            AccessorMinecraft accessor = (AccessorMinecraft) mc;
            int target = (int) (double) delay;
            if (accessor.getRightClickDelay() > target)
                accessor.setRightClickDelay(target);
        }
        if (noJumpDelay) {
            ((AccessorLivingEntity) p).setNoJumpDelay(0);
        }
    }




}