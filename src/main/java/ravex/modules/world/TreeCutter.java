package ravex.modules.world;
<<<<<<< HEAD
import ravex.manager.ModuleManager;

=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import net.minecraft.client.Minecraft;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
<<<<<<< HEAD
import ravex.utility.misc.block.BlockUtility;

public class TreeCutter extends Module {
=======
import java.util.ArrayList;
import java.util.List;
import ravex.utility.nativelib.NativeLibrary;
public class TreeCutter extends Module {
    public static final TreeCutter INSTANCE = new TreeCutter();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final NumberParameter range = new NumberParameter("Range", 4.5, 1.0, 6.0, 0.1);
    public final BooleanParameter rotate = new BooleanParameter("Rotate", true);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0xFF8B5A2B);
<<<<<<< HEAD
    private int miningX, miningY, miningZ;
    private boolean hasTarget;
    private int currentToolSlot = -1;

    public static net.minecraft.core.BlockPos getMiningPos() {
        TreeCutter t = ModuleManager.get(TreeCutter.class);
        if (!t.hasTarget) return null;
        return BlockUtility.pos(t.miningX, t.miningY, t.miningZ);
=======
    public static BlockPos currentMiningBlock = null;
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_treecutter");
    static {
        NATIVE.load();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
<<<<<<< HEAD
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
=======
        List<BlockPos> logs = scanForLogs(mc);
        if (logs.isEmpty()) {
            if (currentMiningBlock != null) {
                mc.gameMode.stopDestroyBlock();
                currentMiningBlock = null;
            }
            return;
        }
        double[] logsData = new double[logs.size() * 3];
        for (int i = 0; i < logs.size(); i++) {
            BlockPos pos = logs.get(i);
            logsData[i * 3] = pos.getX();
            logsData[i * 3 + 1] = pos.getY();
            logsData[i * 3 + 2] = pos.getZ();
        }
        double[] result;
        if (NATIVE.isLoaded()) {
            result = nativeFindBestLog(mc.player.getX(), mc.player.getY(), mc.player.getZ(), logsData);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        } else {
            BlockUtility.continueBreak(mc, tx, ty, tz, cfg);
        }
<<<<<<< HEAD
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(TreeCutter.class);
    }
    public static TreeCutter itz() {
        return ModuleManager.get(TreeCutter.class);
=======
        if (result == null || result[0] < 0.5) {
            if (currentMiningBlock != null) {
                mc.gameMode.stopDestroyBlock();
                currentMiningBlock = null;
            }
            return;
        }
        BlockPos targetPos = new BlockPos((int) result[1], (int) result[2], (int) result[3]);
        if (rotate.getValue()) {
            rotateTo(mc, Vec3.atCenterOf(targetPos));
        }
        if (currentMiningBlock == null || !currentMiningBlock.equals(targetPos)) {
            if (currentMiningBlock != null) {
                mc.gameMode.stopDestroyBlock();
            }
            currentMiningBlock = targetPos;
            mc.gameMode.startDestroyBlock(targetPos, Direction.UP);
        } else {
            mc.gameMode.continueDestroyBlock(targetPos, Direction.UP);
        }
        mc.player.swing(InteractionHand.MAIN_HAND);
    }
    private List<BlockPos> scanForLogs(Minecraft mc) {
        List<BlockPos> found = new ArrayList<>();
        double r = range.getValue();
        BlockPos playerPos = mc.player.blockPosition();
        int rx = (int) Math.ceil(r);
        int ry = (int) Math.ceil(r);
        int rz = (int) Math.ceil(r);
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -ry; dy <= ry; dy++) {
                for (int dz = -rz; dz <= rz; dz++) {
                    BlockPos pos = playerPos.offset(dx, dy, dz);
                    if (mc.player.distanceToSqr(Vec3.atCenterOf(pos)) <= r * r) {
                        BlockState state = mc.level.getBlockState(pos);
                        if (!state.isAir()) {
                            String name = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().toLowerCase();
                            boolean isLog = name.contains("_log") || name.contains("log_") || name.contains("_stem") || name.contains("_wood") || name.endsWith("wood") || name.endsWith("log");
                            if (isLog) {
                                found.add(pos);
                            }
                        }
                    }
                }
            }
        }
        return found;
    }
    private void rotateTo(Minecraft mc, Vec3 target) {
        Vec3 eyes = mc.player.getEyePosition();
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        mc.player.setYRot(targetYaw);
        mc.player.setXRot(targetPitch);
    }
    public static boolean isNativeAvailable() {
        return NATIVE.isLoaded();
    }
    public static native double[] nativeFindBestLog(
        double playerX, double playerY, double playerZ,
        double[] logBlocksData
    );
    public static double[] javaFallbackFindBestLog(
        double playerX, double playerY, double playerZ,
        double[] logBlocksData
    ) {
        if (logBlocksData == null || logBlocksData.length < 3) {
            return new double[]{0.0};
        }
        double bestDistSqr = Double.MAX_VALUE;
        double bestX = 0, bestY = 0, bestZ = 0;
        boolean found = false;
        for (int i = 0; i + 2 < logBlocksData.length; i += 3) {
            double x = logBlocksData[i];
            double y = logBlocksData[i+1];
            double z = logBlocksData[i+2];
            double dx = (x + 0.5) - playerX;
            double dy = (y + 0.5) - playerY;
            double dz = (z + 0.5) - playerZ;
            double distSqr = dx * dx + dy * dy + dz * dz;
            if (distSqr < bestDistSqr) {
                bestDistSqr = distSqr;
                bestX = x;
                bestY = y;
                bestZ = z;
                found = true;
            }
        }
        if (found) {
            return new double[]{1.0, bestX, bestY, bestZ};
        }
        return new double[]{0.0};
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    }
}
