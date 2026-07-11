package ravex.modules.world;
import net.minecraft.client.Minecraft;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
import ravex.manager.ModuleManager;
import ravex.utility.player.InventoryUtility;
import ravex.utility.misc.block.BlockUtility;
import java.util.ArrayList;
import java.util.List;
public class AutoTunnel extends Module {
    public final NumberParameter range = new NumberParameter("Range", 5.0, 1.0, 10.0, 0.5);
    public final NumberParameter height = new NumberParameter("Height", 2, 1, 3, 1);
    public final NumberParameter width = new NumberParameter("Width", 2, 1, 3, 1);
    public final NumberParameter delay = new NumberParameter("Delay", 200, 50, 1000, 50);
    public final BooleanParameter fillLava = new BooleanParameter("FillLava", true);
    public final BooleanParameter autoWalk = new BooleanParameter("AutoWalk", false);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0x3FFFFF00);
    private static int targetX, targetY, targetZ;
    private static boolean hasTarget;
    private long lastActionTime = 0;
    private int miningX, miningY, miningZ;
    private boolean hasMiningTarget;

    public static net.minecraft.core.BlockPos getCurrentTarget() {
        if (!hasTarget) return null;
        return BlockUtility.pos(targetX, targetY, targetZ);
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (hasMiningTarget && mc.gameMode != null) {
            mc.gameMode.stopDestroyBlock();
        }
        hasMiningTarget = false;
        hasTarget = false;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        long now = System.currentTimeMillis();
        if (now - lastActionTime < delay.getValue()) return;
        if (autoWalk.getValue()) {
            mc.options.keyUp.setDown(true);
        }
        List<Long> blocks = getTunnelBlocks(mc);
        if (blocks.isEmpty()) return;
        if (fillLava.getValue()) {
            for (long packed : blocks) {
                int bx = BlockUtility.unpackX(packed), by = BlockUtility.unpackY(packed), bz = BlockUtility.unpackZ(packed);
                if (BlockUtility.isLiquid(mc.level, bx, by, bz)) {
                    fillBlock(mc, bx, by, bz);
                    lastActionTime = now;
                    return;
                }
            }
        }
        for (long packed : blocks) {
            int bx = BlockUtility.unpackX(packed), by = BlockUtility.unpackY(packed), bz = BlockUtility.unpackZ(packed);
            var state = BlockUtility.getState(mc.level, bx, by, bz);
            if (state.isAir() || state.liquid()) continue;
            if (state.getDestroySpeed(mc.level, BlockUtility.pos(bx, by, bz)) < 0) continue;
            if (hasMiningTarget && (miningX != bx || miningY != by || miningZ != bz)) {
                mc.gameMode.stopDestroyBlock();
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
            mc.gameMode.stopDestroyBlock();
        }
        hasMiningTarget = false;
        hasTarget = false;
    }
    private void fillBlock(Minecraft mc, int x, int y, int z) {
        if (!BlockUtility.isLiquid(mc.level, x, y, z)) return;
        int fillSlot = -1;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
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
    private List<Long> getTunnelBlocks(Minecraft mc) {
        List<Long> result = new ArrayList<>();
        var eye = mc.player.getEyePosition();
        var facing = mc.player.getDirection();
        int h = height.getValue().intValue();
        int w = width.getValue().intValue();
        double r = range.getValue();
        var startPos = mc.player.blockPosition();
        int sx = startPos.getX(), sy = startPos.getY(), sz = startPos.getZ();
        for (int f = 0; f < 3; f++) {
            int step = f + 1;
            for (int dy = 0; dy < h; dy++) {
                for (int dx = 0; dx < w; dx++) {
                    int[] off = offsetCoords(facing, step, dx - (w / 2), dy);
                    int px = sx + off[0], py = sy + off[1], pz = sz + off[2];
                    if (BlockUtility.distToSqr(mc.level, px, py, pz, eye.x, eye.y, eye.z) > r * r) continue;
                    var state = BlockUtility.getState(mc.level, px, py, pz);
                    if (state.isAir()) continue;
                    if (state.liquid()) {
                        if (fillLava.getValue()) {
                            result.add(BlockUtility.packPos(px, py, pz));
                        }
                        continue;
                    }
                    if (state.getDestroySpeed(mc.level, BlockUtility.pos(px, py, pz)) < 0) continue;
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

    public static boolean maybeEnabled() {
        return maybeEnabled(AutoTunnel.class);
    }
    public static AutoTunnel itz() {
        return ModuleManager.get(AutoTunnel.class);
    }
}
