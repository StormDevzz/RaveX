package ravex.modules.combat;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.modules.world.GhostBlocks;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import java.util.ArrayList;
import java.util.List;




@ModuleInfo(name = "AntiReGear", category = "Combat")
public class AntiReGear implements ModuleAccess {
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
    static {
        NATIVE.load();
    }
    public static native int nativeCalculateTarget(
        double playerX, double playerY, double playerZ,
        int[] blockX, int[] blockY, int[] blockZ,
        double range
    );
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (currentMiningTarget != null && mc.gameMode != null) {
            mc.gameMode.stopDestroyBlock();
        }
        currentMiningTarget = null;
    }
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        long now = System.currentTimeMillis();
        if (currentMiningTarget != null) {
            net.minecraft.world.level.block.state.BlockState state = mc.level.getBlockState(currentMiningTarget);
            if (state.isAir() || !isTargetBlock(state)) {
                currentMiningTarget = null;
            } else {
                net.minecraft.core.Direction dir = getDirection(mc.player.getEyePosition(), currentMiningTarget);
                mc.gameMode.continueDestroyBlock(currentMiningTarget, dir);
                SwingUtility.swing(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
                lastBreakTime = now;
                return;
            }
        }
        if (now - lastBreakTime < (long) delay) return;
        double r = range;
        net.minecraft.core.BlockPos playerPos = mc.player.blockPosition();
        int minX = (int) Math.floor(playerPos.getX() - r);
        int maxX = (int) Math.ceil(playerPos.getX() + r);
        int minY = (int) Math.max(mc.level.getMinY(), Math.floor(playerPos.getY() - r));
        int maxY = (int) Math.min(mc.level.getMaxY(), Math.ceil(playerPos.getY() + r));
        int minZ = (int) Math.floor(playerPos.getZ() - r);
        int maxZ = (int) Math.ceil(playerPos.getZ() + r);
        List<net.minecraft.core.BlockPos> candidates = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
                    if (pos.closerThan(playerPos, r)) {
                        net.minecraft.world.level.block.state.BlockState state = mc.level.getBlockState(pos);
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
                net.minecraft.world.phys.Vec3 eye = mc.player.getEyePosition();
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
            net.minecraft.core.Direction dir = getDirection(mc.player.getEyePosition(), target);
            mc.gameMode.startDestroyBlock(target, dir);
            SwingUtility.swing(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
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
    private net.minecraft.core.BlockPos fallbackFindTarget(List<net.minecraft.core.BlockPos> candidates, Minecraft mc) {
        net.minecraft.world.phys.Vec3 eye = mc.player.getEyePosition();
        net.minecraft.core.BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;
        for (net.minecraft.core.BlockPos pos : candidates) {
            double distSq = eye.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos));
            if (distSq < closestDist) {
                closestDist = distSq;
                closest = pos;
            }
        }
        return closest;
    }
    public static net.minecraft.core.Direction getDirection(net.minecraft.world.phys.Vec3 eye, net.minecraft.core.BlockPos pos) {
        net.minecraft.world.phys.Vec3 center = net.minecraft.world.phys.Vec3.atCenterOf(pos);
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
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("AntiReGear").getEnabled();
    }
    public static AntiReGear itz() {
        return ravex.manager.ModuleManager.delegate(AntiReGear.class);
    }


}