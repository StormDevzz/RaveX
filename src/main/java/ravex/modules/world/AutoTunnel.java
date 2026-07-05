package ravex.modules.world;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
import java.util.ArrayList;
import java.util.List;
public class AutoTunnel extends Module {
    public static final AutoTunnel INSTANCE = new AutoTunnel();
    public final NumberParameter range = new NumberParameter("Range", 5.0, 1.0, 10.0, 0.5);
    public final NumberParameter height = new NumberParameter("Height", 2, 1, 3, 1);
    public final NumberParameter width = new NumberParameter("Width", 2, 1, 3, 1);
    public final NumberParameter delay = new NumberParameter("Delay (ms)", 200, 50, 1000, 50);
    public final BooleanParameter fillLava = new BooleanParameter("Fill Lava", true);
    public final BooleanParameter autoWalk = new BooleanParameter("AutoWalk", false);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0x3FFFFF00);
    public static BlockPos currentTarget = null;
    private long lastActionTime = 0;
    private BlockPos currentMiningTarget = null;

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (currentMiningTarget != null && mc.gameMode != null) {
            mc.gameMode.stopDestroyBlock();
        }
        currentMiningTarget = null;
        currentTarget = null;
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
        List<BlockPos> blocks = getTunnelBlocks(mc);
        if (blocks.isEmpty()) return;
        if (fillLava.getValue()) {
            for (BlockPos pos : blocks) {
                BlockState state = mc.level.getBlockState(pos);
                if (state.liquid()) {
                    fillBlock(mc, pos);
                    lastActionTime = now;
                    return;
                }
            }
        }
        for (BlockPos pos : blocks) {
            BlockState state = mc.level.getBlockState(pos);
            if (state.isAir() || state.liquid()) continue;
            if (state.getDestroySpeed(mc.level, pos) < 0) continue;
            if (currentMiningTarget != null && !pos.equals(currentMiningTarget)) {
                mc.gameMode.stopDestroyBlock();
            }
            currentMiningTarget = pos;
            currentTarget = pos;
            mc.gameMode.startDestroyBlock(pos, getDirection(mc.player.getEyePosition(), pos));
            mc.player.swing(InteractionHand.MAIN_HAND);
            lastActionTime = now;
            return;
        }
        if (currentMiningTarget != null) {
            mc.gameMode.stopDestroyBlock();
        }
        currentMiningTarget = null;
        currentTarget = null;
    }
    private void fillBlock(Minecraft mc, BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        if (!state.liquid()) return;
        int fillSlot = -1;
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(Items.COBBLESTONE) || stack.is(Items.DIRT) || stack.is(Items.STONE)
                || stack.is(Items.GRAVEL) || stack.is(Items.NETHERRACK) || stack.is(Items.END_STONE)
                || stack.is(Items.COBBLED_DEEPSLATE)) {
                fillSlot = i;
                break;
            }
        }
        if (fillSlot == -1) return;
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(fillSlot);
        BlockPos placeOn = pos;
        BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(placeOn), Direction.UP, placeOn, true
        );
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        mc.player.getInventory().setSelectedSlot(prevSlot);
    }
    private List<BlockPos> getTunnelBlocks(Minecraft mc) {
        List<BlockPos> result = new ArrayList<>();
        Vec3 eye = mc.player.getEyePosition();
        Direction facing = mc.player.getDirection();
        int h = height.getValue().intValue();
        int w = width.getValue().intValue();
        double r = range.getValue();
        BlockPos startPos = mc.player.blockPosition();
        for (int f = 0; f < 3; f++) {
            int step = f + 1;
            for (int dy = 0; dy < h; dy++) {
                for (int dx = 0; dx < w; dx++) {
                    BlockPos pos = offsetPos(startPos, facing, step, dx - (w/2), dy);
                    if (pos.distToCenterSqr(eye.x, eye.y, eye.z) > r * r) continue;
                    BlockState state = mc.level.getBlockState(pos);
                    if (state.isAir()) continue;
                    if (state.liquid()) {
                        if (fillLava.getValue()) {
                            result.add(pos);
                        }
                        continue;
                    }
                    if (state.getDestroySpeed(mc.level, pos) < 0) continue;
                    result.add(pos);
                }
            }
            if (!result.isEmpty()) break;
        }
        return result;
    }
    private static BlockPos offsetPos(BlockPos origin, Direction facing, int forward, int right, int up) {
        int offsetX = 0, offsetZ = 0;
        switch (facing) {
            case NORTH: offsetX = -right; offsetZ = -forward; break;
            case SOUTH: offsetX = right; offsetZ = forward; break;
            case WEST: offsetX = -forward; offsetZ = right; break;
            case EAST: offsetX = forward; offsetZ = -right; break;
        }
        return origin.offset(offsetX, up, offsetZ);
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
            if (absX >= absZ) return dx > 0 ? Direction.EAST : Direction.WEST;
            else return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        } else if (absX <= absY && absX <= absZ) {
            if (absY >= absZ) return dy > 0 ? Direction.DOWN : Direction.UP;
            else return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        } else {
            if (absY >= absX) return dy > 0 ? Direction.DOWN : Direction.UP;
            else return dx > 0 ? Direction.EAST : Direction.WEST;
        }
    }
}
