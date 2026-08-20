package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.modules.world.GhostBlocks;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import java.util.ArrayList;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;




@Module(name = "AntiReGear", category = "Combat")
public class AntiReGear {
    @Parameter(name = "Range", min = 1.0, max = 6.0, step = 0.1)
    public double range = 4.5;
    @Parameter(name = "Delay", min = 0, max = 1000, step = 50)
    public double delay = 100;
    @Parameter(name = "Shulkers")
    public boolean shulkersParam = true;
    @Parameter(name = "Chests")
    public boolean chestsParam = true;
    @Parameter(name = "EnderChests")
    public boolean enderChestsParam = true;
    @Parameter(name = "Barrels")
    public boolean barrelsParam = false;
    private net.minecraft.core.BlockPos currentMiningTarget = null;
    private long lastBreakTime = 0;
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_antiregear");
    public static native int nativeCalculateTarget(
        double playerX, double playerY, double playerZ,
        int[] blockX, int[] blockY, int[] blockZ,
        double range
    );
    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        if (currentMiningTarget != null && mc.getGameMode() != null) {
            mc.getGameMode().stopDestroyBlock();
        }
        currentMiningTarget = null;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        long now = System.currentTimeMillis();
        if (currentMiningTarget != null) {
            net.minecraft.world.level.block.state.BlockState state = mc.getLevel().getBlockState(currentMiningTarget);
            if (state.isAir() || !isTargetBlock(state)) {
                currentMiningTarget = null;
            } else {
                net.minecraft.core.Direction dir = getDirection(mc.getPlayer().getEyePosition(), currentMiningTarget);
                mc.getGameMode().continueDestroyBlock(currentMiningTarget, dir);
                SwingUtility.swing(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
                lastBreakTime = now;
                return;
            }
        }
        if (now - lastBreakTime < (long) delay) return;
        double r = range;
        net.minecraft.core.BlockPos playerPos = mc.getPlayer().blockPosition();
        int minX = (int) Math.floor(playerPos.getX() - r);
        int maxX = (int) Math.ceil(playerPos.getX() + r);
        int minY = (int) Math.max(mc.getLevel().getMinY(), Math.floor(playerPos.getY() - r));
        int maxY = (int) Math.min(mc.getLevel().getMaxY(), Math.ceil(playerPos.getY() + r));
        int minZ = (int) Math.floor(playerPos.getZ() - r);
        int maxZ = (int) Math.ceil(playerPos.getZ() + r);
        List<net.minecraft.core.BlockPos> candidates = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
                    if (pos.closerThan(playerPos, r)) {
                        net.minecraft.world.level.block.state.BlockState state = mc.getLevel().getBlockState(pos);
                        if (isTargetBlock(state)) {
                            candidates.add(pos);
                        }
                    }
                }
            }
        }
        if (candidates.isEmpty()) return;
        net.minecraft.core.BlockPos target = null;
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
                net.minecraft.world.phys.Vec3 eye = mc.getPlayer().getEyePosition();
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
            net.minecraft.core.Direction dir = getDirection(mc.getPlayer().getEyePosition(), target);
            mc.getGameMode().startDestroyBlock(target, dir);
            SwingUtility.swing(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
            GhostBlocks.markMined(target);
            lastBreakTime = now;
        }
    }
    private boolean isTargetBlock(net.minecraft.world.level.block.state.BlockState state) {
        Block block = state.getBlock();
        if (block instanceof ShulkerBoxBlock) return shulkersParam;
        if (block instanceof ChestBlock) return chestsParam;
        if (block instanceof EnderChestBlock) return enderChestsParam;
        if (block instanceof BarrelBlock) return barrelsParam;
        return false;
    }
    private net.minecraft.core.BlockPos fallbackFindTarget(List<net.minecraft.core.BlockPos> candidates, MinecraftWrapper mc) {
        net.minecraft.world.phys.Vec3 eye = mc.getPlayer().getEyePosition();
        net.minecraft.core.BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;
        for (net.minecraft.core.BlockPos pos : candidates) {
            double distSq = eye.distanceToSqr(PhysicUtility.centerOf(pos));
            if (distSq < closestDist) {
                closestDist = distSq;
                closest = pos;
            }
        }
        return closest;
    }
    public static net.minecraft.core.Direction getDirection(net.minecraft.world.phys.Vec3 eye, net.minecraft.core.BlockPos pos) {
        net.minecraft.world.phys.Vec3 center = PhysicUtility.centerOf(pos);
        double dx = eye.x - center.x;
        double dy = eye.y - pos.getY() - 0.5;
        double dz = eye.z - center.z;
        double absX = Math.abs(dx);
        double absY = Math.abs(dy);
        double absZ = Math.abs(dz);
        if (absY <= absX && absY <= absZ) {
            if (absX >= absZ) {
                return dx > 0 ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST;
            } else {
                return dz > 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH;
            }
        } else if (absX <= absY && absX <= absZ) {
            if (absY >= absZ) {
                return dy > 0 ? net.minecraft.core.Direction.DOWN : net.minecraft.core.Direction.UP;
            } else {
                return dz > 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH;
            }
        } else {
            if (absY >= absX) {
                return dy > 0 ? net.minecraft.core.Direction.DOWN : net.minecraft.core.Direction.UP;
            } else {
                return dx > 0 ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST;
            }
        }
    }




}