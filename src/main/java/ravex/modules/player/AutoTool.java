package ravex.modules.player;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.ToolUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import java.util.List;
public class AutoTool extends Module {
    public final ModeParameter swap = new ModeParameter("Swap", "Silent", List.of("Silent", "Normal"));

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!mc.options.keyAttack.isDown()) return;
        if (!(mc.hitResult instanceof BlockHitResult blockHit)) return;
        BlockPos pos = blockHit.getBlockPos();
        int slot = ToolUtility.findBestToolSlot(mc.player, mc.level.getBlockState(pos));
        if (slot < 0) return;
        if ("Silent".equals(swap.getValue()))
            InventoryUtility.silentSelectSlot(mc.player, slot);
        else
            InventoryUtility.selectSlot(mc.player, slot);
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(AutoTool.class);
    }
    public static AutoTool itz() {
        return ModuleManager.get(AutoTool.class);
    }

}