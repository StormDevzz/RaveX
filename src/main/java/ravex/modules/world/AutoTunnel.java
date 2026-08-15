package ravex.modules.world;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;

import ravex.utility.player.InventoryUtility;
import ravex.utility.misc.block.BlockUtility;
import java.util.ArrayList;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
import org.jetbrains.annotations.Nullable;
@Module(name = "AutoTunnel", category = "World")
public class AutoTunnel {
    @Parameter(name = "Range", min = 1.0, max = 10.0, step = 0.5)
    public double range = 5.0;
    @Parameter(name = "Height", min = 1, max = 3, step = 1)
    public double height = 2;
    @Parameter(name = "Width", min = 1, max = 3, step = 1)
    public double width = 2;
    @Parameter(name = "Delay", min = 50, max = 1000, step = 50)
    public double delay = 200;
    @Parameter(name = "FillLava")
    public boolean fillLava = true;
    @Parameter(name = "AutoWalk")
    public boolean autoWalk = false;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Color", color = true, visible = "render")
    public int color = 0x3FFFFF00;
    private static int targetX, targetY, targetZ;
    private static boolean hasTarget;
    private long lastActionTime = 0;
    private int miningX, miningY, miningZ;
    private boolean hasMiningTarget;

    @Nullable
    public static net.minecraft.core.BlockPos getCurrentTarget() {
        if (!hasTarget) return null;
        return BlockUtility.pos(targetX, targetY, targetZ);
    }
    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        if (hasMiningTarget && mc.getGameMode() != null) {
            mc.getGameMode().stopDestroyBlock();
        }
        hasMiningTarget = false;
        hasTarget = false;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        long now = System.currentTimeMillis();
        if (now - lastActionTime < delay) return;
        if (autoWalk) {
            mc.getOptions().keyUp.setDown(true);
        }
        List<Long> blocks = getTunnelBlocks(mc);
        if (blocks.isEmpty()) return;
        if (fillLava) {
            for (long packed : blocks) {
                int bx = BlockUtility.unpackX(packed), by = BlockUtility.unpackY(packed), bz = BlockUtility.unpackZ(packed);
                if (BlockUtility.isLiquid(mc.getLevel(), bx, by, bz)) {
                    fillBlock(mc, bx, by, bz);
                    lastActionTime = now;
                    return;
                }
            }
        }
        for (long packed : blocks) {
            int bx = BlockUtility.unpackX(packed), by = BlockUtility.unpackY(packed), bz = BlockUtility.unpackZ(packed);
            var state = BlockUtility.getState(mc.getLevel(), bx, by, bz);
            if (state.isAir() || state.liquid()) continue;
            if (state.getDestroySpeed(mc.getLevel(), BlockUtility.pos(bx, by, bz)) < 0) continue;
            if (hasMiningTarget && (miningX != bx || miningY != by || miningZ != bz)) {
                mc.getGameMode().stopDestroyBlock();
            }
            miningX = bx; miningY = by; miningZ = bz;
            hasMiningTarget = true;
            targetX = bx; targetY = by; targetZ = bz;
            hasTarget = true;
            BlockUtility.breakBlock(mc, BlockUtility.pos(bx, by, bz));
            lastActionTime = now;
            return;
        }
        if (hasMiningTarget) {
            mc.getGameMode().stopDestroyBlock();
        }
        hasMiningTarget = false;
        hasTarget = false;
    }
    private void fillBlock(MinecraftWrapper mc, int x, int y, int z) {
        if (!BlockUtility.isLiquid(mc.getLevel(), x, y, z)) return;
        int fillSlot = -1;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (stack.isEmpty()) continue;
            if (InventoryUtility.isItem(stack, "cobblestone") || InventoryUtility.isItem(stack, "dirt")
                || InventoryUtility.isItem(stack, "stone") || InventoryUtility.isItem(stack, "gravel")
                || InventoryUtility.isItem(stack, "netherrack") || InventoryUtility.isItem(stack, "end_stone")
                || InventoryUtility.isItem(stack, "cobbled_deepslate")) {
                fillSlot = i;
                break;
            }
        }
        if (fillSlot == -1) return;
        BlockUtility.placeBlock(mc, BlockUtility.pos(x, y, z), fillSlot);
    }
    private List<Long> getTunnelBlocks(MinecraftWrapper mc) {
        List<Long> result = new ArrayList<>();
        var eye = mc.getPlayer().getEyePosition();
        var facing = mc.getPlayer().getDirection();
        int h = (int) height;
        int w = (int) width;
        double r = range;
        var startPos = mc.getPlayer().blockPosition();
        int sx = startPos.getX(), sy = startPos.getY(), sz = startPos.getZ();
        for (int f = 0; f < 3; f++) {
            int step = f + 1;
            for (int dy = 0; dy < h; dy++) {
                for (int dx = 0; dx < w; dx++) {
                    int[] off = offsetCoords(facing, step, dx - (w / 2), dy);
                    int px = sx + off[0], py = sy + off[1], pz = sz + off[2];
                    if (BlockUtility.distToSqr(mc.getLevel(), px, py, pz, eye.x, eye.y, eye.z) > r * r) continue;
                    var state = BlockUtility.getState(mc.getLevel(), px, py, pz);
                    if (state.isAir()) continue;
                    if (state.liquid()) {
                        if (fillLava) {
                            result.add(BlockUtility.packPos(px, py, pz));
                        }
                        continue;
                    }
                    if (state.getDestroySpeed(mc.getLevel(), BlockUtility.pos(px, py, pz)) < 0) continue;
                    result.add(BlockUtility.packPos(px, py, pz));
                }
            }
            if (!result.isEmpty()) break;
        }
        return result;
    }
    private static int[] offsetCoords(net.minecraft.core.Direction facing, int forward, int right, int up) {
        int ox = 0, oz = 0;
        switch (facing) {
            case NORTH: ox = -right; oz = -forward; break;
            case SOUTH: ox = right; oz = forward; break;
            case WEST: ox = -forward; oz = right; break;
            case EAST: ox = forward; oz = -right; break;
        }
        return new int[]{ox, up, oz};
    }





}