package ravex.modules.combat;
import ravex.manager.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLibrary;
import ravex.modules.world.GhostBlocks;
import java.util.ArrayList;
import java.util.List;
public class AntiReGear extends Module {
    public final NumberParameter range = new NumberParameter("Range", 4.5, 1.0, 6.0, 0.1);
    public final NumberParameter delay = new NumberParameter("Delay", 100, 0, 1000, 50);
    public final BooleanParameter shulkersParam = new BooleanParameter("Shulkers", true);
    public final BooleanParameter chestsParam = new BooleanParameter("Chests", true);
    public final BooleanParameter enderChestsParam = new BooleanParameter("EnderChests", true);
    public final BooleanParameter barrelsParam = new BooleanParameter("Barrels", false);
    private BlockPos currentMiningTarget = null;
    private long lastBreakTime = 0;
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_antiregear");
    static {
        NATIVE.load();
    }
    public static native int nativeCalculateTarget(
        double playerX, double playerY, double playerZ,
        int[] blockX, int[] blockY, int[] blockZ,
        double range
    );

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (currentMiningTarget != null && mc.gameMode != null) {
            mc.gameMode.stopDestroyBlock();
        }
        currentMiningTarget = null;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        long now = System.currentTimeMillis();
        if (currentMiningTarget != null) {
            BlockState state = mc.level.getBlockState(currentMiningTarget);
            if (state.isAir() || !isTargetBlock(state)) {
                currentMiningTarget = null;
            } else {
                Direction dir = getDirection(mc.player.getEyePosition(), currentMiningTarget);
                mc.gameMode.continueDestroyBlock(currentMiningTarget, dir);
                mc.player.swing(InteractionHand.MAIN_HAND);
                lastBreakTime = now;
                return;
            }
        }
        if (now - lastBreakTime < delay.getValue().longValue()) return;
        double r = range.getValue();
        BlockPos playerPos = mc.player.blockPosition();
        int minX = (int) Math.floor(playerPos.getX() - r);
        int maxX = (int) Math.ceil(playerPos.getX() + r);
        int minY = (int) Math.max(mc.level.getMinY(), Math.floor(playerPos.getY() - r));
        int maxY = (int) Math.min(mc.level.getMaxY(), Math.ceil(playerPos.getY() + r));
        int minZ = (int) Math.floor(playerPos.getZ() - r);
        int maxZ = (int) Math.ceil(playerPos.getZ() + r);
        List<BlockPos> candidates = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (pos.closerThan(playerPos, r)) {
                        BlockState state = mc.level.getBlockState(pos);
                        if (isTargetBlock(state)) {
                            candidates.add(pos);
                        }
                    }
                }
            }
        }
        if (candidates.isEmpty()) return;
        BlockPos target = null;
        if (NATIVE.isLoaded()) {
            try {
                int cnt = candidates.size();
                int[] bx = new int[cnt];
                int[] by = new int[cnt];
                int[] bz = new int[cnt];
                for (int i = 0; i < cnt; i++) {
                    bx[i] = candidates.get(i).getX();
                    by[i] = candidates.get(i).getY();
                    bz[i] = candidates.get(i).getZ();
                }
                Vec3 eye = mc.player.getEyePosition();
                int resultIdx = nativeCalculateTarget(
                    eye.x, eye.y, eye.z,
                    bx, by, bz, r
                );
                if (resultIdx >= 0 && resultIdx < cnt) {
                    target = candidates.get(resultIdx);
                }
            } catch (Exception e) {
                target = null;
            }
        }
        if (target == null) {
            target = fallbackFindTarget(candidates, mc);
        }
        if (target != null) {
            currentMiningTarget = target;
            Direction dir = getDirection(mc.player.getEyePosition(), target);
            mc.gameMode.startDestroyBlock(target, dir);
            mc.player.swing(InteractionHand.MAIN_HAND);
            GhostBlocks.markMined(target);
            lastBreakTime = now;
        }
    }
    private boolean isTargetBlock(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof ShulkerBoxBlock) return shulkersParam.getValue();
        if (block instanceof ChestBlock) return chestsParam.getValue();
        if (block instanceof EnderChestBlock) return enderChestsParam.getValue();
        if (block instanceof BarrelBlock) return barrelsParam.getValue();
        return false;
    }
    private BlockPos fallbackFindTarget(List<BlockPos> candidates, Minecraft mc) {
        Vec3 eye = mc.player.getEyePosition();
        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;
        for (BlockPos pos : candidates) {
            double distSq = eye.distanceToSqr(Vec3.atCenterOf(pos));
            if (distSq < closestDist) {
                closestDist = distSq;
                closest = pos;
            }
        }
        return closest;
    }
    public static Direction getDirection(Vec3 eye, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        double dx = eye.x - center.x;
        double dy = eye.y - pos.getY() - 0.5;
        double dz = eye.z - center.z;
        double absX = Math.abs(dx);
        double absY = Math.abs(dy);
        double absZ = Math.abs(dz);
        if (absY <= absX && absY <= absZ) {
            if (absX >= absZ) {
                return dx > 0 ? Direction.EAST : Direction.WEST;
            } else {
                return dz > 0 ? Direction.SOUTH : Direction.NORTH;
            }
        } else if (absX <= absY && absX <= absZ) {
            if (absY >= absZ) {
                return dy > 0 ? Direction.DOWN : Direction.UP;
            } else {
                return dz > 0 ? Direction.SOUTH : Direction.NORTH;
            }
        } else {
            if (absY >= absX) {
                return dy > 0 ? Direction.DOWN : Direction.UP;
            } else {
                return dx > 0 ? Direction.EAST : Direction.WEST;
            }
        }
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(AntiReGear.class);
    }
    public static AntiReGear itz() {
        return ModuleManager.get(AntiReGear.class);
    }

}