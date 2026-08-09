package ravex.utility.misc.block;

import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import ravex.utility.player.ToolUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotationUtility;
import java.util.ArrayList;
import java.util.List;

public class BlockUtility {
    public static class BreakConfig {
        public boolean swing = true;
        public boolean rotate = false;
        public InteractionHand hand = InteractionHand.MAIN_HAND;
        public SilentRotationUtility silentRotation = null;
        public Direction face = Direction.UP;
    }

    public static class PlaceConfig {
        public boolean swing = true;
        public boolean rotate = false;
        public boolean restoreSlot = true;
        public InteractionHand hand = InteractionHand.MAIN_HAND;
        public SilentRotationUtility silentRotation = null;
    }

    public static boolean breakBlock(MinecraftWrapper mc, BlockPos pos) {
        var _mc = mc.getRaw();
        return breakBlock(mc, pos, new BreakConfig());
    }

    public static boolean breakBlock(MinecraftWrapper mc, BlockPos pos, BreakConfig cfg) {
        var _mc = mc.getRaw();
        if (_mc.player == null || _mc.level == null || _mc.gameMode == null) return false;
        BlockState state = _mc.level.getBlockState(pos);
        int prev = InventoryUtility.getSelectedSlot(_mc.player);
        int toolSlot = ToolUtility.findBestToolSlot(_mc.player, state);
        if (toolSlot != -1) InventoryUtility.selectSlot(_mc.player, toolSlot);
        if (cfg.rotate && cfg.silentRotation != null) {
            cfg.silentRotation.setAnglesTo(mc, pos.getCenter());
            cfg.silentRotation.hasRotation = true;
        }
        _mc.gameMode.startDestroyBlock(pos, cfg.face);
        if (cfg.swing) SwingUtility.swing(_mc.player, cfg.hand);
        if (toolSlot != -1) InventoryUtility.selectSlot(_mc.player, prev);
        return true;
    }

    public static boolean breakBlock(MinecraftWrapper mc, BlockPos pos, String grimMode) {
        var _mc = mc.getRaw();
        if (_mc.player == null || _mc.level == null || _mc.gameMode == null) return false;
        switch (grimMode) {
            case "Strict":
                _mc.gameMode.continueDestroyBlock(pos, Direction.UP);
                return true;
            case "Dev":
                _mc.gameMode.startDestroyBlock(pos, Direction.UP);
                SwingUtility.swing(_mc.player, InteractionHand.MAIN_HAND);
                for (int i = 0; i < 3; i++) {
                    _mc.gameMode.continueDestroyBlock(pos, Direction.UP);
                }
                return true;
            default:
                return breakBlock(mc, pos);
        }
    }

    public static int startBreak(MinecraftWrapper mc, BlockPos pos, BreakConfig cfg) {
        var _mc = mc.getRaw();
        if (_mc.player == null || _mc.level == null || _mc.gameMode == null) return -1;
        BlockState state = _mc.level.getBlockState(pos);
        int toolSlot = ToolUtility.findBestToolSlot(_mc.player, state);
        if (toolSlot != -1) InventoryUtility.selectSlot(_mc.player, toolSlot);
        if (cfg.rotate && cfg.silentRotation != null) {
            cfg.silentRotation.setAnglesTo(mc, pos.getCenter());
            cfg.silentRotation.hasRotation = true;
        }
        _mc.gameMode.startDestroyBlock(pos, cfg.face);
        if (cfg.swing) SwingUtility.swing(_mc.player, cfg.hand);
        return toolSlot;
    }

    public static int startBreak(MinecraftWrapper mc, int x, int y, int z, BreakConfig cfg) {
        var _mc = mc.getRaw();
        return startBreak(mc, new BlockPos(x, y, z), cfg);
    }

    public static void continueBreak(MinecraftWrapper mc, BlockPos pos, BreakConfig cfg) {
        var _mc = mc.getRaw();
        if (_mc.player == null || _mc.level == null || _mc.gameMode == null) return;
        if (cfg.rotate && cfg.silentRotation != null) {
            cfg.silentRotation.setAnglesTo(mc, pos.getCenter());
            cfg.silentRotation.hasRotation = true;
        }
        _mc.gameMode.continueDestroyBlock(pos, cfg.face);
        if (cfg.swing) SwingUtility.swing(_mc.player, cfg.hand);
    }

    public static void continueBreak(MinecraftWrapper mc, int x, int y, int z, BreakConfig cfg) {
        var _mc = mc.getRaw();
        continueBreak(mc, new BlockPos(x, y, z), cfg);
    }

    public static void stopBreak(MinecraftWrapper mc) {
        var _mc = mc.getRaw();
        if (_mc.gameMode != null) _mc.gameMode.stopDestroyBlock();
    }

    public static void stopBreak(MinecraftWrapper mc, int slotToRestore) {
        var _mc = mc.getRaw();
        if (_mc.gameMode != null) _mc.gameMode.stopDestroyBlock();
        if (slotToRestore != -1) InventoryUtility.selectSlot(_mc.player, slotToRestore);
    }

