package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.ToolUtility;
import net.minecraft.client.Minecraft;
import java.util.List;



@ModuleInfo(name = "AutoTool", category = "net.minecraft.world.entity.player.Player")
public class AutoTool implements ModuleAccess {
    @Parameter(name = "Swap", modes = {"Silent", "Normal"})
    public String swap = "Silent";
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!mc.options.keyAttack.isDown()) return;
        if (!(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit)) return;
        net.minecraft.core.BlockPos pos = blockHit.getBlockPos();
        int slot = ToolUtility.findBestToolSlot(mc.player, mc.level.getBlockState(pos));
        if (slot < 0) return;
        if ("Silent".equals(swap))
            InventoryUtility.silentSelectSlot(mc.player, slot);
        else
            InventoryUtility.selectSlot(mc.player, slot);
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("AutoTool").getEnabled();
    }
    public static AutoTool itz() {
        return ravex.manager.ModuleManager.delegate(AutoTool.class);
    }


}