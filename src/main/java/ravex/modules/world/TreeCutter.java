package ravex.modules.world;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;

import ravex.utility.misc.block.BlockUtility;
import ravex.mcwrapper.MinecraftWrapper;

@ModuleInfo(name = "TreeCutter", category = "World")
public class TreeCutter implements ModuleAccess {
    @Parameter(name = "Range", min = 1.0, max = 6.0, step = 0.1)
    public double range = 4.5;
    @Parameter(name = "Rotate")
    public boolean rotate = true;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Color", color = true)
    public int color = 0xFF8B5A2B;
    private int miningX, miningY, miningZ;
    private boolean hasTarget;
    private int currentToolSlot = -1;

    public static net.minecraft.core.BlockPos getMiningPos() {
        TreeCutter t = ravex.manager.ModuleManager.delegate(TreeCutter.class);
        if (!t.hasTarget) return null;
        return BlockUtility.pos(t.miningX, t.miningY, t.miningZ);
    }
    public void onDisable() {
        if (hasTarget) BlockUtility.stopBreak(ravex.mcwrapper.MinecraftWrapper.getWrapper(), currentToolSlot);
        hasTarget = false;
        currentToolSlot = -1;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        var mcw = ravex.mcwrapper.MinecraftWrapper.getWrapper();
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            hasTarget = false;
            return;
        }
        double[] logs = BlockUtility.findLogs(mc.level, mc.player.blockPosition(), range);
        if (logs.length == 0) {
            if (hasTarget) BlockUtility.stopBreak(mcw, currentToolSlot);
            hasTarget = false;
            currentToolSlot = -1;
            return;
        }
        double[] best = BlockUtility.findNearest(logs, mc.player.getX(), mc.player.getY(), mc.player.getZ());
        if (best == null || best[0] < 0.5) {
            if (hasTarget) BlockUtility.stopBreak(mcw, currentToolSlot);
            hasTarget = false;
            currentToolSlot = -1;
            return;
        }
        int tx = (int) best[1], ty = (int) best[2], tz = (int) best[3];
        if (rotate) BlockUtility.rotateTo(mc.player, tx, ty, tz);
        BlockUtility.BreakConfig cfg = new BlockUtility.BreakConfig();
        if (!hasTarget || miningX != tx || miningY != ty || miningZ != tz) {
            if (hasTarget) BlockUtility.stopBreak(mcw, currentToolSlot);
            miningX = tx;
            miningY = ty;
            miningZ = tz;
            hasTarget = true;
            currentToolSlot = BlockUtility.startBreak(mcw, tx, ty, tz, cfg);
        } else {
            BlockUtility.continueBreak(mcw, tx, ty, tz, cfg);
        }
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("TreeCutter").getEnabled();
    }
    public static TreeCutter itz() {
        return ravex.manager.ModuleManager.delegate(TreeCutter.class);
    }


}