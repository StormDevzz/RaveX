package ravex.utility.misc.block;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import ravex.utility.player.ToolUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotation;
import java.util.ArrayList;
import java.util.List;

public class BlockUtility {
    public static class BreakConfig {
        public boolean swing = true;
        public boolean rotate = false;
        public InteractionHand hand = InteractionHand.MAIN_HAND;
        public SilentRotation silentRotation = null;
        public Direction face = Direction.UP;
    }

    public static class PlaceConfig {
        public boolean swing = true;
        public boolean rotate = false;
        public boolean restoreSlot = true;
        public InteractionHand hand = InteractionHand.MAIN_HAND;
        public SilentRotation silentRotation = null;
    }

    public static boolean breakBlock(Minecraft mc, BlockPos pos) {
        return breakBlock(mc, pos, new BreakConfig());
    }

    public static boolean breakBlock(Minecraft mc, BlockPos pos, BreakConfig cfg) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return false;
        BlockState state = mc.level.getBlockState(pos);
        int prev = InventoryUtility.getSelectedSlot(mc.player);
        int toolSlot = ToolUtility.findBestToolSlot(mc.player, state);
        if (toolSlot != -1) InventoryUtility.selectSlot(mc.player, toolSlot);
        if (cfg.rotate && cfg.silentRotation != null) {
            cfg.silentRotation.setAnglesTo(mc, pos.getCenter());
            cfg.silentRotation.hasRotation = true;
        }
        mc.gameMode.startDestroyBlock(pos, cfg.face);
        if (cfg.swing) SwingUtility.swing(mc.player, cfg.hand);
        if (toolSlot != -1) InventoryUtility.selectSlot(mc.player, prev);
        return true;
    }

    public static int startBreak(Minecraft mc, BlockPos pos, BreakConfig cfg) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return -1;
        BlockState state = mc.level.getBlockState(pos);
        int toolSlot = ToolUtility.findBestToolSlot(mc.player, state);
        if (toolSlot != -1) InventoryUtility.selectSlot(mc.player, toolSlot);
        if (cfg.rotate && cfg.silentRotation != null) {
            cfg.silentRotation.setAnglesTo(mc, pos.getCenter());
            cfg.silentRotation.hasRotation = true;
        }
        mc.gameMode.startDestroyBlock(pos, cfg.face);
        if (cfg.swing) SwingUtility.swing(mc.player, cfg.hand);
        return toolSlot;
    }

    public static int startBreak(Minecraft mc, int x, int y, int z, BreakConfig cfg) {
        return startBreak(mc, new BlockPos(x, y, z), cfg);
    }

    public static void continueBreak(Minecraft mc, BlockPos pos, BreakConfig cfg) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (cfg.rotate && cfg.silentRotation != null) {
            cfg.silentRotation.setAnglesTo(mc, pos.getCenter());
            cfg.silentRotation.hasRotation = true;
        }
        mc.gameMode.continueDestroyBlock(pos, cfg.face);
        if (cfg.swing) SwingUtility.swing(mc.player, cfg.hand);
    }

    public static void continueBreak(Minecraft mc, int x, int y, int z, BreakConfig cfg) {
        continueBreak(mc, new BlockPos(x, y, z), cfg);
    }

    public static void stopBreak(Minecraft mc) {
        if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();
    }

    public static void stopBreak(Minecraft mc, int slotToRestore) {
        if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();
        if (slotToRestore != -1) InventoryUtility.selectSlot(mc.player, slotToRestore);
    }

    public static BlockHitResult findPlaceTarget(Minecraft mc, BlockPos target) {
        Vec3 eye = mc.player.getEyePosition();
        BlockPos bestNeighbor = null;
        Direction bestFace = Direction.UP;
        double bestDist = Double.MAX_VALUE;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = target.relative(dir);
            BlockState st = mc.level.getBlockState(neighbor);
            if (st.isCollisionShapeFullBlock(mc.level, neighbor)) {
                double d = neighbor.distToCenterSqr(eye);
                if (d < bestDist) {
                    bestDist = d;
                    bestNeighbor = neighbor;
                    bestFace = dir.getOpposite();
                }
            }
        }
        if (bestNeighbor != null) {
            Vec3 hitVec = Vec3.atCenterOf(bestNeighbor)
                .add(new Vec3(bestFace.getStepX(), bestFace.getStepY(), bestFace.getStepZ()).scale(0.5));
            return new BlockHitResult(hitVec, bestFace, bestNeighbor, false);
        }
        if (target.distToCenterSqr(eye) < 36.0) {
            return new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);
        }
        return null;
    }

    public static BlockHitResult placeOnTop(Minecraft mc, BlockPos supportBlock) {
        return new BlockHitResult(Vec3.atCenterOf(supportBlock), Direction.UP, supportBlock, false);
    }

    public static boolean placeBlock(Minecraft mc, BlockPos target, int slot) {
        return placeBlock(mc, target, slot, new PlaceConfig());
    }

    public static boolean placeBlock(Minecraft mc, BlockPos target, int slot, PlaceConfig cfg) {
        int prev = InventoryUtility.getSelectedSlot(mc.player);
        InventoryUtility.selectSlot(mc.player, slot);
        BlockHitResult hit = findPlaceTarget(mc, target);
        if (hit == null) {
            if (cfg.restoreSlot) InventoryUtility.selectSlot(mc.player, prev);
            return false;
        }
        if (cfg.rotate && cfg.silentRotation != null) {
            Vec3 hitCenter = Vec3.atCenterOf(target);
            cfg.silentRotation.setAnglesTo(mc, hitCenter);
            cfg.silentRotation.hasRotation = true;
        }
        mc.gameMode.useItemOn(mc.player, cfg.hand, hit);
        if (cfg.swing) SwingUtility.swing(mc.player, cfg.hand);
        if (cfg.restoreSlot) InventoryUtility.selectSlot(mc.player, prev);
        return true;
    }

    public static double[] findLogs(Level level, BlockPos center, double range) {
        List<Double> coords = new ArrayList<>();
        int r = (int) Math.ceil(range);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (center.distToCenterSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > range * range) continue;
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && isLog(state)) {
                        coords.add((double) pos.getX());
                        coords.add((double) pos.getY());
                        coords.add((double) pos.getZ());
                    }
                }
            }
        }
        double[] arr = new double[coords.size()];
        for (int i = 0; i < coords.size(); i++) arr[i] = coords.get(i);
        return arr;
    }

    public static boolean isLog(BlockState state) {
        String name = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().toLowerCase();
        return isLogName(name);
    }

    public static boolean isLogName(String name) {
        return name.contains("_log") || name.contains("log_") || name.contains("_stem") || name.contains("_wood") || name.endsWith("wood") || name.endsWith("log");
    }

    public static double[] findNearest(double[] logBlocks, double playerX, double playerY, double playerZ) {
        if (logBlocks == null || logBlocks.length < 3) return null;
        double bestDist = Double.MAX_VALUE;
        double bestX = 0, bestY = 0, bestZ = 0;
        for (int i = 0; i < logBlocks.length; i += 3) {
            double x = logBlocks[i], y = logBlocks[i + 1], z = logBlocks[i + 2];
            double dx = (x + 0.5) - playerX, dy = (y + 0.5) - playerY, dz = (z + 0.5) - playerZ;
            double d = dx * dx + dy * dy + dz * dz;
            if (d < bestDist) {
                bestDist = d;
                bestX = x; bestY = y; bestZ = z;
            }
        }
        if (bestDist == Double.MAX_VALUE) return null;
        return new double[]{1.0, bestX, bestY, bestZ};
    }

    public static void rotateTo(LocalPlayer player, int x, int y, int z) {
        Vec3 target = Vec3.atCenterOf(new BlockPos(x, y, z));
        float[] angles = RotationUtility.anglesTo(player.getEyePosition(), target);
        player.setYRot(angles[0]);
        player.setXRot(angles[1]);
    }

    public static boolean isBlock(BlockState state, String name) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id.getPath().equals(name) || id.toString().equals(name);
    }

    public static boolean isBlock(Level level, BlockPos pos, String name) {
        return isBlock(level.getBlockState(pos), name);
    }

    public static void useItemOn(Minecraft mc, BlockHitResult hit) {
        useItemOn(mc, hit, InteractionHand.MAIN_HAND);
    }

    public static void useItemOn(Minecraft mc, BlockHitResult hit, InteractionHand hand) {
        if (mc.player != null && mc.gameMode != null)
            mc.gameMode.useItemOn(mc.player, hand, hit);
    }

    public static void swing(Minecraft mc) {
        if (mc.player != null) SwingUtility.swingMainHand((LocalPlayer) mc.player);
    }

    public static boolean isAir(Level level, BlockPos pos) {
        return level.getBlockState(pos).isAir();
    }

    public static boolean isSolid(Level level, BlockPos pos) {
        return level.getBlockState(pos).isCollisionShapeFullBlock(level, pos);
    }

    public static boolean isLiquid(Level level, BlockPos pos) {
        return level.getBlockState(pos).liquid();
    }

    public static float destroySpeed(Level level, BlockPos pos) {
        return level.getBlockState(pos).getDestroySpeed(level, pos);
    }

    public static BlockPos pos(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    public static int belowY(int y) { return y - 1; }
    public static int aboveY(int y) { return y + 1; }

    public static BlockState getState(Level level, int x, int y, int z) {
        return level.getBlockState(pos(x, y, z));
    }

    public static boolean isAir(Level level, int x, int y, int z) {
        return getState(level, x, y, z).isAir();
    }

    public static boolean isSolid(Level level, int x, int y, int z) {
        return getState(level, x, y, z).isCollisionShapeFullBlock(level, pos(x, y, z));
    }

    public static boolean isLiquid(Level level, int x, int y, int z) {
        return getState(level, x, y, z).liquid();
    }

    public static double distToSqr(Level level, int x, int y, int z, double px, double py, double pz) {
        return pos(x, y, z).distToCenterSqr(px, py, pz);
    }

    public static long packPos(int x, int y, int z) {
        return BlockPos.asLong(x, y, z);
    }

    public static int unpackX(long packed) { return BlockPos.getX(packed); }
    public static int unpackY(long packed) { return BlockPos.getY(packed); }
    public static int unpackZ(long packed) { return BlockPos.getZ(packed); }
    public static BlockPos fromPacked(long packed) { return BlockPos.of(packed); }

    public static BlockPos containing(double x, double y, double z) {
        return BlockPos.containing(x, y, z);
    }

    public static BlockPos offset(BlockPos pos, int dx, int dy, int dz) {
        return pos.offset(dx, dy, dz);
    }

    public static BlockPos below(BlockPos pos) { return pos.below(); }
    public static BlockPos above(BlockPos pos) { return pos.above(); }
    public static BlockPos relative(BlockPos pos, Direction dir) { return pos.relative(dir); }
}
