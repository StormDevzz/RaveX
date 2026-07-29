package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.ToolUtility;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;

@Module(name = "AutoTool", category = "net.minecraft.world.entity.player.Player")
public class AutoTool {
    @Parameter(name = "Swap", modes = {"Silent", "Normal"})
    public String swap = "Silent";
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) return;
        if (!mc.isAttackKeyDown()) return;
        if (!(mc.getHitResult() instanceof net.minecraft.world.phys.BlockHitResult blockHit)) return;
        var pos = blockHit.getBlockPos();
        int slot = ToolUtility.findBestToolSlot(player, mc.getLevel().getBlockState(pos));
        if (slot < 0) return;
        if ("Silent".equals(swap))
            InventoryUtility.silentSelectSlot(player, slot);
        else
            InventoryUtility.selectSlot(player, slot);
    }
}
