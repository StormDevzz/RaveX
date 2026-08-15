package ravex.modules.world;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import org.jetbrains.annotations.Nullable;

@Module(name = "TreeCutter", category = "World")
public class TreeCutter {
    @Parameter(name = "Range", min = 1.0, max = 6.0, step = 0.1)
    public double range = 4.5;
    @Parameter(name = "Rotate")
    public boolean rotate = true;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Color", color = true, visible = "render")
    public int color = 0xFF8B5A2B;
    private int miningX, miningY, miningZ;
    private boolean hasTarget;
    private int currentToolSlot = -1;

    @Nullable
    public static net.minecraft.core.BlockPos getMiningPos() {
        TreeCutter t = Modules.get(TreeCutter.class);
        if (!t.hasTarget) return null;
        return BlockUtility.pos(t.miningX, t.miningY, t.miningZ);
    }
    public void onDisable() {
        if (hasTarget) BlockUtility.stopBreak(MinecraftWrapper.getWrapper(), currentToolSlot);
        hasTarget = false;
        currentToolSlot = -1;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null || mc.getGameMode() == null) {
            hasTarget = false;
            return;
        }
        double[] logs = BlockUtility.findLogs(mc.getLevel(), player.blockPosition(), range);
        if (logs.length == 0) {
            if (hasTarget) BlockUtility.stopBreak(mc, currentToolSlot);
            hasTarget = false;
            currentToolSlot = -1;
            return;
        }
        double[] best = BlockUtility.findNearest(logs, player.getX(), player.getY(), player.getZ());
        if (best == null || best[0] < 0.5) {
            if (hasTarget) BlockUtility.stopBreak(mc, currentToolSlot);
            hasTarget = false;
            currentToolSlot = -1;
            return;
        }
        int tx = (int) best[1], ty = (int) best[2], tz = (int) best[3];
        if (rotate) BlockUtility.rotateTo(player, tx, ty, tz);
        BlockUtility.BreakConfig cfg = new BlockUtility.BreakConfig();
        if (!hasTarget || miningX != tx || miningY != ty || miningZ != tz) {
            if (hasTarget) BlockUtility.stopBreak(mc, currentToolSlot);
            miningX = tx;
            miningY = ty;
            miningZ = tz;
            hasTarget = true;
            currentToolSlot = BlockUtility.startBreak(mc, tx, ty, tz, cfg);
        } else {
            BlockUtility.continueBreak(mc, tx, ty, tz, cfg);
        }
    }
}
