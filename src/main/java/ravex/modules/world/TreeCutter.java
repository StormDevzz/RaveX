package ravex.modules.world;
import ravex.manager.ModuleManager;

import net.minecraft.client.Minecraft;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.misc.block.BlockUtility;

public class TreeCutter extends Module {
    public final NumberParameter range = new NumberParameter("Range", 4.5, 1.0, 6.0, 0.1);
    public final BooleanParameter rotate = new BooleanParameter("Rotate", true);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0xFF8B5A2B);
    private int miningX, miningY, miningZ;
    private boolean hasTarget;
    private int currentToolSlot = -1;

    public static net.minecraft.core.BlockPos getMiningPos() {
        TreeCutter t = ModuleManager.get(TreeCutter.class);
        if (!t.hasTarget) return null;
        return BlockUtility.pos(t.miningX, t.miningY, t.miningZ);
    }

    @Override
    protected void onDisable() {
        if (hasTarget) BlockUtility.stopBreak(Minecraft.getInstance(), currentToolSlot);
        hasTarget = false;
        currentToolSlot = -1;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            hasTarget = false;
            return;
        }
        double[] logs = BlockUtility.findLogs(mc.level, mc.player.blockPosition(), range.getValue());
        if (logs.length == 0) {
            if (hasTarget) BlockUtility.stopBreak(mc, currentToolSlot);
            hasTarget = false;
            currentToolSlot = -1;
            return;
        }
        double[] best = BlockUtility.findNearest(logs, mc.player.getX(), mc.player.getY(), mc.player.getZ());
        if (best == null || best[0] < 0.5) {
            if (hasTarget) BlockUtility.stopBreak(mc, currentToolSlot);
            hasTarget = false;
            currentToolSlot = -1;
            return;
        }
        int tx = (int) best[1], ty = (int) best[2], tz = (int) best[3];
        if (rotate.getValue()) BlockUtility.rotateTo(mc.player, tx, ty, tz);
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

    public static boolean maybeEnabled() {
        return maybeEnabled(TreeCutter.class);
    }
    public static TreeCutter itz() {
        return ModuleManager.get(TreeCutter.class);
    }
}
