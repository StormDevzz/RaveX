package ravex.modules.player;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.ToolUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
public class AutoTool extends Module {
    public static final AutoTool INSTANCE = new AutoTool();
    private final BooleanParameter silent = new BooleanParameter("Silent", true);

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!mc.options.keyAttack.isDown()) return;
        if (!(mc.hitResult instanceof BlockHitResult blockHit)) return;
        BlockPos pos = blockHit.getBlockPos();
        int slot = ToolUtility.findBestToolSlot(mc.player, mc.level.getBlockState(pos));
        if (slot < 0) return;
        if (silent.getValue())
            InventoryUtility.silentSelectSlot(mc.player, slot);
        else
            InventoryUtility.selectSlot(mc.player, slot);
    }
}