    public static BlockHitResult findPlaceTarget(MinecraftWrapper mc, BlockPos target) {
        var _mc = mc.getRaw();
        Vec3 eye = _mc.player.getEyePosition();
        BlockPos bestNeighbor = null;
        Direction bestFace = Direction.UP;
        double bestDist = Double.MAX_VALUE;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = target.relative(dir);
            BlockState st = _mc.level.getBlockState(neighbor);
            if (st.isCollisionShapeFullBlock(_mc.level, neighbor)) {
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

    public static BlockHitResult placeOnTop(MinecraftWrapper mc, BlockPos supportBlock) {
        var _mc = mc.getRaw();
        return new BlockHitResult(Vec3.atCenterOf(supportBlock), Direction.UP, supportBlock, false);
    }

    public static boolean placeBlock(MinecraftWrapper mc, BlockPos target, int slot) {
        var _mc = mc.getRaw();
        return placeBlock(mc, target, slot, new PlaceConfig());
    }

    public static boolean placeBlock(MinecraftWrapper mc, BlockPos target, int slot, PlaceConfig cfg) {
        var _mc = mc.getRaw();
        int prev = InventoryUtility.getSelectedSlot(_mc.player);
        InventoryUtility.selectSlot(_mc.player, slot);
        BlockHitResult hit = findPlaceTarget(mc, target);
        if (hit == null) {
            if (cfg.restoreSlot) InventoryUtility.selectSlot(_mc.player, prev);
            return false;
        }
        if (cfg.rotate && cfg.silentRotation != null) {
            Vec3 hitCenter = Vec3.atCenterOf(target);
            cfg.silentRotation.setAnglesTo(mc, hitCenter);
            cfg.silentRotation.hasRotation = true;
        }
        _mc.gameMode.useItemOn(_mc.player, cfg.hand, hit);
        if (cfg.swing) SwingUtility.swing(_mc.player, cfg.hand);
        if (cfg.restoreSlot) InventoryUtility.selectSlot(_mc.player, prev);
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

    public static void useItemOn(MinecraftWrapper mc, BlockHitResult hit) {
        var _mc = mc.getRaw();
        useItemOn(mc, hit, InteractionHand.MAIN_HAND);
    }

    public static void useItemOn(MinecraftWrapper mc, BlockHitResult hit, InteractionHand hand) {
        var _mc = mc.getRaw();
        if (_mc.player != null && _mc.gameMode != null)
            _mc.gameMode.useItemOn(_mc.player, hand, hit);
    }

    public static void swing(MinecraftWrapper mc) {
        var _mc = mc.getRaw();
        if (_mc.player != null) SwingUtility.swingMainHand((LocalPlayer) _mc.player);
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

    public static BlockState getState(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos);
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

    public static boolean grimAirPlace(MinecraftWrapper mc, BlockPos target, InteractionHand hand) {
        var _mc = mc.getRaw();
        if (_mc.player == null || _mc.level == null || _mc.gameMode == null) return false;
        for (Direction dir : Direction.values()) {
            BlockPos side = target.relative(dir);
            BlockState state = _mc.level.getBlockState(side);
            if (state.isAir() || state.liquid()) continue;
            Vec3 hitVec = Vec3.atCenterOf(side).add(
                new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ()).scale(0.5)
            );
            BlockHitResult bhr = new BlockHitResult(hitVec, dir.getOpposite(), side, false);
            useItemOn(mc, bhr, hand);
            return true;
        }
        return false;
    }

    public static boolean grimAirPlaceDesync(MinecraftWrapper mc, BlockPos target, BlockPos breakPos, InteractionHand hand) {
        var _mc = mc.getRaw();
        if (_mc.player == null || _mc.level == null || _mc.gameMode == null) return false;
        _mc.gameMode.startDestroyBlock(breakPos, Direction.UP);
        for (Direction dir : Direction.values()) {
            BlockPos side = target.relative(dir);
            if (side.equals(breakPos)) {
                Vec3 hitVec = Vec3.atCenterOf(side).add(
                    new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ()).scale(0.5)
                );
                BlockHitResult bhr = new BlockHitResult(hitVec, dir.getOpposite(), side, false);
                useItemOn(mc, bhr, hand);
                return true;
            }
        }
        return grimAirPlace(mc, target, hand);
    }

    public static boolean ncpBreakBlock(MinecraftWrapper mc, BlockPos pos, Direction face) {
        var _mc = mc.getRaw();
        if (_mc.player == null || _mc.level == null || _mc.gameMode == null) return false;
        if (_mc.player.distanceToSqr(Vec3.atCenterOf(pos)) > 6.25) return false;
        _mc.gameMode.startDestroyBlock(pos, face);
        SwingUtility.swing(_mc.player, InteractionHand.MAIN_HAND);
        return true;
    }

    public static boolean isExplodable(Level level) {
        var dim = level.dimension();
        return dim == Level.NETHER || dim == Level.END;
    }

    public static boolean isLoaded(Level level, BlockPos pos) {
        return level.isLoaded(pos);
    }

    public static int getMinY(Level level) {
        return level.getMinY();
    }

    public static boolean ncpAirPlace(MinecraftWrapper mc, BlockPos pos, Direction face, InteractionHand hand) {
        var _mc = mc.getRaw();
        if (_mc.player == null || _mc.level == null || _mc.gameMode == null) return false;
        if (_mc.player.distanceToSqr(Vec3.atCenterOf(pos)) > 6.25) return false;
        Vec3 hitVec = Vec3.atCenterOf(pos).add(
            new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.5)
        );
        BlockHitResult bhr = new BlockHitResult(hitVec, face.getOpposite(), pos, false);
        useItemOn(mc, bhr, hand);
        return true;
    }
}
