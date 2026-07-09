package ravex.modules.player;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
=======
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.ToolUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
<<<<<<< HEAD
import java.util.List;
public class AutoTool extends Module {
    public final ModeParameter swap = new ModeParameter("Swap", "Silent", List.of("Silent", "Normal"));
=======
import net.minecraft.world.phys.HitResult;
public class AutoTool extends Module {
    public static final AutoTool INSTANCE = new AutoTool();
    private final BooleanParameter silent = new BooleanParameter("Silent", true);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!mc.options.keyAttack.isDown()) return;
        if (!(mc.hitResult instanceof BlockHitResult blockHit)) return;
        BlockPos pos = blockHit.getBlockPos();
        int slot = ToolUtility.findBestToolSlot(mc.player, mc.level.getBlockState(pos));
        if (slot < 0) return;
<<<<<<< HEAD
        if ("Silent".equals(swap.getValue()))
=======
        if (silent.getValue())
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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